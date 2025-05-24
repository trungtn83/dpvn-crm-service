package com.dpvn.crm.webhook;

import com.dpvn.crm.campaign.DispatchService;
import com.dpvn.crm.client.KiotvietServiceClient;
import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.crm.client.WmsCrudClient;
import com.dpvn.crm.customer.CustomerService;
import com.dpvn.crm.customer.dto.InvoiceHookDto;
import com.dpvn.crm.customer.dto.OrderHookDto;
import com.dpvn.crm.customer.dto.PayloadDto;
import com.dpvn.crm.helper.ConfigurationService;
import com.dpvn.crm.user.UserService;
import com.dpvn.crm.voip24h.domain.ViCallLogDto;
import com.dpvn.crmcrudservice.domain.constant.Customers;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.kiotviet.domain.KvAddressBookDto;
import com.dpvn.kiotviet.domain.KvCustomerDto;
import com.dpvn.kiotviet.domain.KvInvoiceDto;
import com.dpvn.kiotviet.domain.KvOrderDto;
import com.dpvn.kiotviet.domain.constant.KvOrders;
import com.dpvn.reportcrudservice.domain.constant.KvStatus;
import com.dpvn.shared.config.CacheService;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ListUtil;
import com.dpvn.shared.util.ObjectUtil;
import com.dpvn.shared.util.StringUtil;
import com.dpvn.shared.util.SystemUtil;
import com.dpvn.webhookhandler.domain.Topics;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WebHookService extends AbstractService {
  private final UserService userService;
  private final CustomerService customerService;
  private final WebHookHandlerService webHookHandlerService;
  private final KiotvietServiceClient kiotvietServiceClient;
  private final WmsCrudClient wmsCrudClient;
  private final ReportCrudClient reportCrudClient;
  private final DispatchService dispatchService;
  private final ConfigurationService configurationService;
  private final CacheService cacheService;

  public WebHookService(
      UserService userService,
      CustomerService customerService,
      WebHookHandlerService webHookHandlerService,
      KiotvietServiceClient kiotvietServiceClient,
      WmsCrudClient wmsCrudClient,
      ReportCrudClient reportCrudClient,
      DispatchService dispatchService,
      ConfigurationService configurationService,
      CacheService cacheService) {
    this.userService = userService;
    this.customerService = customerService;
    this.webHookHandlerService = webHookHandlerService;
    this.kiotvietServiceClient = kiotvietServiceClient;
    this.wmsCrudClient = wmsCrudClient;
    this.reportCrudClient = reportCrudClient;
    this.dispatchService = dispatchService;
    this.configurationService = configurationService;
    this.cacheService = cacheService;
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

  @KafkaListener(topics = Topics.VOIP24H_CALL_LOGS_UPDATE, groupId = "voip24h-group")
  public void handleUpdateCallLogMessage(ConsumerRecord<String, String> message) {
    String value = message.value();
    LOGGER.info("Received {} payload: {}", "CALLLOG", value);
    ViCallLogDto viCallLogDto = ObjectUtil.readValue(value, ViCallLogDto.class);
    reportCrudClient.syncAllCallLogs(List.of(viCallLogDto));
  }

  @KafkaListener(topics = Topics.WEBSITE_CREATE_ORDER, groupId = "website-group")
  public void handleWebsiteCreateOrderMessage(ConsumerRecord<String, String> message) {
    String value = message.value();
    LOGGER.info("Received {} payload: {}", "WEBSITE duocphamvietnhat", value);
    FastMap order = ObjectUtil.readValue(value, FastMap.class);

    processWebsiteCreateOrder(order.getMap("data"));
  }

  private void processWebsiteCreateOrder(FastMap rawOrder) {
    String mobilePhone = rawOrder.getString("phone_number");
    FastMap rawCustomer = rawOrder.getMap("customer");
    FastMap rawCustomerAddress = rawOrder.getMap("customer_address");

    FastMap customerStatus = customerService.getCustomerByMobilePhoneStatus(mobilePhone);
    CustomerDto customerDto =
        customerStatus.containsKey("customer")
            ? customerStatus.getClass("customer", CustomerDto.class)
            : createNewCustomerFromWeb(rawCustomer, rawCustomerAddress);

    KvAddressBookDto kvAddressBookDto =
        tranformToKvAddressBookDto(customerDto.getIdf(), rawCustomerAddress);
    KvAddressBookDto addressDto =
        kiotvietServiceClient.createCustomerAddressFromWebsite(kvAddressBookDto);

    List<Long> ownerIds = customerStatus.getMap("owner").getListClass("ownerId", Long.class);
    boolean isMine =
        List.of(Customers.Owner.TREASURE, Customers.Owner.GOLD)
            .contains(customerStatus.getMap("owner").getString("ownerName"));
    UserDto sale = dispatchService.getNextSale(customerDto.getId(), ownerIds);
    if (!isMine) {
      configurationService.upsertConfigDispatchRoundRobin(sale.getId());
    }

    // call to kiotviet to create new order with saleId
    FastMap customer =
        FastMap.create()
            .add("id", customerDto.getIdf())
            .add("name", addressDto.getReceiver())
            .add("address", addressDto.getAddress())
            .add("phone", addressDto.getContactNumber())
            .add("location", addressDto.getLocationName())
            .add("ward", addressDto.getWardName());
    String orderCode = rawOrder.getString("order_code");
    List<FastMap> items = rawOrder.getListClass("line_items_at_time", FastMap.class);
    FastMap order =
        FastMap.create()
            .add("code", orderCode)
            .add("saleId", sale.getIdf())
            .add(
                "details",
                items.stream()
                    .map(
                        item ->
                            FastMap.create()
                                .add("code", item.getString("sku"))
                                .add("quantity", item.getInt("quantity"))
                                .add("price", item.getLong("item_price")))
                    .toList());
    kiotvietServiceClient.createOrderFromWebsite(
        FastMap.create().add("customer", customer).add("order", order));
  }

  private CustomerDto createNewCustomerFromWeb(FastMap rawCustomer, FastMap rawAddressBook) {
    // call to kiotviet to create new one
    KvCustomerDto kvCustomerDto = tranformToKvCustomerDto(rawCustomer, rawAddressBook);
    KvCustomerDto savedKvcustomerDto =
        kiotvietServiceClient.createCustomerFromWebsite(kvCustomerDto);
    SystemUtil.sleep(5); // wait for sync customer from kiotviet to crm (event by webhook)
    return customerService.findCustomerByKvCustomerId(savedKvcustomerDto.getId());
  }

  private KvCustomerDto tranformToKvCustomerDto(FastMap rawCustomer, FastMap rawAddressBook) {
    KvCustomerDto kvCustomerDto = new KvCustomerDto();
    kvCustomerDto.setLocationName(
        rawAddressBook.getString("province_name")
            + " - "
            + rawAddressBook.getString("district_name"));
    kvCustomerDto.setWardName(rawAddressBook.getString("wards_name"));
    kvCustomerDto.setName(rawCustomer.getString("name"));
    kvCustomerDto.setContactNumber(rawCustomer.getString("phone_number"));
    kvCustomerDto.setGender(rawCustomer.getInt("sex") == 1 ? Boolean.TRUE : Boolean.FALSE);
    kvCustomerDto.setEmail(rawCustomer.getString("email"));
    kvCustomerDto.setComments("Khách hàng tạo mới từ website");
    return kvCustomerDto;
  }

  private KvAddressBookDto tranformToKvAddressBookDto(Long customerId, FastMap rawAddressBook) {
    KvAddressBookDto kvAddressBookDto = new KvAddressBookDto();
    kvAddressBookDto.setCustomerId(customerId);
    kvAddressBookDto.setName(rawAddressBook.getString("name"));
    kvAddressBookDto.setReceiver(rawAddressBook.getString("name"));
    kvAddressBookDto.setAddress(rawAddressBook.getString("address_detail"));
    kvAddressBookDto.setContactNumber(rawAddressBook.getString("phone"));
    kvAddressBookDto.setLocationName(
        rawAddressBook.getString("province_name")
            + " - "
            + rawAddressBook.getString("district_name"));
    kvAddressBookDto.setWardName(rawAddressBook.getString("wards_name"));
    return kvAddressBookDto;
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
    Long branchId = orderHookDto.getBranchId();
    String orderCode = orderHookDto.getCode();
    String code = String.format("%d-%s", branchId, orderCode);
    Instant purchasedDate = orderHookDto.getPurchaseDate();
    Integer status = orderHookDto.getStatus();
    // find user by kiotviet user id, link by idf
    UserDto sale = userService.findUserByKvUserId(orderHookDto.getSoldById());

    // find customer by kiotviet customer id, link by idf
    CustomerDto customer = customerService.findCustomerByKvCustomerId(orderHookDto.getCustomerId());

    // sometimes, customer is THUOCSI, BUYMED... no need to cover this case
    if (customer != null) {
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
    Long branchId = invoiceHookDto.getBranchId();
    String invoiceCode = invoiceHookDto.getCode();
    String code = String.format("%d-%s", branchId, invoiceCode);
    Instant purchasedDate = invoiceHookDto.getPurchaseDate();
    Integer status = invoiceHookDto.getStatus();
    Integer deliveryStatus =
        invoiceHookDto.getInvoiceDelivery() == null
            ? KvStatus.DeliveryStatus.WAIT_PROCESS.getId()
            : invoiceHookDto.getInvoiceDelivery().getStatus();
    UserDto sale = userService.findUserByKvUserId(invoiceHookDto.getSoldById());
    CustomerDto customer =
        customerService.findCustomerByKvCustomerId(invoiceHookDto.getCustomerId());

    // sometimes, customer is THUOCSI, BUYMED... no need to cover this case
    if (customer != null) {
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

  /*
  In the kiotviet, tranngocm start at : 19/04/2025 15:50 (based on ORDER)
   */
  @Scheduled(cron = "0 30 7,10,12,15,18 * * *", zone = "Asia/Ho_Chi_Minh")
  public void manualSync() {
    manualSync(7);
  }

  public void manualSync(Integer limit) {
    //    cacheService.preventTooManyRequest("manualSync", 30);
    List<KvOrderDto> manualSyncOrders = kiotvietServiceClient.findAllManualSync(limit);
    if (ListUtil.isNotEmpty(manualSyncOrders)) {
      List<String> orderCodes =
          manualSyncOrders.stream()
              .filter(o -> o.getStatus().equals(KvOrders.CONFIRMED))
              .map(KvOrderDto::getCode)
              .toList();
      List<String> invoiceCodes =
          manualSyncOrders.stream()
              .filter(o -> o.getStatus().equals(KvOrders.COMPLETED))
              .map(KvOrderDto::getInvoiceCodes)
              .toList();

      Long branchId = manualSyncOrders.get(0).getBranchId();
      syncOrdersIfNeed(branchId, orderCodes);
      syncInvoicesIfNeed(branchId, invoiceCodes);
    }
  }

  private void syncOrdersIfNeed(Long branchId, List<String> orderCodes) {
    List<String> needToSyncOrderCodes =
        wmsCrudClient.findNotExistedOrderFromList(branchId, orderCodes);
    needToSyncOrderCodes.forEach(
        orderCode -> {
          KvOrderDto kvOrderDto = kiotvietServiceClient.syncOrder(orderCode);
          OrderHookDto orderHookDto = transformKvrderDtoToOrderHookDto(kvOrderDto);
          processOrderHookDto(orderHookDto);
        });
  }

  // public to support manual sync one invoice by leader
  public void syncInvoicesIfNeed(Long branchId, List<String> invoiceCodesFull) {
    List<String> invoiceCodes =
        invoiceCodesFull.stream().map(this::getLastAndValidInvoiceCode).toList();
    List<String> needToSyncInvoiceCodes =
        wmsCrudClient.findNotExistedInvoiceFromList(branchId, invoiceCodes);
    needToSyncInvoiceCodes.forEach(
        invoiceCode -> {
          KvInvoiceDto kvInvoiceDto = kiotvietServiceClient.syncInvoice(invoiceCode);
          InvoiceHookDto invoiceHookDto = transformKvInvoiceDtoToInvoiceHookDto(kvInvoiceDto);
          processInvoiceHookDto(invoiceHookDto);
        });
  }

  private String getLastAndValidInvoiceCode(String invoiceCodes) {
    List<String> invoiceCodesList = StringUtil.split(invoiceCodes, ",");
    return invoiceCodesList.get(invoiceCodesList.size() - 1).trim();
  }

  private OrderHookDto transformKvrderDtoToOrderHookDto(KvOrderDto kvOrderDto) {
    OrderHookDto orderHookDto = new OrderHookDto();
    orderHookDto.setBranchId(kvOrderDto.getBranchId());
    orderHookDto.setCode(kvOrderDto.getCode());
    orderHookDto.setPurchaseDate(kvOrderDto.getPurchaseDate());
    orderHookDto.setStatus(Integer.valueOf(kvOrderDto.getStatus()));
    orderHookDto.setSoldById(kvOrderDto.getSoldById());
    orderHookDto.setCustomerId(kvOrderDto.getCustomerId());
    return orderHookDto;
  }

  private InvoiceHookDto transformKvInvoiceDtoToInvoiceHookDto(KvInvoiceDto kvInvoiceDto) {
    InvoiceHookDto invoiceHookDto = new InvoiceHookDto();
    invoiceHookDto.setBranchId(kvInvoiceDto.getBranchId());
    invoiceHookDto.setId(kvInvoiceDto.getId());
    invoiceHookDto.setCode(kvInvoiceDto.getCode());
    invoiceHookDto.setPurchaseDate(kvInvoiceDto.getPurchaseDate());
    invoiceHookDto.setStatus(Integer.valueOf(kvInvoiceDto.getStatus()));
    invoiceHookDto.setSoldById(kvInvoiceDto.getSoldById());
    invoiceHookDto.setCustomerId(kvInvoiceDto.getCustomerId());
    return invoiceHookDto;
  }
}
