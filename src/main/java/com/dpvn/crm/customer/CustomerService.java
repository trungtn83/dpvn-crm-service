package com.dpvn.crm.customer;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.KiotvietServiceClient;
import com.dpvn.crm.client.WmsCrudClient;
import com.dpvn.crmcrudservice.domain.constant.Customers;
import com.dpvn.crmcrudservice.domain.constant.Genders;
import com.dpvn.crmcrudservice.domain.constant.RelationshipType;
import com.dpvn.crmcrudservice.domain.constant.SaleCustomers;
import com.dpvn.crmcrudservice.domain.constant.Visibility;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.CustomerReferenceDto;
import com.dpvn.crmcrudservice.domain.dto.InteractionDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerStateDto;
import com.dpvn.crmcrudservice.domain.dto.TaskDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.kiotviet.domain.KvCustomerDto;
import com.dpvn.shared.domain.constant.Globals;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.DateUtil;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ListUtil;
import com.dpvn.shared.util.ObjectUtil;
import com.dpvn.shared.util.StringUtil;
import com.dpvn.wmscrudservice.domain.constant.Invoices;
import com.dpvn.wmscrudservice.domain.constant.Orders;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CustomerService extends AbstractService {
  private final CrmCrudClient crmCrudClient;
  private final KiotvietServiceClient kiotvietServiceClient;
  private final SaleCustomerService saleCustomerService;
  private final WmsCrudClient wmsCrudClient;
  private final WebHookHandlerService webHookHandlerService;

  public CustomerService(
      CrmCrudClient crmCrudClient,
      KiotvietServiceClient kiotvietServiceClient,
      SaleCustomerService saleCustomerService,
      WmsCrudClient wmsCrudClient,
      WebHookHandlerService webHookHandlerService) {
    this.crmCrudClient = crmCrudClient;
    this.kiotvietServiceClient = kiotvietServiceClient;
    this.saleCustomerService = saleCustomerService;
    this.wmsCrudClient = wmsCrudClient;
    this.webHookHandlerService = webHookHandlerService;
  }

  private void validateCustomerMobilePhones(CustomerDto customerDto) {
    List<String> mobileReferenceDtos =
        customerDto.getReferences().stream()
            .filter(
                cr ->
                    List.of(Customers.References.MOBILE_PHONE, Customers.References.ZALO)
                        .contains(cr.getCode()))
            .map(CustomerReferenceDto::getValue)
            .collect(Collectors.toList());
    mobileReferenceDtos.add(customerDto.getMobilePhone());
    List<String> duplicatedMobilePhones =
        ListUtil.getDuplicates(mobileReferenceDtos, Objects::toString);
    if (ListUtil.isNotEmpty(duplicatedMobilePhones)) {
      throw new BadRequestException(
          "DUPLICATED",
          String.format(
              "Customer with mobile phone %s is duplicated",
              ListUtil.toString(duplicatedMobilePhones)));
    }

    List<String> errors =
        mobileReferenceDtos.stream()
            .map(mobile -> findCustomerByMobilePhone(customerDto.getId(), mobile))
            .filter(Objects::nonNull)
            .toList();
    if (ListUtil.isNotEmpty(errors)) {
      throw new BadRequestException(
          "DUPLICATED", "Số điện thoại đã tồn tại", ListUtil.toString(errors));
    }
  }

  public void upsertCustomer(CustomerDto customerDto) {
    validateCustomerMobilePhones(customerDto);
    crmCrudClient.upsertCustomer(customerDto);
  }

  public void createNewCustomer(
      Long userId, CustomerDto customerDto, SaleCustomerDto saleCustomerDto) {
    validateCustomerMobilePhones(customerDto);
    CustomerDto result = crmCrudClient.createNewCustomer(customerDto);
    // auto generate code when user leave it empty
    if (StringUtil.isEmpty(result.getCustomerCode())) {
      CustomerDto newOne = new CustomerDto();
      newOne.setId(result.getId());
      newOne.setCustomerCode(String.format("KHA%09d", result.getId()));
      result = crmCrudClient.upsertCustomer(newOne);
    }
    if (saleCustomerDto.getSaleId() != null) {
      saleCustomerDto.setCustomerId(result.getId());
      saleCustomerDto.setCustomerDto(result);
      saleCustomerDto.setReasonRef(userId.toString());
      saleCustomerDto.setReasonNote("Được phân công khi tạo mới khách hàng");
      saleCustomerDto.setRelationshipType(RelationshipType.PIC);
      saleCustomerDto.setReasonId(SaleCustomers.Reason.LEADER);
      saleCustomerDto.setActive(Boolean.TRUE);
      saleCustomerService.upsertSaleCustomer(saleCustomerDto);
    }
  }

  public void updateExistedCustomer(
      Long userId, CustomerDto customerDto, SaleCustomerDto saleCustomerDto) {
    Long customerId = customerDto.getId();
    CustomerDto existedCustomer = crmCrudClient.findCustomerById(customerId);
    if (existedCustomer == null) {
      throw new BadRequestException(String.format("Customer with id %s not found", customerId));
    }
    if (StringUtil.isEmpty(customerDto.getCustomerCode())
        || !customerDto.getCustomerCode().equals(existedCustomer.getCustomerCode())) {
      throw new BadRequestException(
          String.format("Customer code %s is not valid", customerDto.getCustomerCode()));
    }
    if (StringUtil.isEmpty(customerDto.getMobilePhone())
        || !customerDto.getMobilePhone().equals(existedCustomer.getMobilePhone())) {
      throw new BadRequestException(
          String.format("Mobile phone %s is not valid", customerDto.getMobilePhone()));
    }

    validateCustomerMobilePhones(customerDto);
    CustomerDto newOne = crmCrudClient.updateExistedCustomer(customerId, customerDto);

    SaleCustomerDto existedSaleCustomer =
        saleCustomerService.findSaleCustomerByReason(
            null, customerDto.getId(), RelationshipType.PIC, SaleCustomers.Reason.LEADER, null);
    if (existedSaleCustomer != null) {
      crmCrudClient.deleteSaleCustomer(existedSaleCustomer.getId());
    }
    if (saleCustomerDto.getSaleId() != null) {
      saleCustomerDto.setCustomerId(customerId);
      saleCustomerDto.setCustomerDto(newOne);
      saleCustomerDto.setReasonRef(userId.toString());
      saleCustomerDto.setReasonNote("Được phân công khi cập nhật khách hàng");
      saleCustomerDto.setRelationshipType(RelationshipType.PIC);
      saleCustomerDto.setReasonId(SaleCustomers.Reason.LEADER);
      saleCustomerDto.setActive(Boolean.TRUE);
      saleCustomerService.upsertSaleCustomer(saleCustomerDto);
    }
  }

  private SaleCustomerDto initSaleCustomerDto(Long customerId) {
    CustomerDto customerDto = crmCrudClient.findCustomerById(customerId);
    SaleCustomerDto saleCustomerDto = new SaleCustomerDto();
    saleCustomerDto.setCustomerId(customerId);
    saleCustomerDto.setCustomerDto(customerDto);
    return saleCustomerDto;
  }

  public CustomerDto findCustomerById(Long id) {
    return crmCrudClient.findCustomerById(id);
  }

  public FastMap findCustomerOfSale(Long userId, Long customerId) {
    FastMap condition =
        FastMap.create().add("saleId", userId).add("customerIds", List.of(customerId));
    List<SaleCustomerDto> saleCustomerDtos =
        crmCrudClient.findSaleCustomersByOptions(condition).stream()
            .filter(
                sc -> sc.getAvailableTo() == null || sc.getAvailableTo().isAfter(DateUtil.now()))
            .toList();

    SaleCustomerDto mySaleCustomerDto =
        ListUtil.isEmpty(saleCustomerDtos)
            ? initSaleCustomerDto(customerId)
            : saleCustomerDtos.get(0);
    List<SaleCustomerStateDto> lastStates =
        crmCrudClient.findLatestBySaleIdAndCustomerIds(
            FastMap.create()
                .add("saleId", userId)
                .add("customerIds", List.of(mySaleCustomerDto.getCustomerId())));
    SaleCustomerStateDto myLastStateDto =
        ListUtil.isEmpty(lastStates) ? new SaleCustomerStateDto() : lastStates.get(0);

    CustomerDto customerDto = mySaleCustomerDto.getCustomerDto();
    mySaleCustomerDto.setCustomerDto(null);
    return FastMap.create()
        .add("customer", customerDto)
        .add("saleCustomer", mySaleCustomerDto)
        .add("lastState", myLastStateDto);
  }

  /**
   * @param customerId: check for other customer than this customerId
   * @param mobilePhone
   * @return
   */
  public String findCustomerByMobilePhone(Long customerId, String mobilePhone) {
    List<CustomerDto> customerDtos =
        crmCrudClient.findCustomersByMobilePhone(mobilePhone).stream()
            .filter(c -> !Objects.equals(c.getId(), customerId))
            .toList();
    if (ListUtil.isEmpty(customerDtos)) {
      return null;
    }
    CustomerDto exited = customerDtos.get(0);
    List<String> exitedPhones =
        exited.getReferences().stream()
            .filter(r -> Customers.References.MOBILE_PHONE.equals(r.getCode()))
            .map(CustomerReferenceDto::getValue)
            .toList();
    List<String> exitedZalos =
        exited.getReferences().stream()
            .filter(r -> Customers.References.ZALO.equals(r.getCode()))
            .map(CustomerReferenceDto::getValue)
            .toList();
    if (ObjectUtil.equals(exited.getMobilePhone(), mobilePhone)) {
      return String.format(
          "Số điện thoại %s đã được đăng kí bởi %s", mobilePhone, exited.getCustomerName());
    } else if (exitedPhones.contains(mobilePhone)) {
      return String.format(
          "Số điện thoại %s đang là số điện thoại của %s", mobilePhone, exited.getCustomerName());
    } else if (exitedZalos.contains(mobilePhone)) {
      return String.format(
          "Số điện thoại %s đang là số ZALO của %s", mobilePhone, exited.getCustomerName());
    }
    return null;
  }

  public FastMap findMyCustomers(FastMap condition) {
    return crmCrudClient.findMyCustomers(condition);
  }

  public FastMap findInPoolCustomers(FastMap condition) {
    return crmCrudClient.findInPoolCustomers(condition);
  }

  public FastMap findTaskBasedCustomers(FastMap condition) {
    return crmCrudClient.findTaskBasedCustomers(condition);
  }

  public void upsertSaleCustomerState(SaleCustomerStateDto saleCustomerStateDto) {
    crmCrudClient.upsertSaleCustomerState(saleCustomerStateDto);
  }

  public void assignCustomer(SaleCustomerDto body) {
    crmCrudClient.assignCustomer(body);
  }

  public void revokeCustomer(SaleCustomerDto body) {
    crmCrudClient.revokeCustomer(body);
  }

  public void assignCustomers(FastMap body) {
    crmCrudClient.assignCustomers(body);
  }

  public void revokeCustomers(FastMap body) {
    crmCrudClient.revokeCustomers(body);
  }

  public List<InteractionDto> getAllInteractions(Long saleId, Long customerId) {
    List<InteractionDto> interactionDtos =
        crmCrudClient.getAllInteractions(null, customerId, null, null);
    return interactionDtos.stream()
        .filter(
            i ->
                Objects.equals(i.getInteractBy(), saleId) || i.getVisibility() == Visibility.PUBLIC)
        .toList();
  }

  public void upsertInteraction(InteractionDto body) {
    crmCrudClient.upsertInteraction(body);
  }

  public List<TaskDto> getAllTasks(Long saleId, Long customerId) {
    return crmCrudClient.getAllTasks(saleId, customerId, null, null, null);
  }

  public void upsertTask(TaskDto body) {
    crmCrudClient.upsertTask(body);
  }

  public void updateLastTransaction(Long customerId, FastMap body) {
    crmCrudClient.updateLastTransaction(customerId, body);
  }

  public CustomerDto findCustomerByKvCustomerId(Long saleId, Long kvCustomerId) {
    KvCustomerDto kvCustomerDto = kiotvietServiceClient.findKvCustomerById(kvCustomerId);
    return findOrCreateCustomerByMobilePhone(saleId, kvCustomerDto);
  }

  private CustomerDto findOrCreateCustomerByMobilePhone(Long saleId, KvCustomerDto kvCustomerDto) {
    String mobilePhone = kvCustomerDto.getContactNumber();
    List<CustomerDto> customerDto = crmCrudClient.findCustomersByMobilePhone(mobilePhone);
    if (ListUtil.isEmpty(customerDto)) {
      // create new Customer here
      CustomerDto newCustomerDto = new CustomerDto();
      newCustomerDto.setIdf(kvCustomerDto.getId());
      newCustomerDto.setCustomerCode(kvCustomerDto.getCode());
      newCustomerDto.setCustomerName(kvCustomerDto.getName());
      newCustomerDto.setGender(
          Objects.equals(kvCustomerDto.getGender(), Boolean.TRUE) ? Genders.MALE : Genders.FEMALE);
      newCustomerDto.setMobilePhone(mobilePhone);
      newCustomerDto.setAddress(kvCustomerDto.getAddress());
      newCustomerDto.setLevelPoint(kvCustomerDto.getRewardPoint().intValue());
      newCustomerDto.setSourceId(Customers.Source.KIOTVIET);
      newCustomerDto.setCreatedBy(saleId);
      newCustomerDto.setModifiedBy(saleId);
      return crmCrudClient.createNewCustomer(newCustomerDto);
    } else {
      return customerDto.get(0);
    }
  }

  public void doActionCustomer(Long saleId, Long customerId, Integer reasonId, boolean flag) {
    SaleCustomerDto saleCustomerDto =
        saleCustomerService.findSaleCustomerByReason(
            saleId, customerId, RelationshipType.INVOLVED, reasonId, null);
    if (flag) {
      if (saleCustomerDto == null) {
        SaleCustomerDto newSaleCustomerDto = new SaleCustomerDto();
        newSaleCustomerDto.setSaleId(saleId);
        newSaleCustomerDto.setCustomerId(customerId);
        newSaleCustomerDto.setRelationshipType(RelationshipType.INVOLVED);
        newSaleCustomerDto.setReasonId(reasonId);
        newSaleCustomerDto.setActive(Boolean.TRUE);
        saleCustomerService.upsertSaleCustomer(newSaleCustomerDto);
      }
    } else {
      if (saleCustomerDto != null) {
        saleCustomerService.removeSaleCustomerByReason(saleId, customerId, reasonId, null);
      }
    }
  }

  public void initRelationship() {
    int page = 0;
    while (true) {
      List<CustomerDto> customerDtos =
          crmCrudClient.findByStatus(null, page, Globals.Paging.FETCHING_PAGE_SIZE);
      if (ListUtil.isEmpty(customerDtos)) {
        return;
      }
      List<Long> idfs = customerDtos.stream().map(CustomerDto::getIdf).toList();
      List<FastMap> lastTempOrders = findLastOrderOfCustomerByStatus(Orders.Status.TEMP, idfs);
      List<FastMap> lastConfirmedOrders =
          findLastOrderOfCustomerByStatus(Orders.Status.CONFIRMED, idfs);
      List<FastMap> lastInProgressInvoices =
          findLastInvoiceOfCustomerByStatus(Invoices.Status.DELIVERING, idfs);
      List<FastMap> lastCompletedInvoices =
          findLastInvoiceOfCustomerByStatus(Invoices.Status.COMPLETED, idfs);
      Map<Long, UserDto> sellers =
          crmCrudClient.getUsers().stream().collect(Collectors.toMap(UserDto::getIdf, u -> u));
      customerDtos.forEach(
          customerDto -> {
            lastTempOrders.forEach(
                tempOrder -> {
                  UserDto userDto = sellers.get(tempOrder.getLong("sellerId"));
                  String orderCode = tempOrder.getString("orderCode");
                  Instant purchaseDate = tempOrder.getInstant("purchaseDate");
                  webHookHandlerService.handleTempOrder(
                      userDto, customerDto, orderCode, purchaseDate);
                });
            lastConfirmedOrders.forEach(
                confirmedOrder -> {
                  UserDto userDto = sellers.get(confirmedOrder.getLong("sellerId"));
                  String orderCode = confirmedOrder.getString("orderCode");
                  Instant purchaseDate = confirmedOrder.getInstant("purchaseDate");
                  webHookHandlerService.handleConfirmedOrder(
                      userDto, customerDto, orderCode, purchaseDate);
                });
            lastInProgressInvoices.forEach(
                inProgressInvoice -> {
                  UserDto userDto = sellers.get(inProgressInvoice.getLong("sellerId"));
                  String invoiceCode = inProgressInvoice.getString("invoiceCode");
                  Instant purchaseDate = inProgressInvoice.getInstant("purchaseDate");
                  webHookHandlerService.handleInProgressInvoice(
                      userDto, customerDto, invoiceCode, purchaseDate);
                });
            lastCompletedInvoices.forEach(
                completedInvoice -> {
                  UserDto userDto = sellers.get(completedInvoice.getLong("sellerId"));
                  String invoiceCode = completedInvoice.getString("invoiceCode");
                  Instant purchaseDate = completedInvoice.getInstant("purchaseDate");
                  webHookHandlerService.handleCompletedInvoice(
                      userDto, customerDto, invoiceCode, purchaseDate);
                });

            updateCustomerStatus(
                customerDto.getId(), customerDto.getIdf(), Customers.Status.ASSIGNED);
            LOGGER.info("Initialized relationship for customer {}", customerDto.getCustomerName());
          });
      page++;
      if (customerDtos.size() < Globals.Paging.FETCHING_PAGE_SIZE) {
        return;
      }
    }
  }

  private void updateCustomerStatus(Long id, Long idf, String status) {
    CustomerDto customerDto = new CustomerDto();
    customerDto.setId(id);
    customerDto.setIdf(idf);
    customerDto.setStatus(status);
    crmCrudClient.upsertCustomer(customerDto);
  }

  private List<FastMap> findLastOrderOfCustomerByStatus(String status, List<Long> customerIds) {
    return wmsCrudClient.findLastPurchaseOrderByStatusAndCustomers(
        FastMap.create().add("status", status).add("customerIds", customerIds));
  }

  private List<FastMap> findLastInvoiceOfCustomerByStatus(String status, List<Long> customerIds) {
    return wmsCrudClient.findLastPurchaseInvoiceByStatusAndCustomers(
        FastMap.create().add("status", status).add("customerIds", customerIds));
  }
}
