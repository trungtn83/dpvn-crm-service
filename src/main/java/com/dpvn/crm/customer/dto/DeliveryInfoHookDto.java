package com.dpvn.crm.customer.dto;

import com.dpvn.shared.util.DateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryInfoHookDto {
  @JsonProperty("Id")
  private Long id;
  @JsonProperty("OrderId")
  private Long orderId;
  @JsonProperty("DeliveryPackageId")
  private Long deliveryPackageId;
  @JsonProperty("DeliveryBy")
  private Long deliveryBy;
  @JsonProperty("ServiceType")
  private String serviceType;
  @JsonProperty("ServiceTypeText")
  private String serviceTypeText;
  @JsonProperty("ServiceAdd")
  private String serviceAdd;
  @JsonProperty("Price")
  private Long price;
  @JsonProperty("Status")
  private Integer status;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public Long getDeliveryPackageId() {
    return deliveryPackageId;
  }

  public void setDeliveryPackageId(Long deliveryPackageId) {
    this.deliveryPackageId = deliveryPackageId;
  }

  public Long getDeliveryBy() {
    return deliveryBy;
  }

  public void setDeliveryBy(Long deliveryBy) {
    this.deliveryBy = deliveryBy;
  }

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  public String getServiceTypeText() {
    return serviceTypeText;
  }

  public void setServiceTypeText(String serviceTypeText) {
    this.serviceTypeText = serviceTypeText;
  }

  public String getServiceAdd() {
    return serviceAdd;
  }

  public void setServiceAdd(String serviceAdd) {
    this.serviceAdd = serviceAdd;
  }

  public Long getPrice() {
    return price;
  }

  public void setPrice(Long price) {
    this.price = price;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }
}
