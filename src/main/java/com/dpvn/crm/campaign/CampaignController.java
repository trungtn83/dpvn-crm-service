package com.dpvn.crm.campaign;

import com.dpvn.crmcrudservice.domain.dto.CampaignDto;
import com.dpvn.shared.util.FastMap;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/campaign")
public class CampaignController {
  private final CampaignService campaignService;

  public CampaignController(CampaignService campaignService) {
    this.campaignService = campaignService;
  }

  @GetMapping
  public List<CampaignDto> findAllCampaigns() {
    return campaignService.findAllCampaigns().getRows();
  }

  @PostMapping("/{id}/assign-customers-to-sales")
  public void assignCustomers(
      @RequestHeader("x-user-id") Long loginUserId,
      @PathVariable Long id,
      @RequestBody FastMap body) {
    body.add("userId", loginUserId);
    campaignService.assignCustomers(id, body);
  }
}
