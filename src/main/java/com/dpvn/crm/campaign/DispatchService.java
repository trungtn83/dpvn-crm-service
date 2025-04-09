package com.dpvn.crm.campaign;

import com.dpvn.crm.helper.ConfigurationService;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DispatchService {
  private static final Logger LOG = LoggerFactory.getLogger(DispatchService.class);

  private final Map<String, DispatchInterface> dispatchs;
  private final ConfigurationService configurationService;

  public DispatchService(
      Map<String, DispatchInterface> dispatchs, ConfigurationService configurationService) {
    this.dispatchs = dispatchs;
    this.configurationService = configurationService;
  }

  public UserDto getNextSale(Long customerId, List<Long> saleIds) {
    LOG.info("Get Next Sale Id: {}", saleIds);
    String method = configurationService.getConfigDispatchMethod();
    LOG.info("Method: {}", method);
    DispatchInterface dispatch = dispatchs.get(method);
    UserDto next = dispatch.dispatch(customerId, saleIds);
    LOG.info("Next Sale Id: {}", next);
    return next;
  }
}
