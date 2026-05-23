package com.example.blog.service;

import com.example.blog.dto.ChatMessageDto;
import com.example.blog.entity.User;
import java.util.List;

public interface ChatService {
    ChatMessageDto sendMessage(User sender, Long recipientId, String content);
    List<ChatMessageDto> getMessagesBetween(User user1, Long user2Id);
    List<User> getConversationsForUser(User user);
    long getUnreadCount(User recipient, User sender);
    void markAsRead(User recipient, Long senderId);
    void clearHistory(User user1, Long user2Id);
}
