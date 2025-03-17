package com.dpvn.crm.user;

import com.dpvn.crmcrudservice.domain.dto.UserDto;
import com.dpvn.shared.domain.dto.PagingResponse;
import com.dpvn.shared.util.FastMap;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/find-by-options")
  public List<UserDto> findUsersByOptions(@RequestBody UserDto userDto) {
    List<UserDto> userDtos = userService.findUsersByOptions(userDto);
    userDtos.forEach(u -> u.setPassword(null));
    return userDtos;
  }

  /**
   * - filterText
   * - status: Boolean
   * - departments: List<Long>
   * - roles: List<Long>
   * - page
   * - pageSize
   */
  @PostMapping("/search")
  public FastMap searchUsers(@RequestBody FastMap condition) {
    FastMap result = userService.searchUsers(condition);
    List<FastMap> rows = result.getListClass("rows", FastMap.class);
    rows.forEach(u -> u.remove("password"));
    result.add("rows", rows);
    return result;
  }

  @GetMapping
  public PagingResponse<UserDto> listAllUsers() {
    PagingResponse<UserDto> users = userService.listAllUsers();
    users.getRows().forEach(u -> u.setPassword(null));
    return users;
  }

  @GetMapping("/{id}")
  public UserDto getUserById(@PathVariable Long id) {
    UserDto userDto = userService.findById(id);
    userDto.setPassword(null);
    return userDto;
  }

  @PostMapping
  public void createNewUser(@RequestBody UserDto userDto) {
    userService.createNewUser(userDto);
  }

  @PostMapping("/{id}")
  public void updateExistedUser(@PathVariable Long id, @RequestBody FastMap userDto) {
    userService.updateUser(id, userDto);
  }
}
