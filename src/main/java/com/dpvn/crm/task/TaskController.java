package com.dpvn.crm.task;

import com.dpvn.crm.user.UserService;
import com.dpvn.crmcrudservice.domain.dto.TaskDto;
import com.dpvn.shared.util.FastMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
public class TaskController {
  private final TaskService taskService;
  private final UserService userService;

  public TaskController(TaskService taskService, UserService userService) {
    this.taskService = taskService;
    this.userService = userService;
  }

  /**
   *  - userId -> sale id
   *  - customerId
   *  - filterText : find in (title, name, content fields)
   *  - tags: NOT YET
   *  - statuses: List String
   *  - progresses: List Integer
   *  - sorts: List String (should be snake_case as physical fields)
   */
  @PostMapping("/find-by-options")
  public FastMap getAllTasks(
      @RequestHeader("x-user-id") Long loginUserId, @RequestBody FastMap body) {
    if (!userService.isGod(loginUserId)) {
      body.add("userId", loginUserId);
    }
    return taskService.findTasks(body);
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

  @PostMapping("/{id}")
  public void updateExistedTask(
      @RequestHeader("x-user-id") Long loginUserId,
      @PathVariable("id") Long id,
      @RequestBody FastMap body) {
    taskService.updateExistedTask(loginUserId, id, body);
  }

  @DeleteMapping("/{id}")
  public void deleteTask(
      @RequestHeader("x-user-id") Long loginUserId, @PathVariable("id") Long id) {
    taskService.deleteTask(loginUserId, id);
  }
}
