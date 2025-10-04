package com.dpvn.crm.client;

import com.dpvn.shared.util.FastMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
    name = "misa-amis-service",
    contextId = "misa-amis-service-client",
    configuration = MisaAmisFeignClientConfig.class)
public interface MisaAmisServiceClient {

  /**
   * @param refs gồm refid và inv_refid cho tiện việc query tới misa
   */
  @PostMapping("/voucher/invoice-show")
  List<String> getInvoiceShow(@RequestBody List<FastMap> refs);
}
