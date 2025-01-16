package com.dpvn.crm.task;

import com.dpvn.crmcrudservice.domain.dto.TaskDto;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
public class TaskController {
  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GetMapping("/find-by-options")
  public List<TaskDto> getAllTasks(
      @RequestHeader("x-user-id") Long loginUserId,
      @RequestParam(required = false) Long customerId,
      @RequestParam(required = false) Long campaignId,
      @RequestParam(required = false) Long kpiId,
      @RequestParam(required = false) Long otherId) {
    return taskService.getAllTasks(loginUserId, customerId, campaignId, kpiId, otherId);
  }

  @GetMapping("/task")
  public List<TaskDto> getAllTasks(
      @RequestHeader("x-user-id") Long loginUserId, @RequestParam Long customerId) {
    return taskService.getAllTasks(loginUserId, customerId, null, null, null);
  }

  @PostMapping
  public void createNewTask(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody TaskDto body) {
    body.setCreatedBy(loginUserId);
    if (body.getUserId() == null) {
      body.setUserId(loginUserId);
    }
    taskService.createNewTask(body);
  }

  @DeleteMapping("/{id}")
  public void deleteTask(
      @RequestHeader("x-user-id") Long loginUserId, @PathVariable("id") Long id) {
    taskService.deleteTask(loginUserId, id);
  }
}
