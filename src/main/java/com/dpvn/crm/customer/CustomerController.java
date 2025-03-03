package com.dpvn.crm.customer;

import com.dpvn.crm.user.UserService;
import com.dpvn.crm.user.UserUtil;
import com.dpvn.crmcrudservice.domain.constant.Customers;
import com.dpvn.crmcrudservice.domain.constant.SaleCustomers;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.CustomerTypeDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerCategoryDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerStateDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ListUtil;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer")
public class CustomerController {

  private final CustomerService customerService;
  private final SaleCustomerService saleCustomerService;
  private final SaleCustomerCategoryService saleCustomerCategoryService;
  private final UserService userService;
  private final CustomerCraftService customerCraftService;
  private final CustomerTypeService customerTypeService;

  public CustomerController(
      CustomerService customerService,
      SaleCustomerService saleCustomerService,
      SaleCustomerCategoryService saleCustomerCategoryService,
      UserService userService,
      CustomerCraftService customerCraftService,
      CustomerTypeService customerTypeService) {
    this.customerService = customerService;
    this.saleCustomerService = saleCustomerService;
    this.saleCustomerCategoryService = saleCustomerCategoryService;
    this.userService = userService;
    this.customerCraftService = customerCraftService;
    this.customerTypeService = customerTypeService;
  }

  @GetMapping("/{id}/of-sale")
  public FastMap getCustomerOfSale(
      @RequestHeader("x-user-id") Long loginUserId, @PathVariable Long id) {
    return customerService.findCustomerOfSale(loginUserId, id);
  }

  @GetMapping("/{id}")
  public CustomerDto getCustomer(@PathVariable Long id) {
    return customerService.findCustomerById(id);
  }

  /**
   * @param loginUserId
   * @return List:FastMap(customer: CustomerDto, state: SaleCustomerStateDto <- the last state if
   *     have)
   */
  @PostMapping("/my")
  public FastMap getMyCustomers(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody FastMap body) {
    boolean isOld = body.getBoolean("isOld");
    Long saleId = body.getLong("saleId");
    Long customerTypeId = body.getLong("customerTypeId");
    Long customerCategoryId = body.getLong("customerCategoryId");
    String filterText = body.getString("filterText");
    Integer page = body.getInt(0, "page");
    Integer pageSize = body.getInt(10, "pageSize");
    List<Integer> sourceIds = body.getList("sourceIds");
    List<Integer> reasonIds =
        isOld
            ? List.of(SaleCustomers.Reason.INVOICE)
            : (ListUtil.isEmpty(sourceIds)
                ? Stream.concat(
                        Stream.of(
                            SaleCustomers.Reason.ORDER,
                            SaleCustomers.Reason.CAMPAIGN,
                            SaleCustomers.Reason.LEADER),
                        SaleCustomers.Reason.MY_HANDS.stream())
                    .toList()
                : sourceIds);
    UserDto userDto = userService.findById(loginUserId);
    return customerService.findMyCustomers(
        FastMap.create()
            .add("saleId", !UserUtil.isGod(userDto) ? loginUserId : saleId)
            .add("customerTypeId", customerTypeId)
            .add("customerCategoryId", customerCategoryId)
            .add("filterText", filterText)
            .add("reasonIds", reasonIds)
            .add("sourceIds", sourceIds)
            .add("page", page)
            .add("pageSize", pageSize));
  }

  @PostMapping("/in-pool")
  public FastMap findInPoolCustomers(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody FastMap body) {
    String filterText = body.getString("filterText");
    List<String> tags = body.getList("tags");
    List<String> locationCodes = body.getList("locationCodes");
    List<Long> typeIds = body.getList("typeIds");
    List<Integer> sourceIds = body.getList("sourceIds");

    Integer page = body.getInt(0, "page");
    Integer pageSize = body.getInt(10, "pageSize");
    return customerService.findInPoolCustomers(
        FastMap.create()
            .add("saleId", loginUserId)
            .add("filterText", filterText)
            .add("tags", tags)
            .add("locationCodes", locationCodes)
            .add("typeIds", typeIds)
            .add("sourceIds", sourceIds)
            .add("page", page)
            .add("pageSize", pageSize));
  }

  @PostMapping("/in-ocean")
  public FastMap findInOceanCustomers(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody FastMap body) {
    String filterText = body.getString("filterText");
    List<String> locationCodes = body.getList("locationCodes");
    List<Long> typeIds = body.getList("typeIds");
    List<Integer> sourceIds = body.getList("sourceIds");
    Integer page = body.getInt(0, "page");
    Integer pageSize = body.getInt(10, "pageSize");
    return customerService.findInOceanCustomers(
        FastMap.create()
            .add("saleId", loginUserId)
            .add("filterText", filterText)
            .add("locationCodes", locationCodes)
            .add("typeIds", typeIds)
            .add("sourceIds", sourceIds)
            .add("page", page)
            .add("pageSize", pageSize));
  }

  @PostMapping("/task-based")
  public FastMap findTaskBasedCustomers(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody FastMap body) {
    Long saleId = body.getLong("saleId");
    String filterText = body.getString("filterText");
    List<String> tags = body.getList("tags");
    Integer page = body.getInt(0, "page");
    Integer pageSize = body.getInt(10, "pageSize");
    UserDto userDto = userService.findById(loginUserId);
    return customerService.findTaskBasedCustomers(
        FastMap.create()
            .add("saleId", !UserUtil.isGod(userDto) ? loginUserId : saleId)
            .add("filterText", filterText)
            .add("tags", tags)
            .add("page", page)
            .add("pageSize", pageSize));
  }

  @PostMapping("/sale-customer/state")
  public void upsertSaleCustomerState(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody SaleCustomerStateDto body) {
    body.setCreatedBy(loginUserId);
    body.setSaleId(loginUserId);
    customerService.upsertSaleCustomerState(body);
  }

  @PostMapping("/assign-to-sale")
  public void assignCustomerToSale(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody SaleCustomerDto body) {
    body.setSaleId(loginUserId);
    customerService.assignCustomer(body);
  }

  @PostMapping("/assigns-to")
  public void assignCustomersTo(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody FastMap body) {
    body.add("saleId", loginUserId);
    customerService.assignCustomers(body);
  }

  @PostMapping("/revoke-from")
  public void revokeCustomerFrom(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody SaleCustomerDto body) {
    body.setSaleId(loginUserId);
    customerService.revokeCustomer(body);
  }

  @PostMapping("/revokes-from")
  public void revokeCustomersFrom(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody FastMap body) {
    body.add("saleId", loginUserId);
    customerService.revokeCustomers(body);
  }

  /**
   * @param id: customer id
   * @param body (lastTransaction:Instant, isSuccessful:boolean)
   */
  @PostMapping("/{id}/update-last-transaction")
  public void updateLastTransaction(@PathVariable Long id, @RequestBody FastMap body) {
    customerService.updateLastTransaction(id, body);
  }

  @GetMapping("/validate-mobile-phone")
  public FastMap validateCustomerByMobilePhone(
      @RequestHeader("x-user-id") Long loginUserId, @RequestParam String mobilePhone) {
    return customerService.validateMobilePhoneNewCustomer(loginUserId, mobilePhone);
  }

  @PostMapping
  public void createNewCustomer(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody FastMap body) {
    customerService.createNewCustomer(loginUserId, body);
  }

  @PostMapping("/{id}")
  public void updateExistedCustomer(
      @RequestHeader("x-user-id") Long loginUserId,
      @PathVariable(name = "id") Long customerId,
      @RequestBody FastMap customerDto) {
    customerService.updateExistedCustomer(loginUserId, customerId, customerDto);
  }

  @DeleteMapping("/{id}")
  public void deleteCustomer(
      @RequestHeader("x-user-id") Long loginUserId,
      @PathVariable Long id,
      @RequestParam(required = false) String owner) {
    customerService.deleteCustomer(loginUserId, id, owner);
  }

  /**
   * @param body (X) userId: get from header - customerId: Long - action: "STAR", "REQUESTING" -
   *     flag: boolean -> turn on or off
   */
  @PostMapping("/do-action")
  public void doActionCustomer(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody FastMap body) {
    Long customerId = body.getLong("customerId");
    String action = body.getString("action");
    if (!List.of("STAR", "REQUESTING").contains(action)) {
      throw new BadRequestException("Action is not valid");
    }
    boolean flag = body.getBoolean("flag");
    saleCustomerService.doActionCustomer(loginUserId, customerId, getReasonByAction(action), flag);
  }

  private Integer getReasonByAction(String action) {
    if ("STAR".equals(action)) {
      return SaleCustomers.Reason.STAR;
    } else if ("REQUESTING".equals(action)) {
      return SaleCustomers.Reason.REQUESTING;
    }
    throw new BadRequestException("Action is not valid");
  }

  @GetMapping("/sale-customer-category")
  public List<SaleCustomerCategoryDto> findSaleCustomerCategoriesByOptions(
      @RequestHeader("x-user-id") Long loginUserId, @RequestParam(required = false) String code) {
    return saleCustomerCategoryService.findSaleCustomerCategoriesByOptions(loginUserId, code);
  }

  @PostMapping("/sale-customer-category")
  public void upsertSaleCustomerCategory(
      @RequestHeader("x-user-id") Long loginUserId,
      @RequestBody SaleCustomerCategoryDto saleCustomerCategoryDto) {
    saleCustomerCategoryDto.setSaleId(loginUserId);
    saleCustomerCategoryService.upsertSaleCustomerCategory(saleCustomerCategoryDto);
  }

  @DeleteMapping("/sale-customer-category/{id}")
  public void deleteSaleCustomerCategory(
      @RequestHeader("x-user-id") Long loginUserId, @PathVariable Long id) {
    saleCustomerCategoryService.deleteSaleCustomerCategory(loginUserId, id);
  }

  // TODO: need to keep to run only once times when init customer relationship
  // other case will be handler by webhook from kiotviet
  @Deprecated
  @PostMapping("/init-relationship")
  public void initRelationship(@RequestHeader("x-user-id") Long loginUserId) {
    if (!userService.isGod(loginUserId)) {
      throw new BadRequestException("Only GOD can init relationship");
    }
    customerService.initRelationship();
  }

  @PostMapping("/craft/{sourceId}")
  public void craftCustomers(@PathVariable Integer sourceId) {
    customerCraftService.craftCustomers(sourceId);
  }

  @GetMapping("/types")
  public PagingResponse<CustomerTypeDto> getAllCustomerTypes() {
    return customerTypeService.getAllCustomerTypes();
  }

  @PostMapping("/{id}/approve")
  public void approveFromSandToGold(
      @RequestHeader("x-user-id") Long loginUserId,
      @PathVariable Long id,
      @RequestBody FastMap body) {
    body.add("userId", loginUserId);
    customerService.approveFromSandToGold(id, body);
  }

  @PostMapping("/{id}/dig")
  public void digCustomer(
      @RequestHeader("x-user-id") Long loginUserId,
      @PathVariable Long id,
      @RequestParam(required = false, defaultValue = Customers.Owner.SANDBANK) String owner) {
    customerService.digCustomerFromOceanOrGoldmineToGold(loginUserId, id, owner);
  }
}
