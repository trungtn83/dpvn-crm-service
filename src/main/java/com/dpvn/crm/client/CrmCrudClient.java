package com.dpvn.crm.client;

import com.dpvn.crmcrudservice.domain.dto.CampaignDto;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.CustomerTypeDto;
import com.dpvn.crmcrudservice.domain.dto.InteractionDto;
import com.dpvn.crmcrudservice.domain.dto.LeaveRequestDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerCategoryDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerStateDto;
import com.dpvn.crmcrudservice.domain.dto.TaskDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.shared.domain.dto.AddressDto;
import com.dpvn.shared.util.FastMap;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
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

  @Cacheable(value = "users", key = "#root.methodName")
  @GetMapping("/user")
  List<UserDto> getUsers();

  @PostMapping("/user/find-by-ids")
  List<UserDto> findUsersByIds(@RequestBody List<Long> userIds);

  @GetMapping("/user/username/{username}")
  UserDto getCrmUserByUserName(@PathVariable String username);

  @PostMapping("/user/find-by-options")
  List<UserDto> findUsersByOptions(@RequestBody UserDto userDto);

  /**
   * - filterText - department: get from Constant please - role: get from Constant please - page :
   * null if get all - pageSize : null if get all
   */
  @PostMapping("/user/search")
  FastMap searchUsers(@RequestBody FastMap condition);

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

  @GetMapping("/customer/find-by-idf/{idf}")
  CustomerDto findCustomerByIdf(@PathVariable("idf") Long idf);

  @PostMapping("/customer")
  CustomerDto createNewCustomer(@RequestBody CustomerDto dto);

  @PostMapping(value = "/customer/{id}")
  CustomerDto updateExistedCustomer(@PathVariable("id") Long id, @RequestBody FastMap dto);

  @GetMapping("/customer/find-by-mobile-phone")
  List<CustomerDto> findCustomersByMobilePhone(@RequestParam String mobilePhone);

  @GetMapping("/customer/find-by-status-for-init")
  List<CustomerDto> findByStatusForInitRelationship(
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "pageSize", required = false, defaultValue = "100") Integer pageSize);

  /** saleId customerCategoryId filterText reasonIds sourceIds page pageSize */
  @PostMapping("/customer/my")
  FastMap findMyCustomers(@RequestBody FastMap body);

  /** saleId filterText tags page pageSize */
  @PostMapping("/customer/in-pool")
  FastMap findInPoolCustomers(@RequestBody FastMap body);

  /** filterText categoryIds locationIds page pageSize */
  @PostMapping("/customer/in-ocean")
  FastMap findInOceanCustomers(@RequestBody FastMap body);

  /** saleId filterText tags page pageSize */
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

  @PostMapping("/customer/sync-all")
  void syncAllCustomers(List<CustomerDto> customerDtos);

  @GetMapping("/customer/last-created-by-source")
  CustomerDto findLastCreatedCustomerBySource(@RequestParam(required = false) Long sourceId);

  @PostMapping("/customer/{id}/approve")
  void approveCustomerFromSandToGold(@PathVariable Long id, @RequestBody FastMap body);

  // CUSTOMER-TYPE
  // ============================================================================================================
  @GetMapping("/customer-type")
  List<CustomerTypeDto> getAllCustomerTypes();

  // SALE-CUSTOMER
  // ============================================================================================================
  @PostMapping("/sale-customer")
  SaleCustomerDto createNewSaleCustomer(@RequestBody SaleCustomerDto dto);

  @PostMapping("/sale-customer/{id}")
  SaleCustomerDto updateExistedSaleCustomer(@PathVariable Long id, @RequestBody FastMap data);

  @PostMapping("/sale-customer/remove-by-options")
  void removeSaleCustomerByOptions(@RequestBody SaleCustomerDto dto);

  @DeleteMapping("/sale-customer/{id}")
  void deleteSaleCustomer(@PathVariable Long id);

  @PostMapping("/sale-customer/find-by-options")
  List<SaleCustomerDto> findSaleCustomersByOptions(@RequestBody FastMap body);

  @PostMapping("/sale-customer/state/find-latest")
  List<SaleCustomerStateDto> findLatestBySaleIdAndCustomerIds(@RequestBody FastMap body);

  @PostMapping("/sale-customer/state")
  void upsertSaleCustomerState(@RequestBody SaleCustomerStateDto body);

  @GetMapping("/customer/find-last-created")
  CustomerDto findLastCreatedCustomer(
      @RequestParam(value = "sourceId", required = false) Integer sourceId);

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

  @PostMapping("/task")
  void createNewTask(@RequestBody TaskDto body);

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

  @PostMapping("/interaction")
  void createInteraction(@RequestBody InteractionDto body);

  @PostMapping("/interaction/find-last-interactions-date")
  List<InteractionDto> getLastInteractionDates(@RequestBody FastMap body);

  // ADDRESS
  // ===========================================================================================================
  @GetMapping("/address")
  List<AddressDto> findAllAddresses();

  // CAMPAIGN
  // ===========================================================================================================
  @GetMapping("/campaign")
  List<CampaignDto> findAllCampaigns();

  @PostMapping("/campaign/{id}/assign-customers-to-sales")
  void assignToSaleInCampaign(@PathVariable Long id, @RequestBody FastMap body);
}
