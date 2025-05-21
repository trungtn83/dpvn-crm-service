package com.dpvn.crm.customer;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.KiotvietServiceClient;
import com.dpvn.crm.client.WmsCrudClient;
import com.dpvn.crm.interaction.InteractionService;
import com.dpvn.crm.interaction.InteractionUtil;
import com.dpvn.crm.user.UserService;
import com.dpvn.crm.user.UserUtil;
import com.dpvn.crm.webhook.WebHookHandlerService;
import com.dpvn.crmcrudservice.domain.constant.Customers;
import com.dpvn.crmcrudservice.domain.constant.RelationshipType;
import com.dpvn.crmcrudservice.domain.constant.SaleCustomers;
import com.dpvn.crmcrudservice.domain.dto.CustomerAddressDto;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.CustomerReferenceDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerStateDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CustomerService extends AbstractService {
  private final CrmCrudClient crmCrudClient;
  private final KiotvietServiceClient kiotvietServiceClient;
  private final SaleCustomerService saleCustomerService;
  private final WmsCrudClient wmsCrudClient;
  private final WebHookHandlerService webHookHandlerService;
  private final InteractionService interactionService;
  private final UserService userService;

  public CustomerService(
      CrmCrudClient crmCrudClient,
      KiotvietServiceClient kiotvietServiceClient,
      SaleCustomerService saleCustomerService,
      WmsCrudClient wmsCrudClient,
      WebHookHandlerService webHookHandlerService,
      InteractionService interactionService,
      UserService userService) {
    this.crmCrudClient = crmCrudClient;
    this.kiotvietServiceClient = kiotvietServiceClient;
    this.saleCustomerService = saleCustomerService;
    this.wmsCrudClient = wmsCrudClient;
    this.webHookHandlerService = webHookHandlerService;
    this.interactionService = interactionService;
    this.userService = userService;
  }

  private void validateCustomerMobilePhones(
      Long customerId,
      String mobilePhone,
      List<String> mobileReferences,
      List<String> zaloReferences) {
    // check inside current customer
    List<String> checkLists = new ArrayList<>(mobileReferences);
    checkLists.add(mobilePhone);
    List<String> duplicatedMobilePhones = ListUtil.getDuplicates(checkLists, Objects::toString);
    if (ListUtil.isNotEmpty(duplicatedMobilePhones)) {
      throw new BadRequestException(
          "DUPLICATED",
          String.format("Số điện thoại %s bị trùng", ListUtil.toString(duplicatedMobilePhones)));
    }

    // check list zalo inside it
    List<String> duplicatedZalos = ListUtil.getDuplicates(zaloReferences, Objects::toString);
    if (ListUtil.isNotEmpty(duplicatedZalos)) {
      throw new BadRequestException(
          "DUPLICATED",
          String.format(
              "Số điện thoại %s sử dụng zalo bị trùng", ListUtil.toString(duplicatedMobilePhones)));
    }

    // add zalo number to checklist
    Set<String> uniqueMobiles = new HashSet<>(checkLists);
    uniqueMobiles.addAll(zaloReferences);

    // check with other customers
    List<String> errors =
        uniqueMobiles.stream()
            .map(mobile -> findCustomerByMobilePhone(customerId, mobile))
            .filter(Objects::nonNull)
            .toList();
    if (ListUtil.isNotEmpty(errors)) {
      throw new BadRequestException(
          "DUPLICATED", "Số điện thoại đã tồn tại", ListUtil.toString(errors));
    }
  }

  // status: true/false -> can create or not new customer
  // if false: user: "Số điện thoại đã được đăng kí bởi %s"
  // if false: room: là vàng của ai / đang trong kho vàng, đang trong bãi cát...
  public FastMap validateMobilePhoneNewCustomer(Long saleId, String mobilePhone) {
    List<CustomerDto> customerDtos =
        crmCrudClient.findCustomersByMobilePhone(mobilePhone).stream().toList();
    // if mobile phone existed
    if (ListUtil.isNotEmpty(customerDtos)) {
      CustomerDto customerDto = customerDtos.get(0);
      List<SaleCustomerDto> saleCustomerDtos =
          crmCrudClient.findSaleCustomersByOptions(
              FastMap.create().add("customerIds", List.of(customerDto.getId())));
      // if mobile phone existed and assigned to sale
      if (ListUtil.isNotEmpty(saleCustomerDtos)) {
        List<SaleCustomerDto> assignedSaleCustomerDtos =
            saleCustomerDtos.stream()
                .filter(
                    sc ->
                        List.of(
                                SaleCustomers.Reason.INVOICE,
                                SaleCustomers.Reason.ORDER,
                                SaleCustomers.Reason.CAMPAIGN,
                                SaleCustomers.Reason.LEADER)
                            .contains(sc.getReasonId()))
                .toList();
        if (ListUtil.isNotEmpty(assignedSaleCustomerDtos)) {
          return FastMap.create()
              .add("active", false)
              .add("customer", customerDto)
              .add("reasonId", assignedSaleCustomerDtos.get(0).getReasonId())
              .add("saleId", assignedSaleCustomerDtos.get(0).getSaleId());
        }
        List<SaleCustomerDto> ownSaleCustomerDtos =
            saleCustomerDtos.stream()
                .filter(
                    sc ->
                        !saleId.equals(sc.getSaleId())
                            && SaleCustomers.Reason.MY_HANDS.contains(sc.getReasonId()))
                .toList();
        if (ListUtil.isNotEmpty(ownSaleCustomerDtos)) {
          return FastMap.create()
              .add("active", true)
              .add("customer", customerDto)
              .add("saleId", ownSaleCustomerDtos.get(0).getSaleId());
        } else {
          return FastMap.create().add("active", false).add("customer", customerDto);
        }
      }

      // if mobile phone existed and did NOT assign to any sale
      return FastMap.create().add("active", true).add("customer", customerDto);
    }

    // if mobile phone does not exist
    return FastMap.create().add("active", true);
  }

  public void createNewCustomer(Long userId, FastMap body) {
    CustomerDto customerDto = extractCustomerFromBody(body);
    SaleCustomerDto saleCustomerDto = extractSaleCustomerFromBody(body);
    boolean isActive = customerDto.getActive();

    // in case of create new customer but id existed
    // that mean sale 2 find also customer that created (tự đào) by sale 1
    //  or đang tồn tại trong kho vàng hoặc bãi cát mà tự tìm ra số đt ở đâu đó
    if (customerDto.getId() != null) {
      CustomerDto existedCustomer = crmCrudClient.findCustomerById(customerDto.getId());
      if (existedCustomer == null) {
        throw new BadRequestException(
            String.format("Customer with id %s not found", customerDto.getId()));
      }
      saleCustomerDto.setCustomerId(customerDto.getId());
      saleCustomerDto.setCustomerDto(existedCustomer);
      saleCustomerDto.setRelationshipType(RelationshipType.PIC);
      saleCustomerDto.setSaleId(userId);
      saleCustomerDto.setActive(true);
      saleCustomerDto.setDeleted(false);
      saleCustomerDto.setReasonId(
          isActive
              ? SaleCustomers.Reason.BY_MY_HAND_FROM_GOLDMINE
              : SaleCustomers.Reason.BY_MY_HAND_FROM_SANDBANK);
      saleCustomerDto.setReasonRef(userId.toString());
      saleCustomerDto.setReasonNote("Đào được khách này nhưng đã có trong hệ thống rồi");
      saleCustomerService.createNewSaleCustomer(saleCustomerDto);

      String content =
          "Đào được khách hàng từ " + (isActive ? "Kho vàng" : "Bãi cát") + " màn hình tạo mới";
      interactionService.createInteraction(
          InteractionUtil.generateSystemInteraction(userId, customerDto.getId(), null, content));
    } else {
      // TODO: call to function validateMobilePhoneNewCustomer first to check if mobile phone is
      // valid
      customerDto.setCreatedBy(userId);
      customerDto.setActive(true);
      customerDto.setStatus(Customers.Status.VERIFIED);
      CustomerDto result =
          customerDto.getId() == null
              ? crmCrudClient.createNewCustomer(customerDto)
              : crmCrudClient.updateExistedCustomer(
              customerDto.getId(),
              FastMap.create().add("active", true).add("status", Customers.Status.VERIFIED));
      // auto generate code when user leave it empty
      if (StringUtil.isEmpty(result.getCustomerCode())) {
        result =
            crmCrudClient.updateExistedCustomer(
                result.getId(),
                FastMap.create().add("customerCode", String.format("KHA%09d", result.getId())));
      }
      assignCustomerToSaleInUpsertScreen(
          userId, customerDto.getId(), result, saleCustomerDto, isActive);
      String content = "Tạo mới khách hàng chưa có trong hệ thống và phân công cho chính mình";
      interactionService.createInteraction(
          InteractionUtil.generateSystemInteraction(userId, customerDto.getId(), null, content));
    }
  }

  private CustomerDto extractCustomerFromBody(FastMap body) {
    CustomerDto customerDto = new CustomerDto();
    customerDto.setId(body.getLong("id"));
    customerDto.setActive(body.getBoolean("active"));
    customerDto.setCustomerCode(body.getString("customerCode"));
    customerDto.setCustomerName(body.getString("customerName"));
    customerDto.setBirthday(body.getInstant("birthday"));
    customerDto.setGender(body.getInt("gender"));
    customerDto.setMobilePhone(body.getString("mobilePhone"));
    customerDto.setEmail(body.getString("email"));

    customerDto.setAddresses(body.getListClass("addresses", CustomerAddressDto.class));

    customerDto.setTaxCode(body.getString("taxCode"));
    customerDto.setPinCode(body.getString("pinCode"));
    customerDto.setCustomerTypeId(body.getLong("customerTypeId"));
    customerDto.setSourceId(body.getInt("sourceId"));
    customerDto.setIdf(body.getLong("customerId"));
    customerDto.setReferences(body.getListClass("references", CustomerReferenceDto.class));
    return customerDto;
  }

  private SaleCustomerDto extractSaleCustomerFromBody(FastMap body) {
    SaleCustomerDto saleCustomerDto = new SaleCustomerDto();
    saleCustomerDto.setSaleId(body.getLong("pic"));

    List<String> availableDate = body.getList("availableDate");
    if (ListUtil.isNotEmpty(availableDate)) {
      saleCustomerDto.setAvailableFrom(DateUtil.from(availableDate.get(0)));
      saleCustomerDto.setAvailableTo(DateUtil.from(availableDate.get(1)));
    }
    return saleCustomerDto;
  }

  private void assignCustomerToSaleInUpsertScreen(
      Long userId,
      Long customerId,
      CustomerDto customerDto,
      SaleCustomerDto saleCustomerDto,
      boolean isActive) {
    if (saleCustomerDto.getSaleId() != null) {
      // in case of assign to sale, need to enable this customer
      if (!customerDto.getActive() && !Customers.Status.VERIFIED.equals(customerDto.getStatus())) {
        CustomerDto result =
            crmCrudClient.updateExistedCustomer(
                customerId,
                FastMap.create().add("active", true).add("status", Customers.Status.VERIFIED));
        saleCustomerDto.setCustomerDto(result);
      } else {
        saleCustomerDto.setCustomerDto(customerDto);
      }

      saleCustomerDto.setCustomerId(customerDto.getId());
      saleCustomerDto.setRelationshipType(RelationshipType.PIC);
      saleCustomerDto.setActive(true);
      saleCustomerDto.setDeleted(false);
      if (saleCustomerDto.getSaleId().equals(userId)) {
        int reasonId =
            customerId == null
                ? SaleCustomers.Reason.BY_MY_HAND
                : (isActive
                ? SaleCustomers.Reason.BY_MY_HAND_FROM_GOLDMINE
                : SaleCustomers.Reason.BY_MY_HAND_FROM_SANDBANK);
        saleCustomerDto.setReasonId(reasonId);
        saleCustomerDto.setReasonRef(userId.toString());
        saleCustomerDto.setReasonNote("Tạo khách hàng từ màn hình tạo mới");
      } else {
        // available time is set in FE side
        saleCustomerDto.setReasonId(SaleCustomers.Reason.LEADER);
        saleCustomerDto.setReasonRef(userId.toString());
        saleCustomerDto.setReasonNote("Được phân công khi tạo mới khách hàng");
      }
      saleCustomerService.createNewSaleCustomer(saleCustomerDto);
    }
  }

  public void updateExistedCustomer(Long userId, Long customerId, FastMap customerDto) {
    CustomerDto existedCustomer = crmCrudClient.findCustomerById(customerId);
    if (existedCustomer == null) {
      throw new BadRequestException(String.format("Customer with id %s not found", customerId));
    }
    boolean isActive = existedCustomer.getActive();
    validateCustomerMobilePhones(
        customerId,
        customerDto.getString("mobilePhone"),
        customerDto.getListClass("references", CustomerReferenceDto.class).stream()
            .filter(cr -> Customers.References.MOBILE_PHONE.equals(cr.getCode()))
            .map(CustomerReferenceDto::getValue)
            .toList(),
        customerDto.getListClass("references", CustomerReferenceDto.class).stream()
            .filter(cr -> Customers.References.ZALO.equals(cr.getCode()))
            .map(CustomerReferenceDto::getValue)
            .toList());
    CustomerDto result = crmCrudClient.updateExistedCustomer(customerId, customerDto);

    assignCustomerToSaleInUpsertScreen(
        userId, customerId, result, extractSaleCustomerFromBody(customerDto), isActive);
  }

  public void deleteCustomer(Long saleId, Long customerId, String owner) {
    CustomerDto dbCustomerDto = findCustomerById(customerId);
    if (dbCustomerDto == null) {
      throw new BadRequestException(String.format("Customer with id %s not found", customerId));
    }

    String content = "Không đào được khách hàng này tiếp, đưa lại về ";

    // delete forever when GOD do it without owner, or no mobile phone (trash data)
    if ((userService.isGod(saleId) && StringUtil.isEmpty(owner))
        || StringUtil.isEmpty(dbCustomerDto.getMobilePhone())) {
      crmCrudClient.deleteCustomer(customerId);
      content = null;
    } else {
      if (Customers.Owner.GOLDMINE.equals(owner)) {
        content += "Kho vàng";
      } else {
        crmCrudClient.updateExistedCustomer(
            customerId, FastMap.create().add("status", null).add("active", false));
        content += "Bãi cát";
      }
    }
    SaleCustomerDto saleCustomerDto = new SaleCustomerDto();
    saleCustomerDto.setSaleId(saleId);
    saleCustomerDto.setCustomerId(customerId);
    crmCrudClient.removeSaleCustomerByOptions(saleCustomerDto);

    if (StringUtil.isNotEmpty(content)) {
      interactionService.createInteraction(
          InteractionUtil.generateSystemInteraction(saleId, customerId, null, content));
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

  public FastMap findCustomerOfSale(Long saleId, Long customerId) {
    FastMap condition = FastMap.create().add("customerIds", List.of(customerId));
    List<SaleCustomerDto> saleCustomerDtos = crmCrudClient.findSaleCustomersByOptions(condition);
    List<SaleCustomerDto> mySaleCustomerDtos =
        saleCustomerDtos.stream().filter(sc -> sc.getSaleId().equals(saleId)).toList();

    SaleCustomerDto mySaleCustomerDto =
        ListUtil.isEmpty(mySaleCustomerDtos)
            ? initSaleCustomerDto(customerId)
            : mySaleCustomerDtos.get(0);
    List<SaleCustomerStateDto> lastStates =
        crmCrudClient.findLatestBySaleIdAndCustomerIds(
            FastMap.create()
                .add("saleId", saleId)
                .add("customerIds", List.of(mySaleCustomerDto.getCustomerId())));
    SaleCustomerStateDto myLastStateDto =
        ListUtil.isEmpty(lastStates) ? new SaleCustomerStateDto() : lastStates.get(0);

    CustomerDto customerDto = mySaleCustomerDto.getCustomerDto();
    mySaleCustomerDto.setCustomerDto(null);

    UserDto userDto = userService.findById(saleId);
    List<Long> transferIds = userDto.getJudasMemberIds();
    Long userId =
        (UserUtil.isDemiGod(userDto) || UserUtil.isAccount(userDto) || UserUtil.isAdmin(userDto))
            ? null
            : saleId;
    List<Long> saleIds =
        userId == null
            ? List.of()
            : Stream.of(transferIds, List.of(saleId)).flatMap(List::stream).toList();
    return FastMap.create()
        .add("customer", customerDto)
        .add("saleCustomer", mySaleCustomerDto)
        .add("lastState", myLastStateDto)
        .add(
            "isMyCustomer",
            SaleCustomers.Reason.MY_OWN_HANDS.contains(mySaleCustomerDto.getReasonId()))
        .add("owner", CustomerUtil.getCustomerOwner(saleIds, customerDto, saleCustomerDtos));
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

  public FastMap findInOceanCustomers(FastMap condition) {
    return crmCrudClient.findInOceanCustomers(condition);
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

  public void updateLastTransaction(Long customerId, FastMap body) {
    crmCrudClient.updateLastTransaction(customerId, body);
  }

  public CustomerDto findCustomerByKvCustomerId(Long kvCustomerId) {
    // TODO: sometimes customer update event did not send first, we receive update order, invoice
    CustomerDto customerDto = crmCrudClient.findCustomerByIdf(kvCustomerId);
    if (customerDto == null) {
      kiotvietServiceClient.syncCustomer(kvCustomerId);
      customerDto = crmCrudClient.findCustomerByIdf(kvCustomerId);
    }
    return customerDto;
  }

  public void initRelationship() {
    while (true) {
      List<CustomerDto> customerDtos =
          crmCrudClient.findByStatusForInitRelationship(0, Globals.Paging.FETCHING_PAGE_SIZE);
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
          crmCrudClient.getUsers(-1, -1).getRows().stream()
              .collect(Collectors.toMap(UserDto::getIdf, u -> u));
      customerDtos.forEach(
          customerDto -> {
            lastTempOrders.stream()
                .filter(o -> o.getLong("customerId").equals(customerDto.getIdf()))
                .forEach(
                    tempOrder -> {
                      UserDto userDto = sellers.get(tempOrder.getLong("sellerId"));
                      if (userDto != null) { // TODO: some case user is deleted, DO NOT KNOW WHY?
                        String orderCode = tempOrder.getString("orderCode");
                        Instant purchaseDate = tempOrder.getInstant("purchaseDate");
                        webHookHandlerService.handleTempOrder(
                            userDto, customerDto, orderCode, purchaseDate);
                      }
                    });
            lastConfirmedOrders.stream()
                .filter(o -> o.getLong("customerId").equals(customerDto.getIdf()))
                .forEach(
                    confirmedOrder -> {
                      UserDto userDto = sellers.get(confirmedOrder.getLong("sellerId"));
                      if (userDto != null) {
                        String orderCode = confirmedOrder.getString("orderCode");
                        Instant purchaseDate = confirmedOrder.getInstant("purchaseDate");
                        webHookHandlerService.handleConfirmedOrder(
                            userDto, customerDto, orderCode, purchaseDate);
                      }
                    });
            lastInProgressInvoices.stream()
                .filter(i -> i.getLong("customerId").equals(customerDto.getIdf()))
                .forEach(
                    inProgressInvoice -> {
                      UserDto userDto = sellers.get(inProgressInvoice.getLong("sellerId"));
                      if (userDto != null) {
                        String invoiceCode = inProgressInvoice.getString("invoiceCode");
                        Instant purchaseDate = inProgressInvoice.getInstant("purchaseDate");
                        webHookHandlerService.handleInProgressInvoice(
                            userDto, customerDto, invoiceCode, purchaseDate);
                      }
                    });
            lastCompletedInvoices.stream()
                .filter(i -> i.getLong("customerId").equals(customerDto.getIdf()))
                .forEach(
                    completedInvoice -> {
                      UserDto userDto = sellers.get(completedInvoice.getLong("sellerId"));
                      if (userDto != null) {
                        String invoiceCode = completedInvoice.getString("invoiceCode");
                        Instant purchaseDate = completedInvoice.getInstant("purchaseDate");
                        webHookHandlerService.handleCompletedInvoice(
                            userDto, customerDto, invoiceCode, purchaseDate);
                      }
                    });

            crmCrudClient.updateExistedCustomer(
                customerDto.getId(), FastMap.create().add("status", Customers.Status.VERIFIED));
            LOGGER.info("Initialized relationship for customer {}", customerDto.getCustomerName());
          });
      if (customerDtos.size() < Globals.Paging.FETCHING_PAGE_SIZE) {
        return;
      }
    }
  }

  private List<FastMap> findLastOrderOfCustomerByStatus(String status, List<Long> customerIds) {
    return wmsCrudClient.findLastPurchaseOrderByStatusAndCustomers(
        FastMap.create().add("status", status).add("customerIds", customerIds));
  }

  private List<FastMap> findLastInvoiceOfCustomerByStatus(String status, List<Long> customerIds) {
    return wmsCrudClient.findLastPurchaseInvoiceByStatusAndCustomers(
        FastMap.create().add("status", status).add("customerIds", customerIds));
  }

  public void approveFromSandToGold(Long id, FastMap body) {
    crmCrudClient.approveCustomerFromSandToGold(id, body);
  }

  public void digCustomerFromOceanOrGoldmineToGold(Long saleId, Long customerId, String owner) {
    CustomerDto customerDto = crmCrudClient.findCustomerById(customerId);
    // TODO: validate (far future)

    // update customer active if it comes rom SANDBANK
    // TODO: remove to handle case sale 2 can find this customer when sale 1 dig it
    //    if (Customers.Owner.SANDBANK.equals(owner)) {
    //      crmCrudClient.updateExistedCustomer(
    //          customerId,
    //          FastMap.create().add("active", true).add("status", Customers.Status.VERIFIED));
    //    }

    // init relationship
    SaleCustomerDto saleCustomerDto = new SaleCustomerDto();
    saleCustomerDto.setSaleId(saleId);
    saleCustomerDto.setCustomerId(customerId);
    saleCustomerDto.setCustomerDto(customerDto);
    saleCustomerDto.setRelationshipType(RelationshipType.PIC);
    saleCustomerDto.setActive(true);
    saleCustomerDto.setDeleted(false);
    saleCustomerDto.setReasonId(
        Customers.Owner.GOLDMINE.equals(owner)
            ? SaleCustomers.Reason.BY_MY_HAND_FROM_GOLDMINE
            : SaleCustomers.Reason.BY_MY_HAND_FROM_SANDBANK);
    saleCustomerDto.setReasonRef(saleId.toString());
    saleCustomerDto.setReasonNote("Đào khách hàng từ " + Customers.Owners.get(owner));

    saleCustomerService.createNewSaleCustomer(saleCustomerDto);

    // inject interaction for this customer
    interactionService.createInteraction(
        InteractionUtil.generateSystemInteraction(
            saleId, customerId, null, "Đào khách hàng từ " + Customers.Owners.get(owner)));
  }

  /**
   * Find customer status: return that customer if existed and owner
   * If onwer does not exist too (means it is in GOLD_MINE or SAND
   */
  public FastMap getCustomerByMobilePhoneStatus(String mobilePhone) {
    List<CustomerDto> customerDtos = crmCrudClient.findCustomersByMobilePhone(mobilePhone);
    if (ListUtil.isEmpty(customerDtos)) {
      // phone does not exist in system crm, create new one in kiotviet
      return FastMap.create().add("owner", FastMap.create());
    } else {
      CustomerDto customerDto = customerDtos.get(0);
      FastMap condition = FastMap.create().add("customerIds", List.of(customerDto.getId()));
      List<SaleCustomerDto> saleCustomerDtos = crmCrudClient.findSaleCustomersByOptions(condition);
      FastMap owner = CustomerUtil.getCustomerOwner(customerDto, saleCustomerDtos);
      return FastMap.create().add("customer", customerDto).add("owner", owner);
    }
  }
}
