package com.dpvn.crm.client;

import com.dpvn.shared.util.FastMap;
import com.dpvn.storageservice.domain.FileDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "storage-service",
    contextId = "storage-service-client",
    url = "${storage-service.url}")
public interface StorageClient {
  @PostMapping("/file/upload-from-urls")
  List<FileDto> uploadFileFromUrls(@RequestBody List<FastMap> urls);
}
