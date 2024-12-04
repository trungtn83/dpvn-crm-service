package com.dpvn.crm.task;

import com.dpvn.crm.client.CrmCrudClient;
import com.dpvn.crmcrudservice.domain.dto.TaskDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class TaskService {

  private final CrmCrudClient crmCrudClient;

  public TaskService(CrmCrudClient crmCrudClient) {
    this.crmCrudClient = crmCrudClient;
  }

  public List<TaskDto> getAllTasks(
      Long userId, Long customerId, Long campaignId, Long kpiId, Long otherId) {
    return crmCrudClient.getAllTasks(userId, customerId, campaignId, kpiId, otherId);
  }

  public void upsertTask(TaskDto body) {
    crmCrudClient.upsertTask(body);
  }

  public void deleteTask(Long userId, Long id) {
    // validate userId can delete task id or not
    crmCrudClient.deleteTask(id);
  }
}
