package com.dpvn.crm.voip24h.client;

import com.dpvn.shared.util.ApiUtil;
import feign.RequestInterceptor;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

public class Voip24hClientConfig {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Voip24hClientConfig.class);

  @Bean
  public RequestInterceptor requestInterceptor(RedisSessionUtil redisSessionUtil) {
    return requestTemplate -> {
      UserInfo userInfo = redisSessionUtil.getLoggedInUser();
      requestTemplate.header(HttpHeaders.USER_AGENT, ApiUtil.HEADER_USER_AGENT);
      requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + userInfo.getAuthorization());

      if (HttpMethod.POST.toString().equals(requestTemplate.method())) {
        requestTemplate.header(
            HttpHeaders.CONTENT_LENGTH, String.valueOf(requestTemplate.body().length));
      }
      requestTemplate.target(userInfo.getUrl());
    };
  }

  /**
   * ErrorDecoder: nếu cookie hết hạn (401/403) thì login lại, update Redis và retry.
   */
  @Bean
  public ErrorDecoder errorDecoder(RedisSessionUtil redisSessionUtil) {
    return (methodKey, response) -> {
      if (response.status() == 401 || response.status() == 403) {
        String url = response.request().url();
        LOGGER.warn("Got {} for {}, refreshing cookie", response.status(), url);

        try {
          redisSessionUtil.refreshSession();

          // Feign 13: dùng Long thay cho Date
          return new RetryableException(
              response.status(),
              "Unauthorized, refreshed cookie for url=" + url,
              response.request().httpMethod(),
              null, // cause
              0L, // retryAfter = 0ms → retry ngay
              response.request());
        } catch (Exception e) {
          LOGGER.error("Failed to refresh cookie for url={}", url, e);
          return new ErrorDecoder.Default().decode(methodKey, response);
        }
      }
      return new ErrorDecoder.Default().decode(methodKey, response);
    };
  }
}
