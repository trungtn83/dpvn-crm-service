package com.dpvn.crm.customer;

import com.dpvn.crmcrudservice.domain.constant.SaleCustomers;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.InteractionDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerCategoryDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerStateDto;
import com.dpvn.crmcrudservice.domain.dto.TaskDto;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ListUtil;
import java.util.List;
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

  public CustomerController(
      CustomerService customerService,
      SaleCustomerService saleCustomerService,
      SaleCustomerCategoryService saleCustomerCategoryService) {
    this.customerService = customerService;
    this.saleCustomerService = saleCustomerService;
    this.saleCustomerCategoryService = saleCustomerCategoryService;
  }

  @GetMapping("/{id}")
  public FastMap getCustomer(@RequestHeader("x-user-id") Long loginUserId, @PathVariable Long id) {
    return customerService.findCustomer(loginUserId, id);
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
    Long customerCategoryId = body.getLong("customerCategoryId");
    String filterText = body.getString("filterText");
    Integer page = body.getInt(0, "page");
    Integer pageSize = body.getInt(10, "pageSize");
    List<Integer> sourceIds = body.getList("sourceIds");
    List<Integer> reasonIds =
        isOld
            ? List.of(SaleCustomers.Reason.INVOICE)
            : (ListUtil.isEmpty(sourceIds)
                ? List.of(
                    SaleCustomers.Reason.ORDER,
                    SaleCustomers.Reason.CAMPAIGN,
                    SaleCustomers.Reason.LEADER)
                : sourceIds);
    return customerService.findMyCustomers(
        FastMap.create()
            .add("saleId", loginUserId)
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
    Integer page = body.getInt(0, "page");
    Integer pageSize = body.getInt(10, "pageSize");
    return customerService.findInPoolCustomers(
        FastMap.create()
            .add("saleId", loginUserId)
            .add("filterText", filterText)
            .add("tags", tags)
            .add("page", page)
            .add("pageSize", pageSize));
  }

  @PostMapping("/task-based")
  public FastMap findTaskBasedCustomers(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody FastMap body) {
    String filterText = body.getString("filterText");
    List<String> tags = body.getList("tags");
    Integer page = body.getInt(0, "page");
    Integer pageSize = body.getInt(10, "pageSize");
    return customerService.findTaskBasedCustomers(
        FastMap.create()
            .add("saleId", loginUserId)
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

  @PostMapping("/assign-to")
  public void assignCustomerTo(
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

  @GetMapping("/interaction")
  public List<InteractionDto> getInteractionCustomers(
      @RequestHeader("x-user-id") Long loginUserId, @RequestParam Long customerId) {
    return customerService.getAllInteractions(loginUserId, customerId);
  }

  @PostMapping("/interaction")
  public void upsertInteraction(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody InteractionDto body) {
    body.setCreatedBy(loginUserId);
    body.setInteractBy(loginUserId);
    customerService.upsertInteraction(body);
  }

  @GetMapping("/task")
  public List<TaskDto> getAllTasks(
      @RequestHeader("x-user-id") Long loginUserId, @RequestParam Long customerId) {
    return customerService.getAllTasks(loginUserId, customerId);
  }

  @PostMapping("/task")
  public void upsertTask(@RequestHeader("x-user-id") Long loginUserId, @RequestBody TaskDto body) {
    body.setCreatedBy(loginUserId);
    body.setUserId(loginUserId);
    customerService.upsertTask(body);
  }

  /**
   * @param id: customer id
   * @param body (lastTransaction:Instant, isSuccessful:boolean)
   */
  @PostMapping("/{id}/update-last-transaction")
  public void updateLastTransaction(@PathVariable Long id, @RequestBody FastMap body) {
    customerService.updateLastTransaction(id, body);
  }

  @PostMapping("/upsert")
  public void upsertCustomer(@RequestBody CustomerDto customerDto) {
    customerService.upsertCustomer(customerDto);
  }

  @PostMapping("/sale-customer/upsert")
  public void upsertSaleCustomer(@RequestBody SaleCustomerDto saleCustomerDto) {
    saleCustomerService.upsertSaleCustomer(saleCustomerDto);
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
    customerService.doActionCustomer(loginUserId, customerId, getReasonByAction(action), flag);
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
}
