package com.dpvn.crm.client;

import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.InteractionDto;
import com.dpvn.crmcrudservice.domain.dto.LeaveRequestDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerCategoryDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerStateDto;
import com.dpvn.crmcrudservice.domain.dto.TaskDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.shared.util.FastMap;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "crm-crud-service", contextId = "crm-crud-service-client")
public interface CrmCrudClient {
  // USER
  // ===========================================================================================================
  @GetMapping("/user/{id}")
  UserDto getUserById(@PathVariable("id") Long id);

  @GetMapping("/user")
  List<UserDto> getUsers();

  @PostMapping("/user/find-by-ids")
  List<UserDto> findUsersByIds(@RequestBody List<Long> userIds);

  @GetMapping("/user/username/{username}")
  UserDto getCrmUserByUserName(@PathVariable String username);

  // HRM
  // ===========================================================================================================
  @PostMapping("/hrm/leave-request")
  void createLeaveRequest(@RequestBody LeaveRequestDto leaveRequestDto);

  @GetMapping("/hrm/leave-request/find-by-user/{userId}")
  List<LeaveRequestDto> findLeaveRequestByUserId(@PathVariable(name = "userId") Long userId);

  @GetMapping("/hrm/leave-request/find-by-users")
  List<LeaveRequestDto> findLeaveRequestByUsersAndInMonthOfDate(
      @RequestParam(name = "userIds") List<Long> userIds,
      @RequestParam(name = "fromDate") String fromDate,
      @RequestParam(name = "toDate") String toDate);

  // CUSTOMER
  // ============================================================================================================
  @GetMapping("/customer/{id}")
  CustomerDto findCustomerById(@PathVariable("id") Long id);

  @PostMapping("/customer")
  CustomerDto createCustomer(@RequestBody CustomerDto dto);

  @PostMapping("/customer/upsert")
  CustomerDto upsertCustomer(@RequestBody CustomerDto dto);

  @PostMapping("/customer/search")
  List<CustomerDto> searchCustomers(@RequestBody FastMap conditions);

  @GetMapping("/customer/find-by-mobile-phone")
  List<CustomerDto> findCustomersByMobilePhone(@RequestParam String mobilePhone);

  /**
   * saleId
   * customerCategoryId
   * filterText
   * reasonIds
   * sourceIds
   * page
   * pageSize
   */
  @PostMapping("/customer/my")
  FastMap findMyCustomers(@RequestBody FastMap body);

  /**
   * saleId
   * filterText
   * tags
   * page
   * pageSize
   */
  @PostMapping("/customer/in-pool")
  FastMap findInPoolCustomers(@RequestBody FastMap body);

  /**
   * saleId
   * filterText
   * tags
   * page
   * pageSize
   */
  @PostMapping("/customer/task-based")
  FastMap findTaskBasedCustomers(@RequestBody FastMap body);

  @PostMapping("/customer/assign")
  void assignCustomer(@RequestBody SaleCustomerDto body);

  @PostMapping("/customer/revoke")
  void revokeCustomer(@RequestBody SaleCustomerDto body);

  @PostMapping("/customer/assigns")
  List<Long> assignCustomers(@RequestBody FastMap body);

  @PostMapping("/customer/revokes")
  List<Long> revokeCustomers(@RequestBody FastMap body);

  @GetMapping("/customer/state/by-sale")
  SaleCustomerStateDto getSaleCustomerStateBySale(
      @RequestParam Long saleId, @RequestParam Long customerId);

  @PostMapping("/customer/{id}/update-last-transaction")
  void updateLastTransaction(@PathVariable Long id, @RequestBody FastMap body);

  // SALE-CUSTOMER
  // ============================================================================================================
  @PostMapping("/sale-customer/upsert")
  SaleCustomerDto upsertSaleCustomer(@RequestBody SaleCustomerDto saleCustomerDto);

  @PostMapping("/sale-customer/{id}")
  SaleCustomerDto updateSaleCustomer(
      @PathVariable Long id, @RequestBody SaleCustomerDto saleCustomerDto);

  @PostMapping("/sale-customer/remove-by-options")
  void removeSaleCustomerByOptions(@RequestBody SaleCustomerDto dto);

  @PostMapping("/sale-customer/find-by-options")
  List<SaleCustomerDto> findSaleCustomersByOptions(@RequestBody FastMap body);

  @PostMapping("/sale-customer/state/find-latest")
  List<SaleCustomerStateDto> findLatestBySaleIdAndCustomerIds(@RequestBody FastMap body);

  @PostMapping("/sale-customer/state")
  void upsertSaleCustomerState(@RequestBody SaleCustomerStateDto body);

  // SALE-CUSTOMER-CATEGORY
  // ============================================================================================================
  @GetMapping("/sale-customer-category/find-by-options")
  List<SaleCustomerCategoryDto> findSaleCustomerCategoriesByOptions(
      @RequestParam Long saleId, @RequestParam(required = false) String code);

  @PostMapping("/sale-customer-category/upsert")
  void upsertSaleCustomerCategory(SaleCustomerCategoryDto categoryDto);

  @DeleteMapping("/sale-customer-category/{id}")
  void deleteSaleCustomerCategory(@PathVariable Long id);

  // TASK
  // ============================================================================================================
  @GetMapping("/task/find-by-options")
  List<TaskDto> getAllTasks(
      @RequestParam Long userId,
      @RequestParam(required = false) Long customerId,
      @RequestParam(required = false) Long campaignId,
      @RequestParam(required = false) Long kpiId,
      @RequestParam(required = false) Long otherId);

  @PostMapping("/task/upsert")
  void upsertTask(@RequestBody TaskDto body);

  @DeleteMapping("/task/{id}")
  void deleteTask(@PathVariable Long id);

  // INTERACTION
  // ===========================================================================================================
  @GetMapping("/interaction/find-by-options")
  List<InteractionDto> getAllInteractions(
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Long customerId,
      @RequestParam(required = false) Long campaignId,
      @RequestParam(required = false) Integer visibility);

  @PostMapping("/interaction/upsert")
  void upsertInteraction(@RequestBody InteractionDto body);

  @PostMapping("/interaction/find-last-interactions-date")
  List<InteractionDto> getLastInteractionDates(@RequestBody FastMap body);
}
