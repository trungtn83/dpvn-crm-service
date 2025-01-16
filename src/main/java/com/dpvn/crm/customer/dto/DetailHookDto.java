package com.dpvn.crm.customer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetailHookDto {
  @JsonProperty("ProductId")
  private Long productId;

  @JsonProperty("ProductCode")
  private String productCode;

  @JsonProperty("ProductName")
  private String productName;

  @JsonProperty("Quantity")
  private Integer quantity;

  @JsonProperty("Price")
  private Long price;

  @JsonProperty("Discount")
  private Long discount;

  @JsonProperty("DiscountRatio")
  private Long discountRatio;

  @JsonProperty("Note")
  private String note;

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public String getProductCode() {
    return productCode;
  }

  public void setProductCode(String productCode) {
    this.productCode = productCode;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public Long getPrice() {
    return price;
  }

  public void setPrice(Long price) {
    this.price = price;
  }

  public Long getDiscount() {
    return discount;
  }

  public void setDiscount(Long discount) {
    this.discount = discount;
  }

  public Long getDiscountRatio() {
    return discountRatio;
  }

  public void setDiscountRatio(Long discountRatio) {
    this.discountRatio = discountRatio;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
