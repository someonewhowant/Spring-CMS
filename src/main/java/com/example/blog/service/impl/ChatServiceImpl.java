package com.example.blog.service.impl;

import com.example.blog.dto.ChatMessageDto;
import com.example.blog.entity.ChatMessage;
import com.example.blog.entity.User;
import com.example.blog.repository.ChatMessageRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.ChatService;
import com.example.blog.service.GamificationService;
import com.example.blog.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final GamificationService gamificationService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatServiceImpl.class);

    @Override
    @Transactional
    public ChatMessageDto sendMessage(User sender, Long recipientId, String content) {
        log.info("Sending message from {} to {}", sender.getUsername(), recipientId);
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .content(content)
                .timestamp(java.time.Instant.now())
                .isRead(false)
                .build();

        chatMessageRepository.save(message);

        // Evaluate teacher achievements for feedback
        if (sender.getRole() == com.example.blog.entity.Role.TEACHER) {
            gamificationService.evaluateAchievements(sender.getId());
        }

        ChatMessageDto dto = convertToDto(message);

        log.info("Pushing message to recipient: {} and sender: {}", recipient.getUsername(), sender.getUsername());
        try {
            messagingTemplate.convertAndSendToUser(
                    recipient.getUsername(),
                    "/queue/messages",
                    dto
            );
            
            messagingTemplate.convertAndSendToUser(
                    sender.getUsername(),
                    "/queue/messages",
                    dto
            );
        } catch (Exception e) {
            log.error("Failed to push message via WebSocket", e);
        }

        notificationService.createNotification(
                recipient,
                "New message from " + sender.getFullName(),
                "/chat?with=" + sender.getId()
        );

        return dto;
    }

    @Override
    public List<ChatMessageDto> getMessagesBetween(User user1, Long user2Id) {
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return chatMessageRepository.findMessagesBetween(user1, user2).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> getConversationsForUser(User user) {
        List<Long> partnerIds = chatMessageRepository.findConversationPartnerIds(user.getId());
        return userRepository.findAllById(partnerIds);
    }

    @Override
    public long getUnreadCount(User recipient, User sender) {
        return chatMessageRepository.countByRecipientAndSenderAndIsReadFalse(recipient, sender);
    }

    @Override
    @Transactional
    public void markAsRead(User recipient, Long senderId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        
        List<ChatMessage> unreadMessages = chatMessageRepository.findByRecipientAndSenderAndIsReadFalse(recipient, sender);
        unreadMessages.forEach(m -> m.setRead(true));
        chatMessageRepository.saveAll(unreadMessages);
    }

    @Override
    @Transactional
    public void clearHistory(User user1, Long user2Id) {
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        chatMessageRepository.deleteMessagesBetween(user1, user2);
    }

    private ChatMessageDto convertToDto(ChatMessage message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .senderAvatar(message.getSender().getAvatarUrl())
                .recipientId(message.getRecipient().getId())
                .content(message.getContent())
                .timestamp(String.valueOf(message.getTimestamp().toEpochMilli()))
                .isRead(message.isRead())
                .build();
    }
}
