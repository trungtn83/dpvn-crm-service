package com.dpvn.crm.campaign;

import com.dpvn.crm.helper.ConfigurationService;
import com.dpvn.crm.user.UserService;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.shared.util.ListUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("ROUND_ROBIN")
public class RoundRobinDispatchInterface implements DispatchInterface {
  private static final Logger LOG = LoggerFactory.getLogger(RoundRobinDispatchInterface.class);
  private final UserService userService;
  private final ConfigurationService configurationService;

  public RoundRobinDispatchInterface(
      UserService userService, ConfigurationService configurationService) {
    this.userService = userService;
    this.configurationService = configurationService;
  }

  @Override
  public UserDto dispatch(Long customerId, List<Long> saleIds) {
    LOG.info("Round Robin dispatch for customer: {} sales: {}", customerId, saleIds);
    List<UserDto> ownerIds = new ArrayList<>();
    if (ListUtil.isEmpty(saleIds)) {
      ownerIds.addAll(userService.getSaleUsers());
    } else {
      ownerIds.addAll(userService.findUsersByIds(saleIds));
    }
    ownerIds.sort(Comparator.comparing(UserDto::getId));
    LOG.info("Round Robin dispatch for customer: {} owners: {}", customerId, ownerIds);
    if (ownerIds.size() > 1) {
      Long lastSaleId = configurationService.getConfigDispatchRoundRobin();
      return ownerIds.stream().filter(u -> u.getId() > lastSaleId).findFirst().get();
    }
    return ownerIds.get(0);
  }
}
