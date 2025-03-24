package com.dpvn.crm.voip24h.client;

import com.dpvn.crm.voip24h.domain.ViResponse;
import com.dpvn.shared.util.FastMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "voip24h-client",
    url = "https://api.voip24h.vn/v3",
    configuration = Voip24hClientConfig.class)
public interface Voip24hClient {
  @GetMapping("/extension/list")
  ViResponse getExtensionList();

  /**
   * - dateStart: 2025-03-13 or 2025-03-13 12:23:45
   * - dateEnd: 2025-03-13 or 2025-03-13 12:23:45
   * dateStart	Kiểu chuỗi	Không bắt buộc	Ngày bắt đầu lấy dữ liệu
   * dateEnd	Kiểu chuỗi	Không bắt buộc	Ngày kết thúc lấy dữ liệu
   * status	Kiểu chuỗi	Không bắt buộc	Trạng thái cuộc gọi(các trạng thái ngăn cách nhau bởi dấu ' , ')
   * type	Kiểu chuỗi	Không bắt buộc	Loại cuộc gọi(các loại cuộc gọi ngăn cách nhau bởi dấu ',')
   * extension	Kiểu chuỗi	Không bắt buộc	Chuỗi máy nhánh(các máy nhánh ngăn cách nhau bởi dấu ' , ')
   * did	Kiểu chuỗi	Không bắt buộc	Đầu số gọi
   * callid	Kiểu chuỗi	Không bắt buộc	Mã cuộc gọi trên tổng đài
   * id	Kiểu chuỗi	Không bắt buộc	Mã id của cuộc gọi
   * offset	Kiểu số nguyên	Không bắt buộc	Vị trí bắt đầu lấy dữ liệu
   * limit	Kiểu số nguyên	Không bắt buộc	Số lượng dữ liệu được lấy
   * caller	Kiểu chuỗi	Không bắt buộc	Số điện thoại gọi
   * callee	Kiểu chuỗi	Không bắt buộc	Số điện nhận cuộc gọi
   */
  @GetMapping("/call/history")
  ViResponse getCallHistory(@RequestParam FastMap params);

  @GetMapping("/call/recording")
  ViResponse getCallRecording(@RequestParam String callId);
}
