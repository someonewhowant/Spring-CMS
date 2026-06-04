package com.example.blog.service.impl;

import com.example.blog.entity.CodingSubmission;
import com.example.blog.entity.CodingTask;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.User;
import com.example.blog.repository.CodingSubmissionRepository;
import com.example.blog.repository.CodingTaskRepository;
import com.example.blog.repository.CourseModuleRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.CodingTaskService;
import com.example.blog.service.GamificationService;
import com.example.blog.service.SandboxExecutionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CodingTaskServiceImpl implements CodingTaskService {

    private final CodingTaskRepository taskRepository;
    private final CodingSubmissionRepository submissionRepository;
    private final CourseModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final SandboxExecutionService sandboxExecutionService;
    private final GamificationService gamificationService;
    private final ObjectMapper objectMapper;

    @Override
    public CodingTask createTask(Long moduleId, CodingTask task) {
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));
        task.setModule(module);
        return taskRepository.save(task);
    }

    @Override
    @Transactional(readOnly = true)
    public CodingTask getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CodingTask not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CodingTask> getTasksByModule(Long moduleId) {
        return taskRepository.findByModuleId(moduleId);
    }

    @Override
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> runCode(Long taskId, String code, String language) {
        CodingTask task = getTask(taskId);
        List<Map<String, String>> testCases = parseTestCases(task.getTestCasesJson());

        List<Map<String, Object>> testResults = new ArrayList<>();
        boolean allPassed = true;
        StringBuilder fullOutput = new StringBuilder();

        for (int i = 0; i < testCases.size(); i++) {
            Map<String, String> tc = testCases.get(i);
            String input = tc.getOrDefault("input", "");
            String expectedOutput = tc.getOrDefault("expectedOutput", "").trim();
            String label = tc.getOrDefault("label", "Test #" + (i + 1));

            Map<String, Object> execResult = sandboxExecutionService.executeCode(language, code, input);

            String actualOutput = ((String) execResult.getOrDefault("stdout", "")).trim();
            String stderr = (String) execResult.getOrDefault("stderr", "");
            boolean passed = actualOutput.equals(expectedOutput);

            if (!passed) {
                allPassed = false;
            }

            Map<String, Object> testResult = new HashMap<>();
            testResult.put("label", label);
            testResult.put("passed", passed);
            testResult.put("expected", expectedOutput);
            testResult.put("actual", actualOutput);
            testResult.put("input", input);
            if (stderr != null && !stderr.isBlank()) {
                testResult.put("stderr", stderr);
            }
            testResults.add(testResult);

            fullOutput.append("[").append(label).append("] ")
                    .append(passed ? "✓ PASSED" : "✗ FAILED")
                    .append("\n");
            if (!passed) {
                fullOutput.append("  Expected: ").append(expectedOutput).append("\n");
                fullOutput.append("  Got:      ").append(actualOutput).append("\n");
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("passed", allPassed);
        response.put("results", testResults);
        response.put("output", fullOutput.toString());
        response.put("totalTests", testCases.size());
        response.put("passedTests", testResults.stream().filter(r -> (boolean) r.get("passed")).count());
        return response;
    }

    @Override
    public CodingSubmission submitCode(Long taskId, Long userId, String code, String language) {
        CodingTask task = getTask(taskId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Map<String, Object> runResult = runCode(taskId, code, language);
        boolean allPassed = (boolean) runResult.get("passed");

        CodingSubmission.SubmissionStatus status = allPassed
                ? CodingSubmission.SubmissionStatus.PASSED
                : CodingSubmission.SubmissionStatus.FAILED;

        String resultJson;
        try {
            resultJson = objectMapper.writeValueAsString(runResult);
        } catch (Exception e) {
            resultJson = "{}";
        }

        CodingSubmission submission = CodingSubmission.builder()
                .user(user)
                .codingTask(task)
                .codeSubmitted(code)
                .status(status)
                .resultJson(resultJson)
                .build();

        CodingSubmission saved = submissionRepository.save(submission);

        // Award XP on first successful pass
        if (allPassed && !hasUserPassedTask(taskId, userId)) {
            gamificationService.awardXp(userId, task.getPointsReward(), "Coding task passed: " + task.getTitle());
            log.info("Awarded {} XP to user {} for passing coding task: {}", task.getPointsReward(), userId, task.getTitle());
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserPassedTask(Long taskId, Long userId) {
        return submissionRepository.countByCodingTaskIdAndUserIdAndStatus(
                taskId, userId, CodingSubmission.SubmissionStatus.PASSED) > 0;
    }

    private List<Map<String, String>> parseTestCases(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse test cases JSON: {}", e.getMessage());
            return List.of();
        }
    }
}
