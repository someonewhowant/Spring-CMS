package com.example.blog.controller;

import com.example.blog.entity.User;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.security.Principal;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @PostMapping("/mark-all-read")
    public String markAllAsRead(Principal principal, @RequestHeader(value = "Referer", required = false) String referer) {
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> 
                notificationService.markAllAsRead(user.getId())
            );
        }
        // Redirect back to the page user was on, or to dashboard as fallback
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }

    @PostMapping("/clear-all")
    public String clearAll(Principal principal, @RequestHeader(value = "Referer", required = false) String referer) {
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(user -> 
                notificationService.clearAll(user.getId())
            );
        }
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }
}
