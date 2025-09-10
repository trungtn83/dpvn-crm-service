package com.dpvn.crm.webhook;

import com.dpvn.crm.campaign.DispatchService;
import com.dpvn.crm.client.CrmCrudClient;
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
import com.dpvn.crmcrudservice.domain.dto.CustomerAddressDto;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.CustomerReferenceDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.kiotviet.domain.KvAddressBookDto;
import com.dpvn.kiotviet.domain.KvCustomerDto;
import com.dpvn.kiotviet.domain.KvInvoiceDto;
import com.dpvn.kiotviet.domain.KvOrderDto;
import com.dpvn.kiotviet.domain.constant.KvOrders;
import com.dpvn.reportcrudservice.domain.constant.KvStatus;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.FileUtil;
import com.dpvn.shared.util.ListUtil;
import com.dpvn.shared.util.ObjectUtil;
import com.dpvn.shared.util.StringUtil;
import com.dpvn.shared.util.SystemUtil;
import com.dpvn.thuocsi.domain.TsCustomerDto;
import com.dpvn.webhookhandler.domain.Topics;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
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
  private final CrmCrudClient crmCrudClient;

  public WebHookService(
      UserService userService,
      CustomerService customerService,
      WebHookHandlerService webHookHandlerService,
      KiotvietServiceClient kiotvietServiceClient,
      WmsCrudClient wmsCrudClient,
      ReportCrudClient reportCrudClient,
      DispatchService dispatchService,
      ConfigurationService configurationService,
      CrmCrudClient crmCrudClient) {
    this.userService = userService;
    this.customerService = customerService;
    this.webHookHandlerService = webHookHandlerService;
    this.kiotvietServiceClient = kiotvietServiceClient;
    this.wmsCrudClient = wmsCrudClient;
    this.reportCrudClient = reportCrudClient;
    this.dispatchService = dispatchService;
    this.configurationService = configurationService;
    this.crmCrudClient = crmCrudClient;
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
  public void handleUpdateKiotVietCustomerMessage(ConsumerRecord<String, String> message) {
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

  @KafkaListener(topics = Topics.TS_UPDATE_CUSTOMER, groupId = "crm-group")
  public void handleUpdateThuocSiCustomerMessage(ConsumerRecord<String, String> message) {
    // Access record details
    String key = message.key();
    String value = message.value();
    int partition = message.partition();
    long offset = message.offset();
    long timestamp = message.timestamp();

    try {
      TsCustomerDto tsCustomerDto = ObjectUtil.readValue(value, new TypeReference<>() {});

      List<CustomerDto> customerDtos =
          crmCrudClient.findCustomersByMobilePhone(tsCustomerDto.getPhone());

      if (ListUtil.isEmpty(customerDtos)) {
        CustomerDto customerDto = new CustomerDto();
        customerDto.setIdf(tsCustomerDto.getCustomerId());
        customerDto.setCustomerName(tsCustomerDto.getName());
        customerDto.setMobilePhone(tsCustomerDto.getPhone());
        customerDto.setEmail(tsCustomerDto.getEmail());
        customerDto.setTaxCode(tsCustomerDto.getTaxCode());
        customerDto.setSourceId(Customers.Source.CRAFTONLINE);

        CustomerAddressDto addressDto = new CustomerAddressDto();
        addressDto.setActive(true);
        addressDto.setAddress(tsCustomerDto.getAddress());
        customerDto.setAddresses(List.of(addressDto));

        List<CustomerReferenceDto> referenceDtos = extractReferences(tsCustomerDto);
        if (ListUtil.isNotEmpty(referenceDtos)) {
          customerDto.setReferences(referenceDtos);
        }

        crmCrudClient.createNewCustomer(customerDto);
      } else {
        FastMap updatedData = FastMap.create().add("idf", tsCustomerDto.getCustomerId());
        if (StringUtil.isNotEmpty(tsCustomerDto.getName())) {
          updatedData.put("customerName", tsCustomerDto.getName());
        }
        if (StringUtil.isNotEmpty(tsCustomerDto.getTaxCode())) {
          updatedData.put("taxCode", tsCustomerDto.getTaxCode());
        }
        if (StringUtil.isNotEmpty(tsCustomerDto.getPhone())) {
          updatedData.put("mobilePhone", tsCustomerDto.getPhone());
        }
        if (StringUtil.isNotEmpty(tsCustomerDto.getEmail())) {
          updatedData.put("email", tsCustomerDto.getEmail());
        }
        if (StringUtil.isNotEmpty(tsCustomerDto.getAddress())) {
          CustomerAddressDto addressDto = new CustomerAddressDto();
          addressDto.setActive(true);
          addressDto.setAddress(tsCustomerDto.getAddress());
          updatedData.put("addresses", List.of(addressDto));
        }
        List<CustomerReferenceDto> referenceDtos = extractReferences(tsCustomerDto);
        if (ListUtil.isNotEmpty(referenceDtos)) {
          updatedData.put("references", referenceDtos);
        }

        customerDtos.forEach(
            customerDto -> crmCrudClient.updateExistedCustomer(customerDto.getId(), updatedData));
      }
    } catch (Exception e) {
      LOGGER.error("Ignore ts update customer failed with error: {}", e.getMessage(), e);
    }
  }

  private List<CustomerReferenceDto> extractReferences(TsCustomerDto tsCustomerDto) {
    return Stream.of(
            tsCustomerDto.getLicenses().stream()
                .filter(l -> StringUtil.isNotEmpty(l.getPublicURL()))
                .map(
                    l ->
                        new CustomerReferenceDto(
                            Customers.References.PAPER_LICENSE,
                            toBase64(l.getPublicURL()),
                            l.getPublicURL()))
                .toList(),
            tsCustomerDto.getPharmacyEligibilityLicense().stream()
                .filter(l -> StringUtil.isNotEmpty(l.getPublicURL()))
                .map(
                    l ->
                        new CustomerReferenceDto(
                            Customers.References.PAPER_PHARMACY_ELIGIBILITY_LICENSE,
                            toBase64(l.getPublicURL()),
                            l.getPublicURL()))
                .toList(),
            tsCustomerDto.getExaminationAndTreatmentLicense().stream()
                .filter(l -> StringUtil.isNotEmpty(l.getPublicURL()))
                .map(
                    l ->
                        new CustomerReferenceDto(
                            Customers.References.PAPER_EXAMINATION_TREATMENT_LICENSE,
                            toBase64(l.getPublicURL()),
                            l.getPublicURL()))
                .toList(),
            tsCustomerDto.getGpp().stream()
                .filter(l -> StringUtil.isNotEmpty(l.getPublicURL()))
                .map(
                    l ->
                        new CustomerReferenceDto(
                            Customers.References.PAPER_GPP,
                            toBase64(l.getPublicURL()),
                            l.getPublicURL()))
                .toList(),
            tsCustomerDto.getGdp().stream()
                .filter(l -> StringUtil.isNotEmpty(l.getPublicURL()))
                .map(
                    l ->
                        new CustomerReferenceDto(
                            Customers.References.PAPER_GDP,
                            toBase64(l.getPublicURL()),
                            l.getPublicURL()))
                .toList(),
            tsCustomerDto.getGsp().stream()
                .filter(l -> StringUtil.isNotEmpty(l.getPublicURL()))
                .map(
                    l ->
                        new CustomerReferenceDto(
                            Customers.References.PAPER_GSP,
                            toBase64(l.getPublicURL()),
                            l.getPublicURL()))
                .toList())
        .flatMap(List::stream)
        .toList();
  }

  private String toBase64(String url) {
    InputStream inputStream = FileUtil.loadImageInputStream(url);
    try (inputStream;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      if (inputStream == null) {
        return null;
      }
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
      byte[] imageBytes = outputStream.toByteArray();
      return Base64.getEncoder().encodeToString(imageBytes);
    } catch (Exception e) {
      LOGGER.error("Failed to convert image to Base64: {}", e.getMessage(), e);
      return null;
    }
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
  @Scheduled(cron = "0 30 7,9,10,11,12,13,14,15,16,17,18 * * *", zone = "Asia/Ho_Chi_Minh")
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
      syncInvoicesIfNeed(invoiceCodes);
    }
  }

  // branch id không cần do lấy default bên kiotviet service rồi
  private void syncOrdersIfNeed(Long branchId, List<String> orderCodes) {
    List<String> needToSyncOrderCodes =
        wmsCrudClient.findNotExistedOrderFromList(branchId, orderCodes);
    needToSyncOrderCodes.forEach(
        orderCode -> {
          KvOrderDto kvOrderDto = kiotvietServiceClient.syncOrder(orderCode);
          if (kvOrderDto != null) {
            OrderHookDto orderHookDto = transformKvrderDtoToOrderHookDto(kvOrderDto);
            processOrderHookDto(orderHookDto);
          }
        });
  }

  // public to support manual sync one invoice by leader
  // branch id không cần do lấy default bên kiotviet service rồi
  public void syncInvoicesIfNeed(List<String> invoiceCodesFull) {
    // không chỉ đồng bộ invoice cuối, phải đồng bộ tất do cần update status của các invoice trước
    // thành Cancel :(
    List<String> needToSyncInvoiceCodes =
        invoiceCodesFull.stream().map(this::getLastAndValidInvoiceCode).toList();
    needToSyncInvoiceCodes.forEach(
        invoiceCode -> {
          KvInvoiceDto kvInvoiceDto = kiotvietServiceClient.syncInvoice(invoiceCode);
          if (kvInvoiceDto != null) {
            InvoiceHookDto invoiceHookDto = transformKvInvoiceDtoToInvoiceHookDto(kvInvoiceDto);
            processInvoiceHookDto(invoiceHookDto);
          }
        });

    // cancel các hóa đơn hủy
    List<String> needToCancelInvoiceCodes =
        invoiceCodesFull.stream()
            .map(this::getFirstAndValidInvoiceCodes)
            .flatMap(Collection::stream)
            .toList();
    wmsCrudClient.updateBatchInvoiceStatus(
        FastMap.create()
            .add("status", KvStatus.Invoice.CANCELLED)
            .add("codes", needToCancelInvoiceCodes));
  }

  private String getLastAndValidInvoiceCode(String invoiceCodes) {
    List<String> invoiceCodesList = StringUtil.split(invoiceCodes, ",");
    return invoiceCodesList.get(invoiceCodesList.size() - 1).trim();
  }

  private List<String> getFirstAndValidInvoiceCodes(String invoiceCodes) {
    List<String> invoiceCodesList = StringUtil.split(invoiceCodes, ",");
    invoiceCodesList.remove(invoiceCodesList.size() - 1);
    return invoiceCodesList.stream().map(String::trim).toList();
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
