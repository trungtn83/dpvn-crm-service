package com.dpvn.crm.interaction;

import com.dpvn.crmcrudservice.domain.dto.InteractionDto;
import com.dpvn.shared.util.FastMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interaction")
public class InteractionController {
  private final InteractionService interactionService;

  public InteractionController(InteractionService interactionService) {
    this.interactionService = interactionService;
  }

  @GetMapping("/find-by-options")
  public FastMap findInteractionsByOptions(
      @RequestHeader("x-user-id") Long loginUserId,
      @RequestParam(required = false) Long customerId,
      @RequestParam(required = false) Long campaignId,
      @RequestParam(required = false, defaultValue = "false") boolean isLite) {
    return interactionService.findInteractionsByOptions(
        loginUserId, customerId, campaignId, isLite);
  }

  @PostMapping("/upsert")
  public void upsertInteraction(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody InteractionDto body) {
    body.setCreatedBy(loginUserId);
    body.setInteractBy(loginUserId);
    interactionService.upsertInteraction(body);
  }
}
