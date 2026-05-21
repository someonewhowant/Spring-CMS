package com.example.blog.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Long recipientId;
    private String content;
    private String timestamp;
    private boolean isRead;
}
