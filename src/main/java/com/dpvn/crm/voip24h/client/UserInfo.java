package com.dpvn.crm.voip24h.client;

public class UserInfo {
  public static final String API_URL_AUTH = "https://api.voip24h.vn/v3/authentication";
  public static final String API_USERNAME = "info@duocphamvietnhat.com";
  public static final String API_KEY = "857ec340e9bb45e1b45f3afd94ecbd7ce201b25b";
  public static final String API_SECRET = "1eed45cb621810dfb12395ef1b645e5e8da38ee0";

  public static final String API_URL = "https://api.voip24h.vn/v3";

  private String url;
  private String authorization;
  private int timeout;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getAuthorization() {
    return authorization;
  }

  public void setAuthorization(String authorization) {
    this.authorization = authorization;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }
}
