package com.example.blog.controller;

import com.example.blog.entity.User;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.PostService;
import com.example.blog.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final PostService postService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @ModelAttribute("currentUserObj")
    public User currentUserObj() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    @ModelAttribute("unreadNotificationsCount")
    public long unreadNotificationsCount() {
        User user = currentUserObj();
        if (user != null) {
            return notificationService.getUnreadCount(user.getId());
        }
        return 0;
    }

    @ModelAttribute("recentNotifications")
    public Object recentNotifications() {
        User user = currentUserObj();
        if (user != null) {
            return notificationService.getRecentNotifications(user.getId(), 5);
        }
        return null;
    }

    @ModelAttribute("categories")
    public Object categories() {
        return postService.getAllCategories();
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("currentUser")
    public String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return auth.getName();
        }
        return null;
    }

    @ModelAttribute("currentUserRole")
    public String currentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
