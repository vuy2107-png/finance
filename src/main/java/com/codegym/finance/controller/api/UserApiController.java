package com.codegym.finance.controller.api;
import com.codegym.finance.entity.user.User;

import com.codegym.finance.service.user.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserApiController {

    @Autowired
    private IUserService userService;

    @PostMapping("/activate-trial")
    public ResponseEntity<?> activateTrial(Authentication auth) {
        try {
            userService.activateTrial(auth.getName());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Đã kích hoạt 7 ngày dùng thử Premium!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/mark-tour-seen")
    public ResponseEntity<?> markTourSeen(Authentication auth) {
        try {
            userService.markTourAsSeen(auth.getName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
