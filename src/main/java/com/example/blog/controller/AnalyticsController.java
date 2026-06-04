package com.example.blog.controller;

import com.example.blog.entity.Role;
import com.example.blog.entity.User;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.AnalyticsService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    // --- Student analytics page ---

    @GetMapping("/student/analytics")
    public String studentAnalyticsPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("currentUser", user.getUsername());
        model.addAttribute("currentUserRole", "ROLE_" + user.getRole().name());
        model.addAttribute("title", "Performance Analytics");
        return "student/analytics";
    }

    // --- Student REST endpoints for Chart.js ---

    @GetMapping("/student/api/analytics/xp-progression")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getXpProgression(
            Principal principal, @RequestParam(defaultValue = "30") int days) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(analyticsService.getXpProgressionData(user.getId(), days));
    }

    @GetMapping("/student/api/analytics/scores-by-course")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getScoresByCourse(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(analyticsService.getScoresByCourse(user.getId()));
    }

    @GetMapping("/student/api/analytics/activity")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getActivity(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(analyticsService.getActivityHeatmapData(user.getId()));
    }

    @GetMapping("/student/api/analytics/pass-fail")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPassFail(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(analyticsService.getPassFailSummary(user.getId()));
    }

    // --- Teacher analytics page ---

    @GetMapping("/teacher/analytics")
    public String teacherAnalyticsPage(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null || user.getRole() != Role.TEACHER) {
            return "redirect:/admin/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("currentUser", user.getUsername());
        model.addAttribute("currentUserRole", "ROLE_" + user.getRole().name());
        model.addAttribute("title", "Academy Analytics");
        return "teacher/analytics";
    }

    // --- Teacher REST endpoints for Chart.js ---

    @GetMapping("/teacher/api/analytics/level-distribution")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLevelDistribution(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null || user.getRole() != Role.TEACHER) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(analyticsService.getStudentLevelDistribution());
    }

    @GetMapping("/teacher/api/analytics/avg-scores")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAvgScores(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null || user.getRole() != Role.TEACHER) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(analyticsService.getAverageScoresByQuiz());
    }

    @GetMapping("/teacher/api/analytics/hardest-quizzes")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getHardestQuizzes(
            Principal principal, @RequestParam(defaultValue = "10") int limit) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null || user.getRole() != Role.TEACHER) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(analyticsService.getHardestQuizzes(limit));
    }
}
