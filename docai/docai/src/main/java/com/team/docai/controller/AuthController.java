package com.team.docai.controller;

import com.team.docai.common.Result;
import com.team.docai.entity.User;
import com.team.docai.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器 - 用户注册/登录
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            String nickname = body.get("nickname");

            if (username == null || username.isBlank()) {
                return Result.error("用户名不能为空");
            }
            if (password == null || password.length() < 6) {
                return Result.error("密码长度不能少于6位");
            }
            if (username.length() < 3 || username.length() > 20) {
                return Result.error("用户名长度需为3-20位");
            }

            User user = userService.register(username, password, nickname);
            return Result.success(user);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("注册失败", e);
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");

            if (username == null || username.isBlank()) {
                return Result.error("用户名不能为空");
            }
            if (password == null || password.isBlank()) {
                return Result.error("密码不能为空");
            }

            Map<String, Object> loginResult = userService.login(username, password);
            return Result.success(loginResult);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<?> getCurrentUser(@RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.error("未登录");
        }
        User user = userService.getUserById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }
}
