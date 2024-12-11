package com.dpvn.crm.user;

import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.shared.util.FastMap;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

  @PostMapping("/find-by-options")
  public List<UserDto> findUsersByOptions(@RequestBody UserDto userDto) {
    return userService.findUsersByOptions(userDto);
  }

  /**
   * - filterText - department: get from Constant please - role: get from Constant please - page :
   * null if get all - pageSize : null if get all
   */
  @PostMapping("/search")
  public FastMap searchUsers(@RequestBody FastMap condition) {
    return userService.searchUsers(condition);
  }
}
