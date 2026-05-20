package com.example.blog.service;

import com.example.blog.entity.Notification;
import com.example.blog.entity.User;
import java.util.List;

public interface NotificationService {
    void createNotification(User user, String message, String link);
    List<Notification> getRecentNotifications(Long userId, int limit);
    List<Notification> getUnreadNotifications(Long userId);
    long getUnreadCount(Long userId);
    void markAsRead(Long notificationId);
    void markAllAsRead(Long userId);
    void clearAll(Long userId);
}
