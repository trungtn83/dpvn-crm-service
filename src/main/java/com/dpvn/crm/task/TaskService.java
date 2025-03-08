package com.dpvn.crm.task;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.TaskDto;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.dpvn.shared.util.FastMap;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

  private final CrmCrudClient crmCrudClient;

  public TaskService(CrmCrudClient crmCrudClient) {
    this.crmCrudClient = crmCrudClient;
  }

  public void createNewTask(TaskDto body) {
    body.setActive(Boolean.TRUE);
    body.setDeleted(Boolean.FALSE);
    crmCrudClient.createNewTask(body);
  }

  public void deleteTask(Long userId, Long id) {
    // validate userId can delete task id or not
    crmCrudClient.deleteTask(id);
  }

  public FastMap findTasks(FastMap body) {
    FastMap result = crmCrudClient.findTasks(body);
    List<FastMap> rows = result.getListClass("rows", FastMap.class);
    List<Long> customerIds = rows.stream().map(row -> row.getLong("customerId")).filter(Objects::nonNull).toList();
    List<CustomerDto> customerDtos = crmCrudClient.findCustomerByIds(customerIds);
    Map<Long, CustomerDto> customerDtoMap = customerDtos.stream().collect(Collectors.toMap(CustomerDto::getId, c -> c));
    rows.forEach(row -> row.add("customer", customerDtoMap.getOrDefault(row.getLong("customerId"), new CustomerDto())));
    return result.add("rows", rows);
  }
}
