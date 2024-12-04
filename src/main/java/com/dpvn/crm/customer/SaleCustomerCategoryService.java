package com.dpvn.crm.customer;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerCategoryDto;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.util.ListUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SaleCustomerCategoryService {
  private final CrmCrudClient crmCrudClient;

  public SaleCustomerCategoryService(CrmCrudClient crmCrudClient) {
    this.crmCrudClient = crmCrudClient;
  }

  public List<SaleCustomerCategoryDto> findSaleCustomerCategoriesByOptions(Long saleId, String code) {
    return crmCrudClient.findSaleCustomerCategoriesByOptions(saleId, code);
  }

  public void upsertSaleCustomerCategory(SaleCustomerCategoryDto saleCustomerCategoryDto) {
    if (saleCustomerCategoryDto.getId() == null) {
      List<SaleCustomerCategoryDto> saleCustomerCategoryDtos = crmCrudClient.findSaleCustomerCategoriesByOptions(saleCustomerCategoryDto.getSaleId(), saleCustomerCategoryDto.getCode());
      if (ListUtil.isNotEmpty(saleCustomerCategoryDtos)) {
        throw new BadRequestException("EXISTED", String.format("Category code %s existed", saleCustomerCategoryDto.getCode()));
      }
    }
    crmCrudClient.upsertSaleCustomerCategory(saleCustomerCategoryDto);
  }

  public void deleteSaleCustomerCategory(Long saleId, Long id) {
    // TODO: check if id is belong to saleId
    crmCrudClient.deleteSaleCustomerCategory(id);
  }

}
