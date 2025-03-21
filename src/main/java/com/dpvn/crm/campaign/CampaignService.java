package com.dpvn.crm.campaign;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crmcrudservice.domain.dto.CampaignDto;
import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.FastMap;
import org.springframework.stereotype.Service;

@Service
public class CampaignService extends AbstractService {
  private final CrmCrudClient crmCrudClient;

  public CampaignService(CrmCrudClient crmCrudClient) {
    this.crmCrudClient = crmCrudClient;
  }

  public PagingResponse<CampaignDto> findAllCampaigns() {
    return crmCrudClient.findAllCampaigns();
  }

  public void assignCustomers(Long campaignId, FastMap body) {
    crmCrudClient.assignToSaleInCampaign(campaignId, body);
  }
}
