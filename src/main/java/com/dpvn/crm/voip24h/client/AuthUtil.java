package com.dpvn.crm.voip24h.client;

import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.util.ApiUtil;
import com.dpvn.shared.util.FastMap;
import org.springframework.http.HttpHeaders;

public class AuthUtil {
  public static UserInfo login() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.USER_AGENT, ApiUtil.HEADER_USER_AGENT);
    FastMap body =
        FastMap.create().add("apiKey", UserInfo.API_KEY).add("apiSecret", UserInfo.API_SECRET);
    FastMap responseBody = ApiUtil.postObject(UserInfo.API_URL_AUTH, headers, body).getBody();
    if (!"Success".equals(responseBody.getString("message"))) {
      throw new BadRequestException("Failed to login to VoIP24h. Please check your credentials.");
    }
    String token = responseBody.getMap("data").getString("token");
    UserInfo userInfo = new UserInfo();
    userInfo.setUrl(UserInfo.API_URL);
    userInfo.setAuthorization(token);
    return userInfo;
  }
}
