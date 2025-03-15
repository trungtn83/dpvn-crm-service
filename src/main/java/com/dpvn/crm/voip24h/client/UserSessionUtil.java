package com.dpvn.crm.voip24h.client;

import com.dpvn.shared.config.CacheService;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.util.ApiUtil;
import com.dpvn.shared.util.FastMap;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class UserSessionUtil {
  private static final String CACHE_KEY = "VOIP24H_LOGGED_IN_USER";
  private final CacheService cacheService;

  public UserSessionUtil(CacheService cacheService) {
    this.cacheService = cacheService;
  }

  public UserInfo getLoggedInUser() {
    if (!cacheService.hasKey(CACHE_KEY)) {
      UserInfo userInfo = login();
      cacheService.setValue(CACHE_KEY, userInfo, userInfo.getTimeout());
    }
    return cacheService.getValue(CACHE_KEY, UserInfo.class);
  }

  private UserInfo login() {
    String url = "https://api.voip24h.vn/v3/authentication";
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.USER_AGENT, ApiUtil.HEADER_USER_AGENT);
    FastMap body =
        FastMap.create()
            .add("apiKey", "857ec340e9bb45e1b45f3afd94ecbd7ce201b25b")
            .add("apiSecret", "1eed45cb621810dfb12395ef1b645e5e8da38ee0");
    FastMap responseBody = ApiUtil.postObject(url, headers, body).getBody();
    if (!"Success".equals(responseBody.getString("message"))) {
      throw new BadRequestException("Failed to login to VoIP24h. Please check your credentials.");
    }
    String token = responseBody.getMap("data").getString("token");
    UserInfo userInfo = new UserInfo();
    userInfo.setUrl("https://api.voip24h.vn/v3");
    userInfo.setAuthorization(token);
    userInfo.setTimeout(43200); // expired and must re-login after a half day
    return userInfo;
  }
}
