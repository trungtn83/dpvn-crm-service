package com.dpvn.crm.interaction;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.user.UserService;
import com.dpvn.crmcrudservice.domain.constant.Visibility;
import com.dpvn.crmcrudservice.domain.dto.InteractionDto;
import com.dpvn.shared.util.FastMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

  public List<InteractionDto> getAllInteractions(Long saleId, Long customerId) {
    List<InteractionDto> interactionDtos =
        crmCrudClient.getAllInteractions(null, customerId, null, null);
    return interactionDtos.stream()
        .filter(
            i ->
                Objects.equals(i.getInteractBy(), saleId) || i.getVisibility() == Visibility.PUBLIC)
        .toList();
  }

  public FastMap findInteractionsByOptions(
      Long userId, Long customerId, Long campaignId, boolean isLite) {
    List<InteractionDto> myInteractions =
        crmCrudClient.getAllInteractions(userId, customerId, campaignId, null);
    List<InteractionDto> otherPublicInteractions =
        crmCrudClient.getAllInteractions(null, customerId, campaignId, Visibility.PUBLIC);
    List<InteractionDto> interactionDtos =
        new ArrayList<>(
            Stream.concat(myInteractions.stream(), otherPublicInteractions.stream())
                .filter(interaction -> interaction.getId() != null) // Optional: exclude null IDs
                .collect(
                    Collectors.toMap(
                        InteractionDto::getId, // Use getId() as the key for uniqueness
                        interaction -> interaction, // Keep the whole InteractionDto as the value
                        (existing, replacement) ->
                            existing // If there’s a duplicate ID, keep the existing object
                        ))
                .values());

    FastMap result = FastMap.create().add("interactions", interactionDtos);
    if (!isLite) {
      List<Long> userIds = interactionDtos.stream().map(InteractionDto::getInteractBy).toList();
      result.add("users", userService.findUsersByIdsMapByIds(userIds));
    }

    return result;
  }

  public void createInteraction(@RequestBody InteractionDto body) {
    crmCrudClient.createInteraction(body);
  }
}
