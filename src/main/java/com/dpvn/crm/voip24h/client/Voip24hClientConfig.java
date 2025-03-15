package com.dpvn.crm.voip24h.client;

import com.dpvn.shared.util.ApiUtil;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

public class Voip24hClientConfig {
  @Bean
  public RequestInterceptor requestInterceptor(UserSessionUtil userSessionUtil) {
    return requestTemplate -> {
      UserInfo userInfo = userSessionUtil.getLoggedInUser();
      requestTemplate.header(HttpHeaders.USER_AGENT, ApiUtil.HEADER_USER_AGENT);
      requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + userInfo.getAuthorization());

      if (HttpMethod.POST.toString().equals(requestTemplate.method())) {
        requestTemplate.header(
            HttpHeaders.CONTENT_LENGTH, String.valueOf(requestTemplate.body().length));
      }
      requestTemplate.target(userInfo.getUrl());
    };
  }
}
