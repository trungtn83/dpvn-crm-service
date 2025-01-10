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
public class InvoiceHookDto {
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

  @JsonProperty("Status")
  private Integer status;

  @JsonProperty("StatusValue")
  private String statusValue;

  @JsonProperty("Description")
  private String description;

  @JsonProperty("InvoiceDelivery")
  private DeliveryHookDto invoiceDelivery;

  @JsonProperty("InvoiceDetails")
  private List<DetailHookDto> invoiceDetails;

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

  public DeliveryHookDto getInvoiceDelivery() {
    return invoiceDelivery;
  }

  public void setInvoiceDelivery(DeliveryHookDto invoiceDelivery) {
    this.invoiceDelivery = invoiceDelivery;
  }

  public List<DetailHookDto> getInvoiceDetails() {
    return invoiceDetails;
  }

  public void setInvoiceDetails(List<DetailHookDto> invoiceDetails) {
    this.invoiceDetails = invoiceDetails;
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

  public Long getTotal() {
    return total;
  }

  public void setTotal(Long total) {
    this.total = total;
  }

  public Long getDiscount() {
    return discount;
  }

  public void setDiscount(Long discount) {
    this.discount = discount;
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
}
