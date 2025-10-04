package com.dpvn.crm.client;

import com.dpvn.shared.exception.ApiError;
import com.dpvn.shared.util.ObjectUtil;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

public class MisaAmisFeignClientConfig {
  private static final org.slf4j.Logger LOGGER =
      LoggerFactory.getLogger(MisaAmisFeignClientConfig.class);

  @Bean
  public RequestInterceptor requestInterceptor() {
    return requestTemplate -> {
      requestTemplate.header("X-SELLER-ID", "2010");

      // Log request headers
      LOGGER.info("Request URL: {}", requestTemplate.url());
      LOGGER.info("Request Method: {}", requestTemplate.method());
      LOGGER.info("Request Headers: {}", requestTemplate.headers());

      // Log request body
      if (requestTemplate.body() != null && requestTemplate.body().length > 0) {
        LOGGER.info("Request Body: {}", new String(requestTemplate.body()));
      } else {
        LOGGER.info("Request Body: N/A");
      }
    };
  }

  @Bean
  public ErrorDecoder errorDecoder() {
    return (methodKey, response) -> {
      try {
        if (response.body() != null) {
          String body = IOUtils.toString(response.body().asInputStream(), StandardCharsets.UTF_8);
          ApiError error = ObjectUtil.readValue(body, ApiError.class);
          return new RuntimeException(
              String.format(
                  "Gọi đến %s bị lỗi: %s (%s)",
                  methodKey, error.getError(), ObjectUtil.writeValueAsString(error)));
        }
      } catch (Exception e) {
        return new RuntimeException("Feign error decoding failed", e);
      }
      return new RuntimeException("Unknown Feign error, status=" + response.status());
    };
  }
}
