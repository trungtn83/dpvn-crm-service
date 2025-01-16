package com.dpvn.crm.customer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryPackageHookDto {
  @JsonProperty("Id")
  private Long id;

  @JsonProperty("OrderId")
  private Long orderId;

  @JsonProperty("Weight")
  private Integer weight;

  @JsonProperty("Length")
  private Integer length;

  @JsonProperty("Width")
  private Integer width;

  @JsonProperty("Height")
  private Integer height;

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

  @JsonProperty("WardId")
  private Long wardId;

  @JsonProperty("WardName")
  private String wardName;

  @JsonProperty("Comments")
  private String comments;

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

  public Long getWardId() {
    return wardId;
  }

  public void setWardId(Long wardId) {
    this.wardId = wardId;
  }

  public String getWardName() {
    return wardName;
  }

  public void setWardName(String wardName) {
    this.wardName = wardName;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }
}
