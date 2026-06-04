package com.example.blog.controller;

import com.example.blog.entity.CodingSubmission;
import com.example.blog.entity.CodingTask;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.User;
import com.example.blog.repository.CourseModuleRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.CodingTaskService;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SandboxController {

    private final CodingTaskService codingTaskService;
    private final CourseModuleRepository moduleRepository;
    private final UserRepository userRepository;

    // ──────────────────────────────────────────────
    // REST API endpoints for code execution
    // ──────────────────────────────────────────────

    /** Runs user code against test cases without saving (try mode). */
    @PostMapping("/api/sandbox/run")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> runCode(@RequestBody Map<String, Object> request) {
        Long taskId = Long.valueOf(request.get("taskId").toString());
        String code = (String) request.get("code");
        String language = (String) request.get("language");

        Map<String, Object> result = codingTaskService.runCode(taskId, code, language);
        return ResponseEntity.ok(result);
    }

    /** Submits code for final grading and XP award. */
    @PostMapping("/api/sandbox/submit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitCode(
            @RequestBody Map<String, Object> request,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Long taskId = Long.valueOf(request.get("taskId").toString());
        String code = (String) request.get("code");
        String language = (String) request.get("language");

        CodingSubmission submission = codingTaskService.submitCode(taskId, user.getId(), code, language);

        Map<String, Object> response = new HashMap<>();
        response.put("status", submission.getStatus().name());
        response.put("passed", submission.getStatus() == CodingSubmission.SubmissionStatus.PASSED);

        if (submission.getStatus() == CodingSubmission.SubmissionStatus.PASSED) {
            CodingTask task = codingTaskService.getTask(taskId);
            response.put("xpAwarded", task.getPointsReward());
        }

        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────
    // Student: view coding task page
    // ──────────────────────────────────────────────

    @GetMapping("/student/sandbox/{taskId}")
    public String viewCodingTask(@PathVariable Long taskId, Model model, Principal principal) {
        CodingTask task = codingTaskService.getTask(taskId);
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean alreadyPassed = codingTaskService.hasUserPassedTask(taskId, user.getId());

        model.addAttribute("task", task);
        model.addAttribute("alreadyPassed", alreadyPassed);
        model.addAttribute("currentUser", user.getUsername());
        model.addAttribute("currentUserRole", "ROLE_" + user.getRole().name());
        model.addAttribute("title", "Sandbox: " + task.getTitle());

        return "student/coding_sandbox";
    }

    // ──────────────────────────────────────────────
    // Teacher: create coding task
    // ──────────────────────────────────────────────

    @GetMapping("/teacher/modules/{moduleId}/coding-tasks/new")
    public String showCreateCodingTaskForm(@PathVariable Long moduleId, Model model, Principal principal) {
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid module Id:" + moduleId));
        model.addAttribute("module", module);
        model.addAttribute("codingTask", new CodingTask());

        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        model.addAttribute("currentUser", user != null ? user.getUsername() : "");
        model.addAttribute("currentUserRole", user != null ? "ROLE_" + user.getRole().name() : "");
        model.addAttribute("title", "Create Coding Task");

        return "teacher/coding_task_form";
    }

    @PostMapping("/teacher/modules/{moduleId}/coding-tasks/new")
    public String createCodingTask(
            @PathVariable Long moduleId,
            @ModelAttribute CodingTask codingTask,
            RedirectAttributes redirectAttributes) {

        codingTaskService.createTask(moduleId, codingTask);
        redirectAttributes.addFlashAttribute("message", "Coding task created successfully!");
        return "redirect:/teacher/courses/" + moduleRepository.findById(moduleId).get().getCourse().getId() + "/modules";
    }

    @GetMapping("/teacher/coding-tasks/{taskId}/delete")
    public String deleteCodingTask(@PathVariable Long taskId, RedirectAttributes redirectAttributes) {
        CodingTask task = codingTaskService.getTask(taskId);
        Long courseId = task.getModule().getCourse().getId();
        codingTaskService.deleteTask(taskId);
        redirectAttributes.addFlashAttribute("message", "Coding task deleted.");
        return "redirect:/teacher/courses/" + courseId + "/modules";
    }
}
