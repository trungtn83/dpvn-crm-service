package com.dpvn.crm.customer;

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
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class WebHookService extends AbstractService {
  private final UserService userService;
  private final CustomerService customerService;
  private final WebHookHandlerService webHookHandlerService;

  public WebHookService(
      UserService userService,
      CustomerService customerService,
      WebHookHandlerService webHookHandlerService) {
    this.userService = userService;
    this.customerService = customerService;
    this.webHookHandlerService = webHookHandlerService;
  }

  @KafkaListener(topics = Topics.KV_UPDATE_ORDER, groupId = "crm-group")
  public void handleUpdateOrderMessage(ConsumerRecord<String, String> message) {
    // Access record details
    String key = message.key();
    String value = message.value();
    int partition = message.partition();
    long offset = message.offset();
    long timestamp = message.timestamp();

    LOGGER.info(
        "Handled order message: [key = {}, partition = {}, offset = {}, timestamp = {}]]",
        key,
        partition,
        offset,
        timestamp);
    LOGGER.info("Value: {}", value);

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

    LOGGER.info(
        "Handled invoice message: [key = {}, partition = {}, offset = {}, timestamp = {}]]",
        key,
        partition,
        offset,
        timestamp);
    LOGGER.info("Value: {}", value);

    processInvoice(value);
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
    MDC.put("requestId", "1");

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
                          Long id = orderDto.getId();
                          String code = orderDto.getCode();
                          Instant purchasedDate = orderDto.getPurchaseDate();
                          Integer status = orderDto.getStatus();
                          UserDto sale = userService.findUserByKvUserId(orderDto.getSoldById());
                          CustomerDto customer =
                              customerService.findCustomerByKvCustomerId(
                                  sale.getId(), orderDto.getCustomerId());

                          if (Objects.equals(status, KvStatus.Order.TEMP.getId())) {
                            webHookHandlerService.handleTempOrder(
                                sale, customer, code, purchasedDate);
                          } else if (Objects.equals(status, KvStatus.Order.CONFIRMED.getId())) {
                            webHookHandlerService.handleConfirmedOrder(
                                sale, customer, code, purchasedDate);
                          } else if (Objects.equals(status, KvStatus.Order.CANCELLED.getId())) {
                            webHookHandlerService.handleCancelledOrder(sale, customer, code);
                          }
                        }));
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
                          LOGGER.info("Processing update for invoice {}", invoiceDto.getCode());

                          Long id = invoiceDto.getId();
                          String code = invoiceDto.getCode();
                          Instant purchasedDate = invoiceDto.getPurchaseDate();
                          Integer status = invoiceDto.getStatus();
                          Integer deliveryStatus = invoiceDto.getInvoiceDelivery().getStatus();
                          UserDto sale = userService.findUserByKvUserId(invoiceDto.getSoldById());
                          CustomerDto customer =
                              customerService.findCustomerByKvCustomerId(
                                  sale.getId(), invoiceDto.getCustomerId());

                          if (Objects.equals(status, KvStatus.Invoice.DELIVERING.getId())
                              && Objects.equals(
                                  deliveryStatus, KvStatus.DeliveryStatus.WAIT_PROCESS.getId())) {
                            webHookHandlerService.handleInProgressInvoice(
                                sale, customer, code, purchasedDate);
                          } else if (Objects.equals(status, KvStatus.Invoice.DELIVERING.getId())
                              && Objects.equals(
                                  deliveryStatus, KvStatus.DeliveryStatus.DELIVERED.getId())) {
                            webHookHandlerService.handleCompletedInvoice(
                                sale, customer, code, purchasedDate);
                          } else if (Objects.equals(status, KvStatus.Invoice.CANCELLED.getId())
                              && Objects.equals(
                                  deliveryStatus, KvStatus.DeliveryStatus.CANCELLED.getId())) {
                            webHookHandlerService.handleCancelledInvoice(sale, customer, code);
                          }
                        }));
  }
}
