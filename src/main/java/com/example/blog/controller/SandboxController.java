package com.example.blog.controller;

import com.example.blog.entity.CodingSubmission;
import com.example.blog.entity.CodingTask;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.User;
import com.example.blog.repository.CourseModuleRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.CodingTaskService;
import com.example.blog.entity.Course;
import com.example.blog.repository.CourseRepository;
import com.example.blog.repository.CodingTaskRepository;
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
    private final CourseRepository courseRepository;
    private final CodingTaskRepository codingTaskRepository;


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
        model.addAttribute("courseId", task.getModule().getCourse().getId());
        model.addAttribute("moduleId", task.getModule().getId());
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

        CourseModule module = moduleRepository.findById(moduleId).orElse(null);
        if (module == null) {
            redirectAttributes.addFlashAttribute("error", "Failed to create coding task: Module not found.");
            return "redirect:/teacher/dashboard";
        }
        codingTaskService.createTask(moduleId, codingTask);
        redirectAttributes.addFlashAttribute("message", "Coding task created successfully!");
        return "redirect:/teacher/coding-tasks";
    }

    @PostMapping("/teacher/modules/{moduleId}/coding-tasks/import")
    public String importCodingTask(
            @PathVariable Long moduleId,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
            RedirectAttributes redirectAttributes) {

        CourseModule module = moduleRepository.findById(moduleId).orElse(null);
        if (module == null) {
            redirectAttributes.addFlashAttribute("error", "Failed to import coding task: Module not found.");
            return "redirect:/teacher/dashboard";
        }

        try {
            if (file != null && !file.isEmpty()) {
                String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                codingTaskService.importTaskFromMarkdown(moduleId, content);
                redirectAttributes.addFlashAttribute("message", "Coding task imported successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to import coding task: No file was provided.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to import coding task: " + e.getMessage());
        }

        return "redirect:/teacher/coding-tasks";
    }

    @GetMapping("/teacher/coding-tasks/{taskId}/edit")
    public String showEditCodingTaskForm(@PathVariable Long taskId, Model model, Principal principal) {
        CodingTask task = codingTaskService.getTask(taskId);
        model.addAttribute("module", task.getModule());
        model.addAttribute("codingTask", task);

        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        model.addAttribute("currentUser", user != null ? user.getUsername() : "");
        model.addAttribute("currentUserRole", user != null ? "ROLE_" + user.getRole().name() : "");
        model.addAttribute("title", "Edit Coding Task: " + task.getTitle());

        return "teacher/coding_task_form";
    }

    @PostMapping("/teacher/coding-tasks/{taskId}/edit")
    public String editCodingTask(
            @PathVariable Long taskId,
            @ModelAttribute CodingTask codingTask,
            RedirectAttributes redirectAttributes) {
        CodingTask updated = codingTaskService.updateTask(taskId, codingTask);
        redirectAttributes.addFlashAttribute("message", "Coding task updated successfully!");
        return "redirect:/teacher/coding-tasks";
    }

    @GetMapping("/teacher/coding-tasks/{taskId}/delete")
    public String deleteCodingTask(@PathVariable Long taskId, RedirectAttributes redirectAttributes) {
        CodingTask task = codingTaskService.getTask(taskId);
        Long courseId = task.getModule().getCourse().getId();
        codingTaskService.deleteTask(taskId);
        redirectAttributes.addFlashAttribute("message", "Coding task deleted.");
        return "redirect:/teacher/coding-tasks";
    }

    @GetMapping("/teacher/coding-tasks")
    public String listCodingTasks(Model model, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        model.addAttribute("currentUser", user != null ? user.getUsername() : "");
        model.addAttribute("currentUserRole", user != null ? "ROLE_" + user.getRole().name() : "");
        
        List<Course> courses = courseRepository.findAll();
        List<CodingTask> codingTasks = codingTaskRepository.findAll();
        
        model.addAttribute("courses", courses);
        model.addAttribute("codingTasks", codingTasks);
        model.addAttribute("title", "Manage Coding Tasks");
        
        return "teacher/coding_tasks";
    }
}

