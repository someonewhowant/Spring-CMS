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
        if (testCases.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("passed", false);
            response.put("results", testResults);
            response.put("output", "Error: No test cases configured for this task. Please contact the instructor.");
            response.put("totalTests", 0);
            response.put("passedTests", 0);
            return response;
        }

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
            int exitCode = (int) execResult.getOrDefault("exitCode", -1);
            boolean passed = exitCode == 0 && actualOutput.equals(expectedOutput);

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

        boolean alreadyPassed = hasUserPassedTask(taskId, userId);

        CodingSubmission submission = CodingSubmission.builder()
                .user(user)
                .codingTask(task)
                .codeSubmitted(code)
                .status(status)
                .resultJson(resultJson)
                .build();

        CodingSubmission saved = submissionRepository.save(submission);

        // Award XP on first successful pass
        if (allPassed && !alreadyPassed) {
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

    @Override
    @Transactional
    public CodingTask importTaskFromMarkdown(Long moduleId, String content) {
        CodingTask task = new CodingTask();
        task.setPointsReward(50); // Default reward
        task.setLanguage("javascript"); // Default language

        String[] lines = content.split("\n");
        StringBuilder descriptionBuilder = new StringBuilder();
        StringBuilder starterCodeBuilder = new StringBuilder();
        List<Map<String, String>> testCases = new ArrayList<>();
        
        boolean inStarterCode = false;
        boolean inTestsSection = false;

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.startsWith("# ") && task.getTitle() == null) {
                task.setTitle(trimmedLine.substring(2).trim());
            } else if (trimmedLine.toLowerCase().startsWith("language:")) {
                task.setLanguage(trimmedLine.substring(9).trim());
            } else if (trimmedLine.toLowerCase().startsWith("reward:")) {
                try {
                    task.setPointsReward(Integer.parseInt(trimmedLine.substring(7).trim()));
                } catch (NumberFormatException ignored) {}
            } else if (trimmedLine.toLowerCase().startsWith("## starter code") || trimmedLine.toLowerCase().startsWith("## шаблон кода") || trimmedLine.toLowerCase().startsWith("## начальный код")) {
                inStarterCode = true;
                inTestsSection = false;
            } else if (trimmedLine.toLowerCase().startsWith("## tests") || trimmedLine.toLowerCase().startsWith("## тесты") || trimmedLine.toLowerCase().startsWith("## test cases") || trimmedLine.toLowerCase().startsWith("## testcases")) {
                inStarterCode = false;
                inTestsSection = true;
            } else if (inStarterCode) {
                if (!trimmedLine.startsWith("```")) {
                    starterCodeBuilder.append(line).append("\n");
                }
            } else if (inTestsSection) {
                boolean isBullet = trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") || trimmedLine.startsWith("+ ") || trimmedLine.matches("^\\d+\\.\\s+.*");
                if (isBullet) {
                    String testLine = trimmedLine.replaceFirst("^([\\-*+]|\\d+\\.)\\s+", "");
                    Map<String, String> testCase = new HashMap<>();
                    String[] parts = testLine.split("\\|");
                    for (String part : parts) {
                        String[] kv = part.split(":", 2);
                        if (kv.length == 2) {
                            String key = kv[0].trim().toLowerCase().replace(" ", "").replace("_", "").replace("-", "");
                            String value = kv[1].trim().replace("`", ""); // Remove backticks
                            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            if (key.equals("input") || key.equals("stdin") || key.equals("testinput") || key.equals("вход") || key.equals("входныеданные")) {
                                testCase.put("input", value);
                            } else if (key.equals("expected") || key.equals("expectedoutput") || key.equals("output") || key.equals("expected_output") || key.equals("результат") || key.equals("выход") || key.equals("выходныеданные")) {
                                testCase.put("expectedOutput", value);
                            } else if (key.equals("label") || key.equals("name") || key.equals("testname") || key.equals("название") || key.equals("метка")) {
                                testCase.put("label", value);
                            }
                        }
                    }
                    if (!testCase.isEmpty()) {
                        testCases.add(testCase);
                    }
                }
            } else {
                if (task.getTitle() != null && !trimmedLine.startsWith("# ") && !trimmedLine.toLowerCase().startsWith("language:") && !trimmedLine.toLowerCase().startsWith("reward:")) {
                    descriptionBuilder.append(line).append("\n");
                }
            }
        }

        if (task.getTitle() == null) {
            task.setTitle("Imported Coding Task");
        }
        
        task.setDescription(descriptionBuilder.toString().trim());
        task.setStarterCode(starterCodeBuilder.toString().trim());
        
        try {
            task.setTestCasesJson(objectMapper.writeValueAsString(testCases));
        } catch (Exception e) {
            task.setTestCasesJson("[]");
        }

        return createTask(moduleId, task);
    }
}
