package com.dpvn.crm.voip24h.domain;

import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ObjectUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;

public class ViResponse {
  private String message;
  private Object data; // convert to FastMap or List<FastMap>

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<FastMap> getData() {
    if (data instanceof List) {
      return ObjectUtil.readValue(data, new TypeReference<>() {});
    }
    return List.of(ObjectUtil.readValue(data, FastMap.class));
  }

  public void setData(Object data) {
    this.data = data;
  }
}
