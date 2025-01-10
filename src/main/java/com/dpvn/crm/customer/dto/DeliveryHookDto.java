package com.dpvn.crm.customer.dto;

import com.dpvn.shared.util.DateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryHookDto {
  @JsonProperty("DeliveryBy")
  private Long deliveryBy;

  @JsonProperty("DeliveryCode")
  private String deliveryCode;

  @JsonProperty("ServiceType")
  private String serviceType;

  @JsonProperty("ServiceTypeText")
  private String serviceTypeText;

  @JsonProperty("Price")
  private Long price;

  @JsonProperty("Status")
  private Integer status;

  @JsonProperty("Receiver")
  private String receiver;

  @JsonProperty("ContactNumber")
  private String contactNumber;

  @JsonProperty("Address")
  private String address;

  @JsonProperty("LocationId")
  private Long locationId;

  @JsonProperty("LocationName")
  private String locationName;

  @JsonProperty("Weight")
  private Integer weight;

  @JsonProperty("Length")
  private Integer length;

  @JsonProperty("Width")
  private Integer width;

  @JsonProperty("Height")
  private Integer height;

  @JsonProperty("PartnerDeliveryId")
  private Long partnerDeliveryId;

  @JsonProperty("PartnerDelivery")
  private PartnerDeliveryHookDto partnerDelivery;

  @JsonProperty("ExpectedDelivery")
  @JsonDeserialize(using = DateTimeDeserializer.class)
  private Instant expectedDelivery;

  public Long getDeliveryBy() {
    return deliveryBy;
  }

  public void setDeliveryBy(Long deliveryBy) {
    this.deliveryBy = deliveryBy;
  }

  public String getDeliveryCode() {
    return deliveryCode;
  }

  public void setDeliveryCode(String deliveryCode) {
    this.deliveryCode = deliveryCode;
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

  public String getReceiver() {
    return receiver;
  }

  public void setReceiver(String receiver) {
    this.receiver = receiver;
  }

  public String getContactNumber() {
    return contactNumber;
  }

  public void setContactNumber(String contactNumber) {
    this.contactNumber = contactNumber;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Long getLocationId() {
    return locationId;
  }

  public void setLocationId(Long locationId) {
    this.locationId = locationId;
  }

  public String getLocationName() {
    return locationName;
  }

  public void setLocationName(String locationName) {
    this.locationName = locationName;
  }

  public Integer getWeight() {
    return weight;
  }

  public void setWeight(Integer weight) {
    this.weight = weight;
  }

  public Integer getLength() {
    return length;
  }

  public void setLength(Integer length) {
    this.length = length;
  }

  public Integer getWidth() {
    return width;
  }

  public void setWidth(Integer width) {
    this.width = width;
  }

  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }

  public Long getPartnerDeliveryId() {
    return partnerDeliveryId;
  }

  public void setPartnerDeliveryId(Long partnerDeliveryId) {
    this.partnerDeliveryId = partnerDeliveryId;
  }

  public PartnerDeliveryHookDto getPartnerDelivery() {
    return partnerDelivery;
  }

  public void setPartnerDelivery(PartnerDeliveryHookDto partnerDelivery) {
    this.partnerDelivery = partnerDelivery;
  }

  public Instant getExpectedDelivery() {
    return expectedDelivery;
  }

  public void setExpectedDelivery(Instant expectedDelivery) {
    this.expectedDelivery = expectedDelivery;
  }
}
