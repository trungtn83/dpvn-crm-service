package com.dpvn.crm.webhook;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.customer.SaleCustomerService;
import com.dpvn.crmcrudservice.domain.constant.RelationshipType;
import com.dpvn.crmcrudservice.domain.constant.SaleCustomers;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.SaleCustomerDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.shared.domain.constant.Globals;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.DateUtil;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ListUtil;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WebHookHandlerService extends AbstractService {
  private final SaleCustomerService saleCustomerService;
  private final CrmCrudClient crmCrudClient;

  public WebHookHandlerService(
      SaleCustomerService saleCustomerService, CrmCrudClient crmCrudClient) {
    this.saleCustomerService = saleCustomerService;
    this.crmCrudClient = crmCrudClient;
  }

  public boolean isOldCustomer(Long saleId, Long customerId) {
    FastMap condition =
        FastMap.create()
            .add("saleId", saleId)
            .add("customerIds", List.of(customerId))
            .add("relationshipType", RelationshipType.PIC)
            .add("reasonIds", List.of(SaleCustomers.Reason.INVOICE));
    List<SaleCustomerDto> saleCustomerDtos = crmCrudClient.findSaleCustomersByOptions(condition);
    return ListUtil.isNotEmpty(saleCustomerDtos);
  }

  /**
   * Khi tạo ra temp order - Nếu khách mới thì tạo ra record mới cho sale-customer có giá trị trong
   * 7 ngày - Nếu khách cũ thì không cần làm gì do đã có record rồi
   */
  public void handleTempOrder(
      UserDto sale, CustomerDto customer, String code, Instant purchaseDate) {
    LOGGER.info("Processing temp order {} of customer {} by sale {}", code, customer, sale);
    Instant from = purchaseDate != null ? purchaseDate : DateUtil.now();
    Instant to = from.plus(Globals.Customer.LIFE_TIME_TEMP_ORDER_IN_DAYS, ChronoUnit.DAYS);

    SaleCustomerDto saleCustomerDto =
        saleCustomerService.findSaleCustomerByReason(
            sale.getId(), customer.getId(), null, null, code);
    if (saleCustomerDto != null) {
      saleCustomerService.updateExistedSaleCustomer(
          saleCustomerDto.getId(),
          FastMap.create()
              .add("reasonId", SaleCustomers.Reason.ORDER)
              .add("availableFrom", from)
              .add("availableTo", to));
      LOGGER.info(
          "Sale customer [{}, {}] for ORDER code {} existed, process update", sale, customer, code);
      return;
    }

    // TODO: Có cần check nếu khách cũ nhưng còn ít hơn 7 ngày thì có nên tạo không???
    if (!isOldCustomer(sale.getId(), customer.getId())) {
      saleCustomerService.createNewSaleCustomer(
          generateAssignCustomerToSaleWithTypeInDays(
              sale.getId(), customer.getId(), SaleCustomers.Reason.ORDER, code, from, to));
      LOGGER.info(
          "Sale customer [{}, {}] for ORDER code {} does not exist, create the new one for 7 days",
          sale,
          customer,
          code);
      return;
    }

    LOGGER.info(
        "Sale customer [{}, {}] is old customer, temp ORDER does not affect to current relationship",
        sale,
        customer);
  }

  /**
   * Khi tạo ra confirmed order: tạo record mới cho sale với khách đó trong 3 tháng
   */
  public void handleConfirmedOrder(
      UserDto sale, CustomerDto customer, String code, Instant purchaseDate) {
    Instant from = purchaseDate != null ? purchaseDate : DateUtil.now();
    Instant to = from.plus(Globals.Customer.LIFE_TIME_TREASURE_IN_DAYS, ChronoUnit.DAYS);

    SaleCustomerDto saleCustomerDto =
        saleCustomerService.findSaleCustomerByReason(
            sale.getId(), customer.getId(), null, null, code);
    if (saleCustomerDto != null) {
      saleCustomerService.updateExistedSaleCustomer(
          saleCustomerDto.getId(),
          FastMap.create()
              .add("relationshipType", RelationshipType.PIC)
              .add("reasonId", SaleCustomers.Reason.ORDER)
              .add("availableFrom", from)
              .add("availableTo", to));
    } else {
      saleCustomerService.createNewSaleCustomer(
          generateAssignCustomerToSaleWithTypeInDays(
              sale.getId(), customer.getId(), SaleCustomers.Reason.ORDER, code, from, to));
    }
  }

  /**
   * Khi order bị huỷ: tìm sale-customer với reason là Order và reason ref là Order đó xoá đi Không
   * quan tâm type = gì vì có thể huỷ khi Order đang là temp hay confirmed
   */
  public void handleCancelledOrder(UserDto sale, CustomerDto customer, String code) {
    saleCustomerService.removeSaleCustomerByReason(
        sale.getId(), customer.getId(), SaleCustomers.Reason.ORDER, code);
  }

  /**
   * Khi order là completed, không quan tâm bới sẽ handle trong case Invoice In-Progress Khi Invoice
   * là in-progress - Update relationship type của sale-customer sang INVOICE, ref sang invoice code
   * - Update available time của customer đó theo ngày hiện tại + rule 3 tháng
   */
  public void handleInProgressInvoice(
      UserDto sale, CustomerDto customer, String code, Instant purchaseDate) {
    Instant from = purchaseDate != null ? purchaseDate : DateUtil.now();
    Instant to = from.plus(Globals.Customer.LIFE_TIME_TREASURE_IN_DAYS, ChronoUnit.DAYS);

    SaleCustomerDto saleCustomerDto =
        saleCustomerService.findSaleCustomerByReason(
            sale.getId(), customer.getId(), null, SaleCustomers.Reason.ORDER, null);
    if (saleCustomerDto != null) {
      saleCustomerDto.setRelationshipType(RelationshipType.PIC);
      saleCustomerDto.setReasonId(SaleCustomers.Reason.INVOICE);
      saleCustomerDto.setReasonRef(code);
      saleCustomerDto.setAvailableFrom(from);
      saleCustomerDto.setAvailableTo(to);
      saleCustomerService.updateExistedSaleCustomer(
          saleCustomerDto.getId(),
          FastMap.create()
              .add("relationshipType", RelationshipType.PIC)
              .add("reasonId", SaleCustomers.Reason.INVOICE)
              .add("reasonRef", code)
              .add("availableFrom", from)
              .add("availableTo", to));
    } else {
      saleCustomerService.createNewSaleCustomer(
          generateAssignCustomerToSaleWithTypeInDays(
              sale.getId(), customer.getId(), SaleCustomers.Reason.INVOICE, code, from, to));
    }
  }

  /**
   * Khi Invoice là completed: hình như chưa cần làm gì
   */
  public void handleCompletedInvoice(
      UserDto sale, CustomerDto customer, String code, Instant purchaseDate) {
    handleInProgressInvoice(sale, customer, code, purchaseDate);
  }

  public void handleCancelledInvoice(UserDto sale, CustomerDto customer, String code) {
    saleCustomerService.removeSaleCustomerByReason(
        sale.getId(), customer.getId(), SaleCustomers.Reason.INVOICE, code);
  }

  private SaleCustomerDto generateAssignCustomerToSaleWithTypeInDays(
      Long saleId, Long customerId, int type, String typeRef, Instant fromDate, Instant toDate) {
    SaleCustomerDto saleCustomerDto = new SaleCustomerDto();
    saleCustomerDto.setCustomerId(customerId);
    saleCustomerDto.setSaleId(saleId);
    saleCustomerDto.setRelationshipType(RelationshipType.PIC);
    saleCustomerDto.setReasonId(type);
    saleCustomerDto.setReasonRef(typeRef);
    saleCustomerDto.setReasonNote(
        String.format("Handling: [Ref=%s, sale=%s, customer=%s]", typeRef, saleId, customerId));
    saleCustomerDto.setAvailableFrom(fromDate);
    saleCustomerDto.setAvailableTo(toDate);
    saleCustomerDto.setActive(true);
    saleCustomerDto.setDeleted(false);
    return saleCustomerDto;
  }
}
