package com.dpvn.crm.interaction;

import com.dpvn.crmcrudservice.domain.constant.Visibility;
import com.dpvn.crmcrudservice.domain.dto.InteractionDto;

public class InteractionUtil {
  private InteractionUtil() {}

  public static InteractionDto generateSystemInteraction(
      Long userId, Long customerId, Long campaignId, String content) {
    InteractionDto interaction = new InteractionDto();
    interaction.setTypeId(-1);
    interaction.setType("Hệ thống");
    interaction.setCreatedBy(-1L);
    interaction.setInteractBy(userId);
    interaction.setCampaignId(campaignId);
    interaction.setCustomerId(customerId);
    interaction.setTitle("Tin nhắn tự động");
    interaction.setContent(content);
    interaction.setVisibility(Visibility.PUBLIC);
    return interaction;
  }
}
