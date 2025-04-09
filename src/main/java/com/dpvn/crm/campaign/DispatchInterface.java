package com.dpvn.crm.campaign;

import com.dpvn.crmcrudservice.domain.dto.UserDto;
import java.util.List;

public interface DispatchInterface {
  UserDto dispatch(Long customerId, List<Long> saleIds);
}
