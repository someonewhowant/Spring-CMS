package com.example.blog.controller;

import com.example.blog.entity.Assignment;
import com.example.blog.entity.AssignmentSubmission;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.PeerReview;
import com.example.blog.entity.User;
import com.example.blog.repository.CourseModuleRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.AssignmentService;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final CourseModuleRepository moduleRepository;
    private final UserRepository userRepository;

    // --- Teacher endpoints ---

    @GetMapping("/teacher/modules/{moduleId}/assignments/new")
    public String showCreateAssignmentForm(@PathVariable Long moduleId, Model model, Principal principal) {
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid module Id:" + moduleId));
        model.addAttribute("module", module);
        model.addAttribute("assignment", new Assignment());
        
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        model.addAttribute("currentUser", user != null ? user.getUsername() : "");
        model.addAttribute("currentUserRole", user != null ? "ROLE_" + user.getRole().name() : "");
        return "teacher/assignment_form";
    }

    @PostMapping("/teacher/modules/{moduleId}/assignments/new")
    public String createAssignment(
            @PathVariable Long moduleId,
            @ModelAttribute Assignment assignment,
            Principal principal,
            RedirectAttributes redirectAttributes) {
            
        // Optional: Ensure only teachers can create (can be done via SecurityConfig)
        
        if (assignment.getDeadline() == null) {
            // Default deadline to 14 days from now if not set
            assignment.setDeadline(LocalDateTime.now().plusDays(14));
        }
        
        assignmentService.createAssignment(moduleId, assignment);
        redirectAttributes.addFlashAttribute("message", "Assignment created successfully!");
        return "redirect:/teacher/courses/" + moduleRepository.findById(moduleId).get().getCourse().getId();
    }

    // --- Student endpoints ---

    @GetMapping("/student/assignments/{assignmentId}")
    public String viewAssignment(@PathVariable Long assignmentId, Model model, Principal principal) {
        Assignment assignment = assignmentService.getAssignment(assignmentId);
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        
        if (user == null) {
            return "redirect:/admin/login";
        }
        
        AssignmentSubmission mySubmission = assignmentService.getSubmissionByUserAndAssignment(user.getId(), assignmentId);
        List<PeerReview> reviews = null;
        if (mySubmission != null) {
            reviews = assignmentService.getPeerReviewsForSubmission(mySubmission.getId());
        }
        
        model.addAttribute("assignment", assignment);
        model.addAttribute("mySubmission", mySubmission);
        model.addAttribute("reviews", reviews);
        model.addAttribute("currentUser", user.getUsername());
        model.addAttribute("currentUserRole", "ROLE_" + user.getRole().name());
        
        return "student/assignment_detail";
    }

    @PostMapping("/student/assignments/{assignmentId}/submit")
    public String submitAssignment(
            @PathVariable Long assignmentId,
            @RequestParam String submissionText,
            @RequestParam(required = false) String fileUrl,
            Principal principal,
            RedirectAttributes redirectAttributes) {
            
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/admin/login";
        
        assignmentService.submitAssignment(assignmentId, user.getId(), submissionText, fileUrl);
        redirectAttributes.addFlashAttribute("message", "Assignment submitted successfully! Waiting for peer review.");
        return "redirect:/student/assignments/" + assignmentId;
    }

    // --- Peer Review endpoints ---

    @GetMapping("/student/peer-reviews")
    public String peerReviewQueue(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/admin/login";
        
        List<AssignmentSubmission> submissionsToReview = assignmentService.getSubmissionsNeedingReview(user.getId());
        
        model.addAttribute("submissions", submissionsToReview);
        model.addAttribute("currentUser", user.getUsername());
        model.addAttribute("currentUserRole", "ROLE_" + user.getRole().name());
        
        return "student/peer_review_queue";
    }

    @GetMapping("/student/peer-reviews/{submissionId}")
    public String viewSubmissionForReview(@PathVariable Long submissionId, Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/admin/login";
        
        AssignmentSubmission submission = assignmentService.getSubmission(submissionId);
        
        // Ensure user hasn't already reviewed it and it's not their own
        if (submission.getUser().getId().equals(user.getId())) {
            return "redirect:/student/peer-reviews";
        }
        
        model.addAttribute("submission", submission);
        model.addAttribute("currentUser", user.getUsername());
        model.addAttribute("currentUserRole", "ROLE_" + user.getRole().name());
        
        return "student/peer_review_form";
    }

    @PostMapping("/student/peer-reviews/{submissionId}")
    public String submitPeerReview(
            @PathVariable Long submissionId,
            @RequestParam int score,
            @RequestParam String feedbackComment,
            Principal principal,
            RedirectAttributes redirectAttributes) {
            
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/admin/login";
        
        try {
            assignmentService.addPeerReview(submissionId, user.getId(), score, feedbackComment);
            redirectAttributes.addFlashAttribute("message", "Peer review submitted successfully! You earned some XP.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/student/peer-reviews";
    }
}
