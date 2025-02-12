package com.dpvn.crm.customer;

import com.dpvn.crm.client.KiotvietServiceClient;
import com.dpvn.crm.customer.dto.InvoiceHookDto;
import com.dpvn.crm.customer.dto.OrderHookDto;
import com.dpvn.crm.customer.dto.PayloadDto;
import com.dpvn.crm.user.UserService;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.reportcrudservice.domain.constant.KvStatus;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.ListUtil;
import com.dpvn.shared.util.ObjectUtil;
import com.dpvn.webhookhandler.domain.Topics;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Instant;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class WebHookService extends AbstractService {
  private final UserService userService;
  private final CustomerService customerService;
  private final WebHookHandlerService webHookHandlerService;
  private final KiotvietServiceClient kiotvietServiceClient;

  public WebHookService(
      UserService userService,
      CustomerService customerService,
      WebHookHandlerService webHookHandlerService,
      KiotvietServiceClient kiotvietServiceClient) {
    this.userService = userService;
    this.customerService = customerService;
    this.webHookHandlerService = webHookHandlerService;
    this.kiotvietServiceClient = kiotvietServiceClient;
  }

  @KafkaListener(topics = Topics.KV_UPDATE_ORDER, groupId = "crm-group")
  public void handleUpdateOrderMessage(ConsumerRecord<String, String> message) {
    // Access record details
    String key = message.key();
    String value = message.value();
    int partition = message.partition();
    long offset = message.offset();
    long timestamp = message.timestamp();

    processOrder(value);
  }

  @KafkaListener(topics = Topics.KV_UPDATE_INVOICE, groupId = "crm-group")
  public void handleUpdateInvoiceMessage(ConsumerRecord<String, String> message) {
    // Access record details
    String key = message.key();
    String value = message.value();
    int partition = message.partition();
    long offset = message.offset();
    long timestamp = message.timestamp();

    processInvoice(value);
  }

  @KafkaListener(topics = Topics.KV_UPDATE_CUSTOMER, groupId = "crm-group")
  public void handleUpdateCustomerMessage(ConsumerRecord<String, String> message) {
    // Access record details
    String key = message.key();
    String value = message.value();
    int partition = message.partition();
    long offset = message.offset();
    long timestamp = message.timestamp();

    // TODO: sync one customer by call back to kiotviet
    // data from hook does not have enough information to sync customer (wardname, extra phones...)
    // fuk kiotviet webhook here
    PayloadDto<OrderHookDto> payloadDto = ObjectUtil.readValue(value, new TypeReference<>() {});
    payloadDto
        .getNotifications()
        .forEach(
            notification ->
                notification
                    .getData()
                    .forEach(
                        customerHookDto ->
                            kiotvietServiceClient.syncCustomer(customerHookDto.getId())));
  }

  private void validatePayload(String type, String payload) {
    PayloadDto<?> payloadDto = ObjectUtil.readValue(payload, new TypeReference<>() {});
    if (payloadDto.getId() == null
        || ListUtil.isEmpty(payloadDto.getNotifications())
        || payloadDto.getNotifications().stream()
            .anyMatch(notification -> ListUtil.isEmpty(notification.getData()))) {
      LOGGER.error("Received {} payload in mal-format", type);

      throw new BadRequestException("Invalid payload");
    }
  }

  public void processOrder(String payload) {
    LOGGER.info("Received {} payload: {}", "ORDER", payload);
    PayloadDto<OrderHookDto> payloadDto = ObjectUtil.readValue(payload, new TypeReference<>() {});
    validatePayload("ORDER", payload);
    payloadDto
        .getNotifications()
        .forEach(
            notification ->
                notification
                    .getData()
                    .forEach(
                        orderDto -> {
                          LOGGER.info(
                              "Processing update for order {} = [{}]",
                              orderDto.getCode(),
                              ObjectUtil.writeValueAsString(orderDto));
                          try {
                            // fuk kiot, send missing data, have to call to web to sync
                            kiotvietServiceClient.syncOrder(orderDto.getCode());

                            // process order hook to update customer relationship
                            processOrderHookDto(orderDto);
                          } catch (Exception e) {
                            LOGGER.error(
                                "Error processing order {}: {}",
                                orderDto.getCode(),
                                e.getLocalizedMessage(),
                                e);
                          }
                        }));
  }

  private void processOrderHookDto(OrderHookDto orderHookDto) {
    Long id = orderHookDto.getId();
    String code = orderHookDto.getCode();
    Instant purchasedDate = orderHookDto.getPurchaseDate();
    Integer status = orderHookDto.getStatus();
    // find user by kiotviet user id, link by idf
    UserDto sale = userService.findUserByKvUserId(orderHookDto.getSoldById());

    // find customer by kiotviet customer id, link by idf
    CustomerDto customer = customerService.findCustomerByKvCustomerId(orderHookDto.getCustomerId());

    if (Objects.equals(status, KvStatus.Order.TEMP.getId())) {
      webHookHandlerService.handleTempOrder(sale, customer, code, purchasedDate);
      LOGGER.info("Processed update for TEMP order {}", orderHookDto.getCode());
    } else if (Objects.equals(status, KvStatus.Order.CONFIRMED.getId())) {
      webHookHandlerService.handleConfirmedOrder(sale, customer, code, purchasedDate);
      LOGGER.info("Processed update for CONFIRMED order {}", orderHookDto.getCode());
    } else if (Objects.equals(status, KvStatus.Order.CANCELLED.getId())) {
      webHookHandlerService.handleCancelledOrder(sale, customer, code);
      LOGGER.info("Processed update for DELETED order {}", orderHookDto.getCode());
    } else {
      LOGGER.info("Processed update for NO NEED ANY ACTION order {}", orderHookDto.getCode());
    }
  }

  public void processInvoice(String payload) {
    PayloadDto<InvoiceHookDto> payloadDto = ObjectUtil.readValue(payload, new TypeReference<>() {});
    validatePayload("invoice", payload);
    payloadDto
        .getNotifications()
        .forEach(
            notification ->
                notification
                    .getData()
                    .forEach(
                        invoiceDto -> {
                          LOGGER.info(
                              "Processing update for invoice {} = [{}]",
                              invoiceDto.getCode(),
                              ObjectUtil.writeValueAsString(invoiceDto));
                          try {
                            // TODO: fuk kiot, send missing data, have to call to web to sync
                            // update order in database of wms from webhook
                            kiotvietServiceClient.syncInvoice(invoiceDto.getCode());

                            // process order hook to update customer relationship
                            processInvoiceHookDto(invoiceDto);
                          } catch (Exception e) {
                            LOGGER.error(
                                "Error processing invoice {}: [{}] with error: {}",
                                invoiceDto.getCode(),
                                ObjectUtil.writeValueAsString(invoiceDto),
                                e.getLocalizedMessage(),
                                e);
                          }
                        }));
  }

  private void processInvoiceHookDto(InvoiceHookDto invoiceHookDto) {
    Long id = invoiceHookDto.getId();
    String code = invoiceHookDto.getCode();
    Instant purchasedDate = invoiceHookDto.getPurchaseDate();
    Integer status = invoiceHookDto.getStatus();
    Integer deliveryStatus =
        invoiceHookDto.getInvoiceDelivery() == null
            ? KvStatus.DeliveryStatus.WAIT_PROCESS.getId()
            : invoiceHookDto.getInvoiceDelivery().getStatus();
    UserDto sale = userService.findUserByKvUserId(invoiceHookDto.getSoldById());
    CustomerDto customer =
        customerService.findCustomerByKvCustomerId(invoiceHookDto.getCustomerId());

    LOGGER.info(
        "Process inside invoice {} with content: {}",
        invoiceHookDto.getCode(),
        ObjectUtil.writeValueAsString(invoiceHookDto));
    if (Objects.equals(status, KvStatus.Invoice.DELIVERING.getId())
        && Objects.equals(deliveryStatus, KvStatus.DeliveryStatus.WAIT_PROCESS.getId())) {
      webHookHandlerService.handleInProgressInvoice(sale, customer, code, purchasedDate);
      LOGGER.info("Processed update for IN-PROGRESS invoice {}", invoiceHookDto.getCode());
    } else if (Objects.equals(status, KvStatus.Invoice.DELIVERING.getId())
        && Objects.equals(deliveryStatus, KvStatus.DeliveryStatus.DELIVERED.getId())) {
      webHookHandlerService.handleCompletedInvoice(sale, customer, code, purchasedDate);
      LOGGER.info("Processed update for COMPLETED invoice {}", invoiceHookDto.getCode());
    } else if (Objects.equals(status, KvStatus.Invoice.CANCELLED.getId())
        && Objects.equals(deliveryStatus, KvStatus.DeliveryStatus.CANCELLED.getId())) {
      webHookHandlerService.handleCancelledInvoice(sale, customer, code);
      LOGGER.info("Processed update for CANCELLED invoice {}", invoiceHookDto.getCode());
    } else if (Objects.equals(status, KvStatus.Invoice.COMPLETED.getId())
        || Objects.equals(deliveryStatus, KvStatus.DeliveryStatus.DELIVERED.getId())) {
      webHookHandlerService.handleCompletedInvoice(sale, customer, code, purchasedDate);
      LOGGER.info("Processed update for NO NEED ANY ACTION invoice {}", invoiceHookDto.getCode());
    }
  }
}
