package com.dpvn.crm.customer.dto;

import com.dpvn.shared.util.DateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderHookDto {
  @JsonProperty("Id")
  private Long id;

  @JsonProperty("Code")
  private String code;

  @JsonProperty("PurchaseDate")
  @JsonDeserialize(using = DateTimeDeserializer.class)
  private Instant purchaseDate;

  @JsonProperty("BranchId")
  private Long branchId;

  @JsonProperty("BranchName")
  private String branchName;

  @JsonProperty("SoldById")
  private Long soldById;

  @JsonProperty("CustomerId")
  private Long customerId;

  @JsonProperty("CustomerCode")
  private String customerCode;

  @JsonProperty("Total")
  private Long total;

  @JsonProperty("Discount")
  private Long discount;

  @JsonProperty("TotalPayment")
  private Long totalPayment;

  @JsonProperty("Description")
  private String description;

  @JsonProperty("Status")
  private Integer status;

  @JsonProperty("StatusValue")
  private String statusValue;

  @JsonProperty("ModifiedDate")
  @JsonDeserialize(using = DateTimeDeserializer.class)
  private Instant modifiedDate;

  @JsonProperty("CreatedDate")
  @JsonDeserialize(using = DateTimeDeserializer.class)
  private Instant createdDate;

  @JsonProperty("OrderDelivery")
  private DeliveryHookDto orderDelivery;

  @JsonProperty("DeliveryInfo")
  private DeliveryInfoHookDto deliveryInfo;

  @JsonProperty("DeliveryPackage")
  private DeliveryPackageHookDto deliveryPackage;

  @JsonProperty("OrderDetails")
  private List<DetailHookDto> orderDetails;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Instant getPurchaseDate() {
    return purchaseDate;
  }

  public void setPurchaseDate(Instant purchaseDate) {
    this.purchaseDate = purchaseDate;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public Long getSoldById() {
    return soldById;
  }

  public void setSoldById(Long soldById) {
    this.soldById = soldById;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public String getCustomerCode() {
    return customerCode;
  }

  public void setCustomerCode(String customerCode) {
    this.customerCode = customerCode;
  }

  public Long getTotal() {
    return total;
  }

  public void setTotal(Long total) {
    this.total = total;
  }

  public Long getTotalPayment() {
    return totalPayment;
  }

  public void setTotalPayment(Long totalPayment) {
    this.totalPayment = totalPayment;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getStatusValue() {
    return statusValue;
  }

  public void setStatusValue(String statusValue) {
    this.statusValue = statusValue;
  }

  public DeliveryHookDto getOrderDelivery() {
    return orderDelivery;
  }

  public void setOrderDelivery(DeliveryHookDto orderDelivery) {
    this.orderDelivery = orderDelivery;
  }

  public List<DetailHookDto> getOrderDetails() {
    return orderDetails;
  }

  public void setOrderDetails(List<DetailHookDto> orderDetails) {
    this.orderDetails = orderDetails;
  }

  public Long getDiscount() {
    return discount;
  }

  public void setDiscount(Long discount) {
    this.discount = discount;
  }

  public Instant getModifiedDate() {
    return modifiedDate;
  }

  public void setModifiedDate(Instant modifiedDate) {
    this.modifiedDate = modifiedDate;
  }

  public Instant getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Instant createdDate) {
    this.createdDate = createdDate;
  }

  public DeliveryInfoHookDto getDeliveryInfo() {
    return deliveryInfo;
  }

  public void setDeliveryInfo(DeliveryInfoHookDto deliveryInfo) {
    this.deliveryInfo = deliveryInfo;
  }

  public DeliveryPackageHookDto getDeliveryPackage() {
    return deliveryPackage;
  }

  public void setDeliveryPackage(DeliveryPackageHookDto deliveryPackage) {
    this.deliveryPackage = deliveryPackage;
  }
}
