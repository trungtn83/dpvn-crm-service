package com.dpvn.crm.task;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crm.user.UserService;
import com.dpvn.crmcrudservice.domain.constant.Tasks;
import com.dpvn.crmcrudservice.domain.dto.CustomerDto;
import com.dpvn.crmcrudservice.domain.dto.TaskDto;
import com.dpvn.shared.exception.BadRequestException;
import com.dpvn.shared.util.FastMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

  private final CrmCrudClient crmCrudClient;
  private final UserService userService;

  public TaskService(CrmCrudClient crmCrudClient, UserService userService) {
    this.crmCrudClient = crmCrudClient;
    this.userService = userService;
  }

  public void createNewTask(TaskDto body) {
    body.setActive(Boolean.TRUE);
    body.setDeleted(Boolean.FALSE);
    crmCrudClient.createNewTask(body);
  }

  public void deleteTask(Long loginUserId, Long id) {
    TaskDto existingTask = crmCrudClient.findTaskById(id);
    if (existingTask == null) {
      throw new BadRequestException("NOT_FOUND", String.format("Task with id %s not found", id));
    }
    if (userService.isGod(loginUserId) || loginUserId.equals(existingTask.getUserId())) {
      crmCrudClient.deleteTask(id);
    } else {
      throw new BadRequestException(
          "FORBIDDEN",
          String.format(
              "User with id %s is not allowed to delete task with id %s", loginUserId, id));
    }
  }

  public void updateExistedTask(Long loginUserId, Long id, FastMap body) {
    TaskDto existingTask = crmCrudClient.findTaskById(id);
    if (existingTask == null) {
      throw new BadRequestException("NOT_FOUND", String.format("Task with id %s not found", id));
    }
    if (userService.isGod(loginUserId) || loginUserId.equals(existingTask.getUserId())) {
      crmCrudClient.updateExistedTask(id, body);
    } else {
      throw new BadRequestException(
          "FORBIDDEN",
          String.format(
              "User with id %s is not allowed to update task with id %s", loginUserId, id));
    }
  }

  public FastMap findTasks(FastMap body) {
    FastMap result = crmCrudClient.findTasks(body);
    List<FastMap> rows = result.getListClass("rows", FastMap.class);
    List<Long> customerIds =
        rows.stream().map(row -> row.getLong("customerId")).filter(Objects::nonNull).toList();
    List<CustomerDto> customerDtos = crmCrudClient.findCustomerByIds(customerIds);
    Map<Long, CustomerDto> customerDtoMap =
        customerDtos.stream().collect(Collectors.toMap(CustomerDto::getId, c -> c));
    rows.forEach(
        row ->
            row.add(
                "customer",
                customerDtoMap.getOrDefault(row.getLong("customerId"), new CustomerDto())));
    return result.add("rows", rows);
  }

  public List<TaskDto> findTasksReportBySeller(Long sellerId, String fromDate, String toDate) {
    FastMap body =
        FastMap.create()
            .add("sellerId", sellerId)
            .add("progress", List.of(Tasks.Progress.DONE))
            .add("fromDate", fromDate)
            .add("toDate", toDate);
    FastMap result = crmCrudClient.findTasks(body);
    return result.getListClass("rows", TaskDto.class);
  }
}
