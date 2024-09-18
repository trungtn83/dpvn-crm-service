package com.dpvn.crm.user;

import com.dpvn.crm.hrm.leave.LeaveRequestService;
import com.dpvn.crmcrudservice.domain.dto.LeaveRequestDto;
import com.dpvn.crmcrudservice.domain.dto.UserDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/find-by-user/{userId}")
  public List<UserDto> getByUser(@PathVariable(name = "userId") Long userId) {
    return userService.getByUser(userId);
  }
}
