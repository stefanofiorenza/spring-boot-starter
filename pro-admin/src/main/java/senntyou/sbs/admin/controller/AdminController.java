package senntyou.sbs.admin.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import senntyou.sbs.admin.dto.AdminLoginParam;
import senntyou.sbs.admin.dto.AdminUserParam;
import senntyou.sbs.admin.dto.UpdateAdminUserPasswordParam;
import senntyou.sbs.admin.service.AdminRoleService;
import senntyou.sbs.admin.service.AdminUserService;
import senntyou.sbs.common.CommonPage;
import senntyou.sbs.common.CommonResult;
import senntyou.sbs.mbg.model.AdminPermission;
import senntyou.sbs.mbg.model.AdminRole;
import senntyou.sbs.mbg.model.AdminUser;

/** 后台用户管理 */
@Controller
@Api(tags = "AdminController", description = "后台用户管理")
@RequestMapping("/admin")
public class AdminController {
  @Value("${jwt.tokenHeader}")
  private String tokenHeader;

  @Value("${jwt.tokenHead}")
  private String tokenHead;

  @Autowired private AdminUserService userService;
  @Autowired private AdminRoleService roleService;

  @ApiOperation(value = "用户注册")
  @RequestMapping(value = "/register", method = RequestMethod.POST)
  @ResponseBody
  public CommonResult<AdminUser> register(
      @RequestBody AdminUserParam adminUserParam, BindingResult result) {
    AdminUser adminUser = userService.register(adminUserParam);
    if (adminUser == null) {
      CommonResult.failed();
    }
    return CommonResult.success(adminUser);
  }

  @ApiOperation(value = "登录以后返回token")
  @RequestMapping(value = "/login", method = RequestMethod.POST)
  @ResponseBody
  public CommonResult login(@RequestBody AdminLoginParam adminLoginParam, BindingResult result) {
    String token = userService.login(adminLoginParam.getUsername(), adminLoginParam.getPassword());
    if (token == null) {
      return CommonResult.validateFailed("用户名或密码错误");
    }
    Map<String, String> tokenMap = new HashMap<>();
    tokenMap.put("token", token);
    tokenMap.put("tokenHead", tokenHead);
    return CommonResult.success(tokenMap);
  }

  @ApiOperation(value = "刷新token")
  @RequestMapping(value = "/refreshToken", method = RequestMethod.GET)
  @ResponseBody
  public CommonResult refreshToken(HttpServletRequest request) {
    String token = request.getHeader(tokenHeader);
    String refreshToken = userService.refreshToken(token);
    if (refreshToken == null) {
      return CommonResult.failed("token已经过期！");
    }
    Map<String, String> tokenMap = new HashMap<>();
    tokenMap.put("token", refreshToken);
    tokenMap.put("tokenHead", tokenHead);
    return CommonResult.success(tokenMap);
  }

  @ApiOperation(value = "获取当前登录用户信息")
  @RequestMapping(value = "/info", method = RequestMethod.GET)
  @ResponseBody
  public CommonResult getAdminInfo(Principal principal) {
    if (principal == null) {
      return CommonResult.unauthorized(null);
    }
    String username = principal.getName();
    AdminUser adminUser = userService.getUserByUsername(username);
    Map<String, Object> data = new HashMap<>();
    data.put("username", adminUser.getUsername());
    data.put("roles", new String[] {"NONE"});
    data.put("menus", roleService.getMenuList(adminUser.getId()));
    data.put("avatar", adminUser.getAvatar());
    return CommonResult.success(data);
  }

  @ApiOperation(value = "登出功能")
  @RequestMapping(value = "/logout", method = RequestMethod.POST)
  @ResponseBody
  public CommonResult logout() {
    return CommonResult.success(null);
  }

  @ApiOperation("根据用户名或姓名分页获取用户列表")
  @RequestMapping(value = "/list", method = RequestMethod.GET)
  @ResponseBody
  public CommonResult<CommonPage<AdminUser>> list(
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
      @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
    List<AdminUser> adminList = userService.list(keyword, pageSize, pageNum);
    return CommonResult.success(CommonPage.toPage(adminList));
  }

  @ApiOperation("获取指定用户信息")
  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  @ResponseBody
  public CommonResult<AdminUser> getItem(@PathVariable Long id) {
    AdminUser adminUser = userService.getItem(id);
    return CommonResult.success(adminUser);
  }

  @ApiOperation("修改指定用户信息")
  @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
  @ResponseBody
  public CommonResult update(@PathVariable Long id, @RequestBody AdminUser adminUser) {
    int count = userService.update(id, adminUser);
    if (count > 0) {
      return CommonResult.success(count);
    }
    return CommonResult.failed();
  }

  @ApiOperation("修改指定用户密码")
  @RequestMapping(value = "/updatePassword", method = RequestMethod.POST)
  @ResponseBody
  public CommonResult updatePassword(
      @RequestBody UpdateAdminUserPasswordParam updatePasswordParam) {
    int status = userService.updatePassword(updatePasswordParam);
    if (status > 0) {
      return CommonResult.success(status);
    } else if (status == -1) {
      return CommonResult.failed("提交参数不合法");
    } else if (status == -2) {
      return CommonResult.failed("找不到该用户");
    } else if (status == -3) {
      return CommonResult.failed("旧密码错误");
    } else {
      return CommonResult.failed();
    }
  }

  @ApiOperation("删除指定用户信息")
  @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
  @ResponseBody
  public CommonResult delete(@PathVariable Long id) {
    int count = userService.delete(id);
    if (count > 0) {
      return CommonResult.success(count);
    }
    return CommonResult.failed();
  }

  @ApiOperation("修改帐号状态")
  @RequestMapping(value = "/updateStatus/{id}", method = RequestMethod.POST)
  @ResponseBody
  public CommonResult updateStatus(
      @PathVariable Long id, @RequestParam(value = "status") Integer status) {
    AdminUser adminUser = new AdminUser();
    adminUser.setStatus(status);
    int count = userService.update(id, adminUser);
    if (count > 0) {
      return CommonResult.success(count);
    }
    return CommonResult.failed();
  }

  @ApiOperation("给用户分配角色")
  @RequestMapping(value = "/role/update", method = RequestMethod.POST)
  @ResponseBody
  public CommonResult updateRole(
      @RequestParam("userId") Long userId, @RequestParam("roleIds") List<Long> roleIds) {
    int count = userService.updateRole(userId, roleIds);
    if (count >= 0) {
      return CommonResult.success(count);
    }
    return CommonResult.failed();
  }

  @ApiOperation("获取指定用户的角色")
  @RequestMapping(value = "/role/{userId}", method = RequestMethod.GET)
  @ResponseBody
  public CommonResult<List<AdminRole>> getRoleList(@PathVariable Long userId) {
    List<AdminRole> roleList = userService.getRoleList(userId);
    return CommonResult.success(roleList);
  }

  @ApiOperation("给用户分配+-权限")
  @RequestMapping(value = "/permission/update", method = RequestMethod.POST)
  @ResponseBody
  public CommonResult updatePermission(
      @RequestParam Long userId, @RequestParam("permissionIds") List<Long> permissionIds) {
    int count = userService.updatePermission(userId, permissionIds);
    if (count > 0) {
      return CommonResult.success(count);
    }
    return CommonResult.failed();
  }

  @ApiOperation("获取用户所有权限（包括+-权限）")
  @RequestMapping(value = "/permission/{userId}", method = RequestMethod.GET)
  @ResponseBody
  public CommonResult<List<AdminPermission>> getPermissionList(@PathVariable Long userId) {
    List<AdminPermission> permissionList = userService.getPermissionList(userId);
    return CommonResult.success(permissionList);
  }
}
