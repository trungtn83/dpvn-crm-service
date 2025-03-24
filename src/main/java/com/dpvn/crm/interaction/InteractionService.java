package com.dpvn.crm.interaction;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.user.UserService;
import com.dpvn.crmcrudservice.domain.constant.Visibility;
import com.dpvn.crmcrudservice.domain.dto.InteractionDto;
import com.dpvn.shared.util.FastMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class InteractionService {

  private final CrmCrudClient crmCrudClient;
  private final UserService userService;

  public InteractionService(CrmCrudClient crmCrudClient, UserService userService) {
    this.crmCrudClient = crmCrudClient;
    this.userService = userService;
  }

  public FastMap findInteractionsByOptions(
      Long userId, Long customerId, Long campaignId, boolean isLite) {
    FastMap myParams = FastMap.create().add("customerId", customerId).add("campaignId", campaignId);
    if (!userService.isGod(userId)) {
      myParams.add("userId", userId);
    }
    List<InteractionDto> myInteractions = crmCrudClient.findAllInteractions(myParams);

    FastMap publicParams =
        FastMap.create()
            .add("customerId", customerId)
            .add("campaignId", campaignId)
            .add("visibility", Visibility.PUBLIC);
    List<InteractionDto> otherPublicInteractions = crmCrudClient.findAllInteractions(publicParams);

    List<InteractionDto> interactionDtos =
        new ArrayList<>(
            Stream.concat(myInteractions.stream(), otherPublicInteractions.stream())
                .filter(interaction -> interaction.getId() != null) // Optional: exclude null IDs
                .collect(
                    Collectors.toMap(
                        InteractionDto::getId,
                        interaction -> interaction,
                        (existing, replacement) -> existing))
                .values());

    FastMap result = FastMap.create().add("interactions", interactionDtos);
    if (!isLite) {
      List<Long> userIds = interactionDtos.stream().map(InteractionDto::getInteractBy).toList();
      result.add("users", userService.findUsersByIdsMapByIds(userIds));
    }

    return result;
  }

  public void createInteraction(@RequestBody InteractionDto body) {
    body.setActive(Boolean.TRUE);
    body.setDeleted(Boolean.FALSE);
    crmCrudClient.createInteraction(body);
  }
}
