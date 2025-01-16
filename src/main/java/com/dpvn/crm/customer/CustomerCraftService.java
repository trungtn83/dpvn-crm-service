package com.dpvn.crm.customer;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.client.ReportCrudClient;
import com.dpvn.crmcrudservice.domain.constant.Customers;
import com.dpvn.crmcrudservice.domain.constant.Genders;
import com.dpvn.crmcrudservice.domain.dto.CustomerAddressDto;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.CustomerReferenceDto;
import com.dpvn.kiotviet.domain.KvCustomerDto;
import com.dpvn.shared.domain.constant.Globals;
import com.dpvn.shared.service.AbstractService;
import com.dpvn.shared.util.DateUtil;
import com.dpvn.shared.util.FastMap;
import com.dpvn.shared.util.ListUtil;
import com.dpvn.shared.util.ObjectUtil;
import com.dpvn.shared.util.StringUtil;
import com.dpvn.thuocsi.domain.TsAddressDto;
import com.dpvn.thuocsi.domain.TsCustomerDto;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CustomerCraftService extends AbstractService {
  private final ReportCrudClient reportCrudClient;
  private final CrmCrudClient crmCrudClient;

  public CustomerCraftService(ReportCrudClient reportCrudClient, CrmCrudClient crmCrudClient) {
    this.reportCrudClient = reportCrudClient;
    this.crmCrudClient = crmCrudClient;
  }

  public void craftCustomers(Integer sourceId) {
    switch (sourceId) {
      case Customers.Source.KIOTVIET:
        craftKiotVietCustomers();
        break;
      case Customers.Source.CRAFTONLINE:
        craftCraftOnlineCustomers();
        break;
      default:
        break;
    }
  }

  private void craftKiotVietCustomers() {
    // TODO: call directly to kiotviet crud service to get list of customers
    CustomerDto lastCreatedCustomer =
        crmCrudClient.findLastCreatedCustomer(Customers.Source.KIOTVIET);
    Instant lastCreatedCustomerDate =
        lastCreatedCustomer == null ? DateUtil.WELCOME_DATE : lastCreatedCustomer.getCreatedDate();
    int page = 0;
    while (craftKiotVietCustomers(lastCreatedCustomerDate, page)) {
      page++;
    }
    LOGGER.info(
        String.format(
            "Synced all customers from KiotViet to CRM, last created date: %s",
            lastCreatedCustomerDate),
        lastCreatedCustomerDate);
  }

  private boolean craftKiotVietCustomers(Instant lastCreatedCustomerDate, int page) {
    List<KvCustomerDto> kvCustomerDtos =
        reportCrudClient.findForSync(
            FastMap.create()
                .add("lastCreatedDate", lastCreatedCustomerDate)
                .add("page", page)
                .add("pageSize", Globals.Paging.FETCHING_PAGE_SIZE));
    if (ListUtil.isNotEmpty(kvCustomerDtos)) {
      List<CustomerDto> customerDtos =
          kvCustomerDtos.stream()
              .map(CustomerCraftService::tranformKiotVietCustomerDtoToCustomerDto)
              .toList();
      crmCrudClient.syncAllCustomers(customerDtos);
      LOGGER.info(
          ListUtil.toString(customerDtos.stream().map(CustomerDto::getMobilePhone).toList()),
          String.format("Synced %d customers from KiotViet to CRM", customerDtos.size()));
    }
    return kvCustomerDtos.size() == Globals.Paging.FETCHING_PAGE_SIZE;
  }

  public static CustomerDto tranformKiotVietCustomerDtoToCustomerDto(KvCustomerDto kvCustomerDto) {
    CustomerDto customerDto = new CustomerDto();
    customerDto.setSourceId(Customers.Source.KIOTVIET);
    customerDto.setIdf(kvCustomerDto.getIdf());
    customerDto.setCustomerCode(kvCustomerDto.getCode());
    customerDto.setCustomerName(kvCustomerDto.getName());
    customerDto.setGender(
        Objects.equals(kvCustomerDto.getGender(), Boolean.TRUE) ? Genders.MALE : Genders.FEMALE);
    customerDto.setMobilePhone(kvCustomerDto.getContactNumber());
    customerDto.setEmail(kvCustomerDto.getEmail());
    customerDto.setTaxCode(kvCustomerDto.getTaxCode());
    customerDto.setPinCode(kvCustomerDto.getIdentificationNumber());
    customerDto.setLevelPoint(kvCustomerDto.getRewardPoint());
    customerDto.setCreatedDate(kvCustomerDto.getCreatedDate());
    customerDto.setCreatedBy(kvCustomerDto.getCreatedBy());
    customerDto.setModifiedBy(kvCustomerDto.getModifiedBy());

    if (StringUtil.isNotEmpty(kvCustomerDto.getFacebook())) {
      CustomerReferenceDto customerReferenceDto = new CustomerReferenceDto();
      customerReferenceDto.setCode(Customers.References.FACEBOOK);
      customerReferenceDto.setName(kvCustomerDto.getFacebook());
      customerDto.getReferences().add(customerReferenceDto);
    }

    CustomerAddressDto customerAddressDto = new CustomerAddressDto();
    customerAddressDto.setStatus("DEFAULT");
    customerAddressDto.setAddress(kvCustomerDto.getAddress());
    customerAddressDto.setWardName(kvCustomerDto.getWardName());
    List<String> names = splitLocationName(kvCustomerDto.getLocationName());
    if (names.size() == 2) {
      customerAddressDto.setDistrictName(names.get(1));
      customerAddressDto.setProvinceName(names.get(0));
    } else {
      customerAddressDto.setProvinceName(kvCustomerDto.getLocationName());
    }
    customerDto.setAddresses(List.of(customerAddressDto));
    customerDto.setCustomerType(convertKvCustomerType(kvCustomerDto));
    customerDto.setNotes(kvCustomerDto.getComments());
    customerDto.setActive(true);
    customerDto.setDeleted(false);
    return customerDto;
  }

  /** Some location like 'Ba Ria - Vung Tau - TInh brvn */
  private static List<String> splitLocationName(String locationName) {
    if (StringUtil.isEmpty(locationName)) {
      return List.of();
    }
    int lastIndex = locationName.lastIndexOf(" - "); // Find the last occurrence of " - "

    List<String> names;
    if (lastIndex != -1) {
      // Split into two parts: before and after the last " - "
      String firstPart = locationName.substring(0, lastIndex);
      String secondPart = locationName.substring(lastIndex + 3); // +3 to skip the " - "
      names = List.of(firstPart, secondPart);
    } else {
      // If " - " is not found, return the whole string as a single element
      names = List.of(locationName);
    }

    return names;
  }

  private static String convertKvCustomerType(KvCustomerDto kvCustomerDto) {
    String type = kvCustomerDto.getCustomerType();
    if ("công ty".equalsIgnoreCase(type)) {
      return "COMPANY";
    } else if ("cá nhân".equalsIgnoreCase(type)) {
      String name = kvCustomerDto.getName();
      if (StringUtil.isNotEmpty(name) && name.toLowerCase().contains("khách lẻ")) {
        return "PATIENT";
      }
      return "PHARMACY";
    }
    return "OTHER";
  }

  private void craftCraftOnlineCustomers() {
    // TODO: it should be call to thuocsi service to get list of customers, but to reduce delay
    // time, call directly to report client

    int page = 0;
    while (craftCraftOnlineCustomers(page)) {
      page++;
    }
  }

  private boolean craftCraftOnlineCustomers(int page) {
    List<TsCustomerDto> tsCustomerDtos =
        reportCrudClient.findTsCustomers(page, Globals.Paging.FETCHING_PAGE_SIZE);
    if (ListUtil.isNotEmpty(tsCustomerDtos)) {
      List<CustomerDto> customerDtos =
          tsCustomerDtos.stream().map(this::tranformCraftOnlineCustomerDtoToCustomerDto).toList();
      crmCrudClient.syncAllCustomers(customerDtos);
      LOGGER.info(
          ListUtil.toString(customerDtos),
          String.format("Synced %d customers from CRAFTONLINE to CRM", customerDtos.size()));
    }
    return tsCustomerDtos.size() == Globals.Paging.FETCHING_PAGE_SIZE;
  }

  private CustomerDto tranformCraftOnlineCustomerDtoToCustomerDto(TsCustomerDto tsCustomerDto) {
    CustomerDto customerDto = new CustomerDto();
    customerDto.setSourceId(Customers.Source.CRAFTONLINE);
    customerDto.setCustomerCode(tsCustomerDto.getCode());
    customerDto.setCustomerName(tsCustomerDto.getName());
    customerDto.setSourceNote(
        ObjectUtil.writeValueAsString(
            FastMap.create()
                .add("Representative", tsCustomerDto.getLegalRepresentative())
                .add("License", tsCustomerDto.getLicensePath())
                .add("Tag", tsCustomerDto.getHashTag())
                .add("Scope", tsCustomerDto.getScope())
                .add("GPP", tsCustomerDto.getGppPath())));
    customerDto.setMobilePhone(tsCustomerDto.getPhone());
    customerDto.setEmail(tsCustomerDto.getEmail());
    CustomerAddressDto customerAddressDto = new CustomerAddressDto();
    customerAddressDto.setStatus("DEFAULT");
    customerAddressDto.setAddress(tsCustomerDto.getAddress());
    TsAddressDto tsAddressDto =
        reportCrudClient.findAllTsAddresses().stream()
            .filter(a -> a.getCode().equals(tsCustomerDto.getWardCode()))
            .findFirst()
            .orElse(null);
    if (tsAddressDto != null) {
      customerAddressDto.setWardName(tsAddressDto.getName());
      customerAddressDto.setDistrictName(tsAddressDto.getDistrictName());
      customerAddressDto.setProvinceName(tsAddressDto.getProvinceName());
    }
    customerDto.setAddresses(List.of(customerAddressDto));

    customerDto.setTaxCode(tsCustomerDto.getTaxCode());
    customerDto.setCustomerType(tsCustomerDto.getScope()); // get type id from scope
    customerDto.setActive(false);
    return customerDto;
  }
}
