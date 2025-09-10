package com.dpvn.crm.voip24h.client;

import com.dpvn.shared.config.CacheService;
import com.dpvn.shared.util.StringUtil;
import org.springframework.stereotype.Component;

@Component
public class RedisSessionUtil {
  private static final String CACHE_KEY = "VOIP24H:SESSION_LOGGED_IN_USER:%s";
  private final CacheService cacheService;

  public RedisSessionUtil(CacheService cacheService) {
    this.cacheService = cacheService;
  }

  public static String getCurrentUser() {
    return UserInfo.API_USERNAME;
  }

  public UserInfo getLoggedInUser() {
    String currentUser = getCurrentUser();
    String key = String.format(CACHE_KEY, currentUser);

    UserInfo session = cacheService.getValue(key, UserInfo.class);
    if (session != null && StringUtil.isNotEmpty(session.getAuthorization())) {
      return session;
    }

    session = AuthUtil.login();
    cacheService.setValue(key, session);
    return session;
  }

  /**
   * Refresh cookie seller (ép login mới, xóa session cũ trước)
   */
  public void refreshSession() {
    String currentUser = getCurrentUser();
    String key = String.format(CACHE_KEY, currentUser);

    cacheService.removeKey(key);
    UserInfo session = AuthUtil.login();
    cacheService.setValue(key, session);
  }
}
