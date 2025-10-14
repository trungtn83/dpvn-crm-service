package com.dpvn.crm.interaction;

import com.dpvn.crmcrudservice.domain.dto.InteractionDto;
import com.dpvn.shared.domain.constant.Globals;
import com.dpvn.shared.util.FastMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interaction")
public class InteractionController {
  private final InteractionService interactionService;

  public InteractionController(InteractionService interactionService) {
    this.interactionService = interactionService;
  }

  @PostMapping("/find-by-options")
  public FastMap findInteractionsByOptions(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody FastMap body) {
    Long customerId = body.getLong("customerId");
    Long campaignId = body.getLong("campaignId");
    boolean isLite = body.getBoolean("isLite");
    int page = body.getInt(0, "page");
    int pageSize = body.getInt(Globals.Paging.PAGE_SIZE, "pageSize");
    return interactionService.findInteractionsByOptions(
        loginUserId, customerId, campaignId, isLite, page, pageSize);
  }

  @PostMapping
  public void createNewInteraction(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody InteractionDto body) {
    body.setCreatedBy(loginUserId);
    body.setInteractBy(loginUserId);
    interactionService.createInteraction(body);
  }
}
