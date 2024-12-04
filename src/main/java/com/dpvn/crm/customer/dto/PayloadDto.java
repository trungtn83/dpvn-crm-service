package com.dpvn.crm.customer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayloadDto<T> {
  @JsonProperty("Id")
  private String id;
  @JsonProperty("Attempt")
  private Integer attempt;
  @JsonProperty("Notifications")
  private List<Notification<T>> notifications;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getAttempt() {
    return attempt;
  }

  public void setAttempt(Integer attempt) {
    this.attempt = attempt;
  }

  public List<Notification<T>> getNotifications() {
    return notifications;
  }

  public void setNotifications(List<Notification<T>> notifications) {
    this.notifications = notifications;
  }

  public static class Notification<T> {
    @JsonProperty("Action")
    private String action;
    @JsonProperty("Data")
    private List<T> data;

    public String getAction() {
      return action;
    }

    public void setAction(String action) {
      this.action = action;
    }

    public List<T> getData() {
      return data;
    }

    public void setData(List<T> data) {
      this.data = data;
    }
  }

}
