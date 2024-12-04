package com.dpvn.crm.customer;

import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.crm.customer.dto.InvoiceHookDto;
import com.dpvn.crm.customer.dto.OrderHookDto;
import com.dpvn.crm.customer.dto.PayloadDto;
import com.dpvn.crm.user.UserService;
import com.dpvn.crmcrudservice.domain.constant.RelationshipType;
import com.dpvn.crmcrudservice.domain.constant.SaleCustomers;
import com.dpvn.crmcrudservice.domain.constant.Status;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.reportcrudservice.domain.constant.KvStatus;
import com.dpvn.reportcrudservice.domain.dto.LogDto;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.util.DateUtil;
import com.dpvn.shared.util.ListUtil;
import com.dpvn.shared.util.ObjectUtil;
import com.dpvn.webhookhandler.domain.Topics;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class WebHookService {
  private static final Logger LOG = LoggerFactory.getLogger(WebHookService.class);
  private final UserService userService;
  private final CustomerService customerService;
  private final SaleCustomerService saleCustomerService;
  private final ReportCrudClient reportCrudClient;

  public WebHookService(
      UserService userService,
      CustomerService customerService,
      SaleCustomerService saleCustomerService,
      ReportCrudClient reportCrudClient) {
    this.userService = userService;
    this.customerService = customerService;
    this.saleCustomerService = saleCustomerService;
    this.reportCrudClient = reportCrudClient;
  }

  @KafkaListener(topics = Topics.KV_UPDATE_ORDER, groupId = "crm-group")
  public void handleUpdateOrderMessage(ConsumerRecord<String, String> message) {
    // Access record details
    String key = message.key();
    String value = message.value();
    int partition = message.partition();
    long offset = message.offset();
    long timestamp = message.timestamp();

    LOG.info(
        "Handled order message: [key = {}, partition = {}, offset = {}, timestamp = {}]]",
        key,
        partition,
        offset,
        timestamp);
    LOG.info("Value: {}", value);

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

    LOG.info(
        "Handled invoice message: [key = {}, partition = {}, offset = {}, timestamp = {}]]",
        key,
        partition,
        offset,
        timestamp);
    LOG.info("Value: {}", value);

    processInvoice(value);
  }

  private void validatePayload(String type, String payload) {
    PayloadDto<?> payloadDto = ObjectUtil.readValue(payload, new TypeReference<>() {});
    if (payloadDto.getId() == null
        || ListUtil.isEmpty(payloadDto.getNotifications())
        || payloadDto.getNotifications().stream()
            .anyMatch(notification -> ListUtil.isEmpty(notification.getData()))) {
//      LogDto logDto = new LogDto();
//      logDto.setCreatedBy(-1L);
//      logDto.setAction("WEB-HOOK");
//      logDto.setFunction(type);
//      logDto.setSource(payload);
//      logDto.setDescription(String.format("Received %s payload in mal-format", type));
//      reportCrudClient.createLog(logDto);
      LOG.error("Received {} payload in mal-format", type);

      throw new BadRequestException("Invalid payload");
    }
  }

  public void processOrder(String payload) {
    MDC.put("requestId", "1");

    LOG.info("Received {} payload: {}", "ORDER", payload);
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
                          String purchasedDate = orderDto.getPurchaseDate();
                          Integer status = orderDto.getStatus();
                          UserDto sale = userService.findUserByKvUserId(orderDto.getSoldById());
                          CustomerDto customer =
                              customerService.findCustomerByKvCustomerId(
                                  sale.getId(), orderDto.getCustomerId());

                          if (Objects.equals(status, KvStatus.Order.TEMP.getId())) {
                            handleTempOrder(sale, customer, code);
                          } else if (Objects.equals(status, KvStatus.Order.CONFIRMED.getId())) {
                            handleConfirmedOrder(sale, customer, code);
                          } else if (Objects.equals(status, KvStatus.Order.CANCELLED.getId())) {
                            handleCancelledOrder(sale, customer, code);
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
                          LOG.info("Processing update for invoice {}", invoiceDto.getCode());

                          Long id = invoiceDto.getId();
                          String code = invoiceDto.getCode();
                          String purchasedDate = invoiceDto.getPurchaseDate();
                          Integer status = invoiceDto.getStatus();
                          Integer deliveryStatus = invoiceDto.getInvoiceDelivery().getStatus();
                          UserDto sale = userService.findUserByKvUserId(invoiceDto.getSoldById());
                          CustomerDto customer =
                              customerService.findCustomerByKvCustomerId(
                                  sale.getId(), invoiceDto.getCustomerId());

                          if (Objects.equals(status, KvStatus.Invoice.DELIVERING.getId())
                              && Objects.equals(
                                  deliveryStatus, KvStatus.DeliveryStatus.WAIT_PROCESS.getId())) {
                            handleInProgressInvoice(sale, customer, code);
                          } else if (Objects.equals(status, KvStatus.Invoice.DELIVERING.getId())
                              && Objects.equals(
                                  deliveryStatus, KvStatus.DeliveryStatus.DELIVERED.getId())) {
                            handleCompletedInvoice(sale, customer, code);
                          } else if (Objects.equals(status, KvStatus.Invoice.CANCELLED.getId())
                              && Objects.equals(
                                  deliveryStatus, KvStatus.DeliveryStatus.CANCELLED.getId())) {
                            handleCancelledInvoice(sale, customer, code);
                          }
                        }));
  }

  private SaleCustomerDto generateAssignCustomerToSaleWithTypeInDays(
      Long saleId, Long customerId, int type, String typeRef, int days) {
    SaleCustomerDto saleCustomerDto = new SaleCustomerDto();
    saleCustomerDto.setCustomerId(customerId);
    saleCustomerDto.setSaleId(saleId);
    saleCustomerDto.setRelationshipType(RelationshipType.PIC);
    saleCustomerDto.setReasonId(type);
    saleCustomerDto.setReasonRef(typeRef);
    saleCustomerDto.setReasonNote(
        String.format("Handling: [Ref=%s, sale=%s, customer=%s]", typeRef, saleId, customerId));
    saleCustomerDto.setAvailableFrom(DateUtil.now());
    saleCustomerDto.setAvailableTo(DateUtil.now().plus(days, ChronoUnit.DAYS));
    saleCustomerDto.setStatus(Status.ACTIVE);
    return saleCustomerDto;
  }

  /**
   * Khi tạo ra temp order - Nếu khách mới thì tạo ra record mới cho sale-customer có giá trị trong
   * 7 ngày - Nếu khách cũ thì không cần làm gì do đã có record rồi
   */
  private void handleTempOrder(UserDto sale, CustomerDto customer, String code) {
    LOG.info("Processing temp order {} of customer {} by sale {}", code, customer, sale);
    SaleCustomerDto saleCustomerDto =
        saleCustomerService.findSaleCustomerByReason(
            sale.getId(), customer.getId(), null, null, code);
    if (saleCustomerDto != null) {
      saleCustomerDto.setReasonId(SaleCustomers.Reason.ORDER);
      saleCustomerDto.setAvailableFrom(DateUtil.now());
      saleCustomerDto.setAvailableTo(DateUtil.now().plus(7, ChronoUnit.DAYS));
      saleCustomerService.upsertSaleCustomer(saleCustomerDto);
      LOG.info("Sale customer [{}, {}] for ORDER code {} existed, process update", sale, customer, code);
      return;
    }

    // TODO: Có cần check nếu khách cũ nhưng còn ít hơn 7 ngày thì có nên tạo không???
    if (!customerService.isOldCustomer(sale.getId(), customer.getId())) {
      saleCustomerService.upsertSaleCustomer(
          generateAssignCustomerToSaleWithTypeInDays(
              sale.getId(), customer.getId(), SaleCustomers.Reason.ORDER, code, 7));
      LOG.info("Sale customer [{}, {}] for ORDER code {} does not exist, create the new one for 7 days", sale, customer, code);
      return;
    }

    LOG.info("Sale customer [{}, {}] is old customer, temp ORDER does not affect to current relationship", sale, customer);
  }

  /** Khi tạo ra confirmed order: tạo record mới cho sale với khách đó trong 3 tháng */
  private void handleConfirmedOrder(UserDto sale, CustomerDto customer, String code) {
    SaleCustomerDto saleCustomerDto =
        saleCustomerService.findSaleCustomerByReason(
            sale.getId(), customer.getId(), null, null, code);
    if (saleCustomerDto != null) {
      saleCustomerDto.setReasonId(SaleCustomers.Reason.ORDER);
      saleCustomerDto.setAvailableFrom(DateUtil.now());
      saleCustomerDto.setAvailableTo(DateUtil.now().plus(90, ChronoUnit.DAYS));
      saleCustomerService.upsertSaleCustomer(saleCustomerDto);
      return;
    }

    saleCustomerService.upsertSaleCustomer(
        generateAssignCustomerToSaleWithTypeInDays(
            sale.getId(), customer.getId(), SaleCustomers.Reason.ORDER, code, 90));
  }

  /**
   * Khi order bị huỷ: tìm sale-customer với reason là Order và reason ref là Order đó xoá đi Không
   * quan tâm type = gì vì có thể huỷ khi Order đang là temp hay confirmed
   */
  private void handleCancelledOrder(UserDto sale, CustomerDto customer, String code) {
    saleCustomerService.removeSaleCustomerByReason(
        sale.getId(), customer.getId(), SaleCustomers.Reason.ORDER, code);
  }

  /**
   * Khi order là completed, không quan tâm bới sẽ handle trong case Invoice In-Progress Khi Invoice
   * là in-progress - Update relationship type của sale-customer sang INVOICE, ref sang invoice code
   * - Update available time của customer đó theo ngày hiện tại + rule 3 tháng
   */
  private void handleInProgressInvoice(UserDto sale, CustomerDto customer, String code) {
    SaleCustomerDto saleCustomerDto =
        saleCustomerService.findSaleCustomerByReason(
            sale.getId(), customer.getId(), null, SaleCustomers.Reason.ORDER, code);
    if (saleCustomerDto != null) {
      saleCustomerDto.setReasonId(SaleCustomers.Reason.INVOICE);
      saleCustomerDto.setReasonRef(code);
      saleCustomerDto.setAvailableFrom(DateUtil.now());
      saleCustomerDto.setAvailableTo(DateUtil.now().plus(90, ChronoUnit.DAYS));
      saleCustomerService.upsertSaleCustomer(saleCustomerDto);
      return;
    }

    saleCustomerService.upsertSaleCustomer(
        generateAssignCustomerToSaleWithTypeInDays(
            sale.getId(), customer.getId(), SaleCustomers.Reason.INVOICE, code, 90));
  }

  /** Khi Invoice là completed: hình như chưa cần làm gì */
  private void handleCompletedInvoice(UserDto sale, CustomerDto customer, String code) {}

  private void handleCancelledInvoice(UserDto sale, CustomerDto customer, String code) {
    saleCustomerService.removeSaleCustomerByReason(
        sale.getId(), customer.getId(), SaleCustomers.Reason.INVOICE, code);
  }
}
