package com.example.blog.controller;

import com.example.blog.dto.ChatMessageDto;
import com.example.blog.entity.Role;
import com.example.blog.entity.User;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final com.example.blog.repository.UserQuizResultRepository userQuizResultRepository;

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName()).orElseThrow();
    }

    @GetMapping("/chat")
    public String chatPage(@RequestParam(value = "with", required = false) Long withUserId,
                           Authentication authentication, Model model) {
        User currentUser = getCurrentUser(authentication);
        
        List<User> chatPartners;
        if (currentUser.getRole() == Role.STUDENT) {
            // Students can chat with teachers
            chatPartners = userRepository.findByRole(Role.TEACHER);
        } else {
            // Teachers can chat with students they have messaged or who are enrolled
            List<User> allStudents = userRepository.findByRole(Role.STUDENT);
            List<User> withDialogues = chatService.getConversationsForUser(currentUser).stream()
                    .filter(u -> u.getRole() == Role.STUDENT)
                    .collect(Collectors.toList());
            
            chatPartners = allStudents.stream()
                    .filter(s -> withDialogues.contains(s) || 
                                 s.getLastOpenedCourseId() != null || 
                                 !userQuizResultRepository.findByUserId(s.getId()).isEmpty())
                    .collect(Collectors.toList());
        }

        model.addAttribute("chatPartners", chatPartners);
        model.addAttribute("activePartnerId", withUserId);
        
        // Add unread counts for each partner
        Map<Long, Long> unreadCounts = new HashMap<>();
        for (User partner : chatPartners) {
            unreadCounts.put(partner.getId(), chatService.getUnreadCount(currentUser, partner));
        }
        model.addAttribute("unreadCounts", unreadCounts);

        return "chat";
    }

    @GetMapping("/api/chat/messages/{partnerId}")
    @ResponseBody
    public List<ChatMessageDto> getMessages(@PathVariable Long partnerId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        chatService.markAsRead(currentUser, partnerId);
        return chatService.getMessagesBetween(currentUser, partnerId);
    }

    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<?> sendMessage(@RequestParam Long recipientId, @RequestParam String content, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        chatService.sendMessage(currentUser, recipientId, content);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/chat/clear")
    @ResponseBody
    public ResponseEntity<?> clearHistory(@RequestParam Long partnerId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        chatService.clearHistory(currentUser, partnerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/chat/unread-counts")
    @ResponseBody
    public Map<Long, Long> getUnreadCounts(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        List<User> conversations = chatService.getConversationsForUser(currentUser);
        Map<Long, Long> counts = new HashMap<>();
        for (User partner : conversations) {
            counts.put(partner.getId(), chatService.getUnreadCount(currentUser, partner));
        }
        return counts;
    }
}
