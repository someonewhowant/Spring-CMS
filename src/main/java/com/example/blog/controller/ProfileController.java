package com.example.blog.controller;

import com.example.blog.entity.User;
import com.example.blog.repository.UserRepository;
import com.example.blog.repository.CourseRepository;
import com.example.blog.repository.QuizRepository;
import com.example.blog.repository.PostRepository;
import com.example.blog.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final PostRepository postRepository;
    private final FileStorageService fileStorageService;
    private final com.example.blog.service.GamificationService gamificationService;

    @GetMapping("/u/{username}")
    public String publicProfile(@PathVariable String username, Model model) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return "redirect:/leaderboard";
        }
        User user = userOpt.get();
        model.addAttribute("profileUser", user);
        
        // Calculate XP progress for premium UI
        int currentLevel = user.getLevel() != null ? user.getLevel() : 1;
        int requiredXp = Math.max(100, currentLevel * 100);
        int progressPct = (user.getExperiencePoints() * 100) / requiredXp;
        
        model.addAttribute("requiredXp", requiredXp);
        model.addAttribute("xpProgressPct", progressPct);
        
        return "profile/public";
    }

    @GetMapping("/settings")
    public String profileSettings(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        Optional<User> userOpt = userRepository.findByUsername(principal.getName());
        if (userOpt.isEmpty()) {
            return "redirect:/admin/login";
        }
        User user = userOpt.get();
        model.addAttribute("user", user);
        
        // Add academy stats to make settings page look premium and informative!
        model.addAttribute("totalCourses", courseRepository.count());
        model.addAttribute("totalQuizzes", quizRepository.count());
        model.addAttribute("totalPosts", postRepository.count());
        
        return "profile/settings";
    }

    @PostMapping("/settings/update")
    public String updateProfile(Principal principal,
                                @RequestParam String username,
                                @RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String title,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) String githubUrl,
                                @RequestParam(required = false) String linkedinUrl,
                                @RequestParam(required = false) MultipartFile avatarFile,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        Optional<User> userOpt = userRepository.findByUsername(principal.getName());
        if (userOpt.isEmpty()) {
            return "redirect:/admin/login";
        }
        User user = userOpt.get();

        boolean usernameChanged = !user.getUsername().equals(username);
        
        // If username changed, verify uniqueness
        if (usernameChanged) {
            if (userRepository.findByUsername(username).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Username already exists");
                return "redirect:/profile/settings";
            }
        }

        // Handle password change if requested
        if (currentPassword != null && !currentPassword.isEmpty()) {
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Incorrect current password");
                return "redirect:/profile/settings";
            }
            if (newPassword == null || newPassword.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "New password cannot be empty");
                return "redirect:/profile/settings";
            }
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Passwords do not match");
                return "redirect:/profile/settings";
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        // Set LMS personal metadata
        user.setFullName(fullName);
        user.setEmail(email);
        user.setTitle(title);
        user.setBio(bio);
        user.setGithubUrl(githubUrl);
        user.setLinkedinUrl(linkedinUrl);

        // Store avatar file if present
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String uploadedUrl = fileStorageService.storeFile(avatarFile);
                if (uploadedUrl != null) {
                    user.setAvatarUrl(uploadedUrl);
                }
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Failed to store avatar: " + e.getMessage());
                return "redirect:/profile/settings";
            }
        }

        if (usernameChanged) {
            user.setUsername(username);
        }

        userRepository.save(user);

        if (usernameChanged) {
            // Warn them that they need to log in again since username changed
            redirectAttributes.addFlashAttribute("registered", "Username changed successfully! Please authenticate again with your new username.");
            return "redirect:/admin/login?registered";
        }

        redirectAttributes.addFlashAttribute("success", "Profile settings updated successfully!");
        return "redirect:/profile/settings";
    }

    @PostMapping("/delete")
    @org.springframework.transaction.annotation.Transactional
    public String deleteProfile(Principal principal, jakarta.servlet.http.HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        Optional<User> userOpt = userRepository.findByUsername(principal.getName());
        if (userOpt.isEmpty()) {
            return "redirect:/admin/login";
        }
        User user = userOpt.get();
        
        gamificationService.deleteUser(user.getId());
        
        try {
            request.logout();
        } catch (jakarta.servlet.ServletException e) {
            // ignore
        }
        
        redirectAttributes.addFlashAttribute("registered", "Ваш аккаунт был успешно удален.");
        return "redirect:/admin/login?registered";
    }
}
