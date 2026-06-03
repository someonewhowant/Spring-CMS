package com.example.blog.controller;

import com.example.blog.dto.UserProfileUpdateRequest;
import com.example.blog.entity.Bookmark;
import com.example.blog.entity.User;
import com.example.blog.repository.CourseRepository;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.QuizRepository;
import com.example.blog.service.BookmarkService;
import com.example.blog.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final PostRepository postRepository;
    private final BookmarkService bookmarkService;

    @GetMapping("/u/{username}")
    public String publicProfile(@PathVariable String username, Model model) {
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/leaderboard";
        }
        model.addAttribute("profileUser", user);

        // Calculate XP progress for premium UI
        int currentLevel = user.getLevel() != null ? user.getLevel() : 1;
        int requiredXp = Math.max(100, currentLevel * 100);
        int progressPct = (user.getExperiencePoints() * 100) / requiredXp;

        model.addAttribute("requiredXp", requiredXp);
        model.addAttribute("xpProgressPct", progressPct);

        return "profile/public";
    }

    @GetMapping("/bookmarks")
    public String profileBookmarks(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("user", user);

        List<Bookmark> bookmarks = bookmarkService.getUserBookmarks(user.getId());
        model.addAttribute("bookmarks", bookmarks);

        List<Bookmark> articleBookmarks =
                bookmarks.stream().filter(b -> b.getPost() != null).collect(Collectors.toList());
        List<Bookmark> courseBookmarks =
                bookmarks.stream().filter(b -> b.getCourse() != null).collect(Collectors.toList());
        model.addAttribute("articleBookmarks", articleBookmarks);
        model.addAttribute("courseBookmarks", courseBookmarks);

        return "profile/bookmarks";
    }

    @GetMapping("/settings")
    public String profileSettings(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("user", user);

        // Add academy stats to make settings page look premium and informative!
        model.addAttribute("totalCourses", courseRepository.count());
        model.addAttribute("totalQuizzes", quizRepository.count());
        model.addAttribute("totalPosts", postRepository.count());

        return "profile/settings";
    }

    @PostMapping("/settings/update")
    public String updateProfile(
            Principal principal,
            @Valid @ModelAttribute("profileUpdateRequest") UserProfileUpdateRequest requestDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/admin/login";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/profile/settings";
        }

        boolean usernameChanged = !user.getUsername().equals(requestDto.getUsername());

        try {
            userService.updateProfile(user.getId(), requestDto);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile/settings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error", "Failed to update profile: " + e.getMessage());
            return "redirect:/profile/settings";
        }

        if (usernameChanged) {
            // Warn them that they need to log in again since username changed
            redirectAttributes.addFlashAttribute(
                    "registered",
                    "Username changed successfully! Please authenticate again with your new username.");
            return "redirect:/admin/login?registered";
        }

        redirectAttributes.addFlashAttribute("success", "Profile settings updated successfully!");
        return "redirect:/profile/settings";
    }

    @PostMapping("/delete")
    @org.springframework.transaction.annotation.Transactional
    public String deleteProfile(
            Principal principal,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/admin/login";
        }

        userService.deleteUser(user.getId());

        try {
            request.logout();
        } catch (ServletException e) {
            // ignore
        }

        redirectAttributes.addFlashAttribute("registered", "Ваш аккаунт был успешно удален.");
        return "redirect:/admin/login?registered";
    }
}
