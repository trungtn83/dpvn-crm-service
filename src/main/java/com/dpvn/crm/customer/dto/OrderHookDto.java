package com.dpvn.crm.customer.dto;

import com.dpvn.shared.util.DateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;

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

  @JsonProperty("SoldById")
  private Long soldById;

  @JsonProperty("CustomerId")
  private Long customerId;

  @JsonProperty("CustomerCode")
  private String customerCode;

  @JsonProperty("Status")
  private Integer status;

  @JsonProperty("StatusValue")
  private String statusValue;

  @JsonProperty("OrderDelivery")
  private DeliveryHookDto orderDelivery;

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

  public DeliveryHookDto getOrderDelivery() {
    return orderDelivery;
  }

  public void setOrderDelivery(DeliveryHookDto orderDelivery) {
    this.orderDelivery = orderDelivery;
  }
}
