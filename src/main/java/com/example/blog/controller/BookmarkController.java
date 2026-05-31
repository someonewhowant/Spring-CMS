package com.example.blog.controller;

import com.example.blog.entity.User;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/bookmark")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final UserRepository userRepository;

    @PostMapping("/post/{id}")
    public String togglePostBookmark(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        bookmarkService.togglePostBookmark(user.getId(), id);
        
        boolean isBookmarked = bookmarkService.isPostBookmarked(user.getId(), id);
        if (isBookmarked) {
            redirectAttributes.addFlashAttribute("success", "Article bookmarked successfully.");
        } else {
            redirectAttributes.addFlashAttribute("success", "Article removed from bookmarks.");
        }
        
        return "redirect:/post/" + id;
    }

    @PostMapping("/course/{id}")
    public String toggleCourseBookmark(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        bookmarkService.toggleCourseBookmark(user.getId(), id);
        
        boolean isBookmarked = bookmarkService.isCourseBookmarked(user.getId(), id);
        if (isBookmarked) {
            redirectAttributes.addFlashAttribute("success", "Course bookmarked successfully.");
        } else {
            redirectAttributes.addFlashAttribute("success", "Course removed from bookmarks.");
        }
        
        return "redirect:/course/" + id;
    }
}
