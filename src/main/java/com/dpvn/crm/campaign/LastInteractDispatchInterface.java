package com.dpvn.crm.campaign;

import com.dpvn.crmcrudservice.domain.dto.UserDto;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("LAST_INTERACT")
public class LastInteractDispatchInterface implements DispatchInterface {
  private static final Logger LOG = LoggerFactory.getLogger(LastInteractDispatchInterface.class);

  @Override
  public UserDto dispatch(Long customerId, List<Long> saleIds) {
    UserDto userDto = new UserDto();
    userDto.setId(0L);
    userDto.setIdf(0L);
    return userDto;
  }
}
