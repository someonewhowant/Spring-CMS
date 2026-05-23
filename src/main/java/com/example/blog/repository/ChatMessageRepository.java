package com.example.blog.repository;

import com.example.blog.entity.ChatMessage;
import com.example.blog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE (m.sender = :user1 AND m.recipient = :user2) " +
           "OR (m.sender = :user2 AND m.recipient = :user1) ORDER BY m.timestamp ASC")
    List<ChatMessage> findMessagesBetween(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT DISTINCT CASE WHEN m.sender.id = :userId THEN m.recipient.id ELSE m.sender.id END " +
           "FROM ChatMessage m WHERE m.sender.id = :userId OR m.recipient.id = :userId")
    List<Long> findConversationPartnerIds(@Param("userId") Long userId);

    long countByRecipientAndSenderAndIsReadFalse(User recipient, User sender);

    List<ChatMessage> findByRecipientAndSenderAndIsReadFalse(User recipient, User sender);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ChatMessage m WHERE (m.sender = :u1 AND m.recipient = :u2) OR (m.sender = :u2 AND m.recipient = :u1)")
    void deleteMessagesBetween(@Param("u1") User u1, @Param("u2") User u2);
}
