package com.example.blog.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.blog.entity.CodingSubmission;
import com.example.blog.entity.CodingTask;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.User;
import com.example.blog.repository.CodingSubmissionRepository;
import com.example.blog.repository.CodingTaskRepository;
import com.example.blog.repository.CourseModuleRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.impl.CodingTaskServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CodingTaskServiceTest {

    private CodingTaskServiceImpl codingTaskService;

    @Mock private CodingTaskRepository taskRepository;
    @Mock private CodingSubmissionRepository submissionRepository;
    @Mock private CourseModuleRepository moduleRepository;
    @Mock private UserRepository userRepository;
    @Mock private SandboxExecutionService sandboxExecutionService;
    @Mock private GamificationService gamificationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        codingTaskService = new CodingTaskServiceImpl(
                taskRepository,
                submissionRepository,
                moduleRepository,
                userRepository,
                sandboxExecutionService,
                gamificationService,
                objectMapper
        );
    }

    @Test
    void testImportTaskFromMarkdown() {
        String markdown = "# FizzBuzz\n" +
                "Language: javascript\n" +
                "Reward: 100\n" +
                "Write a function that returns Fizz for multiples of 3.\n" +
                "\n" +
                "## Starter Code\n" +
                "```javascript\n" +
                "function fizzBuzz(n) {\n" +
                "  // code here\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "## Tests\n" +
                "- Input: `3` | Expected: `\"Fizz\"` | Label: `Divisible by 3`\n" +
                "- Input: `5` | Expected: `\"Buzz\"` | Label: `Divisible by 5`";

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(new CourseModule()));
        when(taskRepository.save(any(CodingTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CodingTask task = codingTaskService.importTaskFromMarkdown(1L, markdown);

        assertNotNull(task);
        assertEquals("FizzBuzz", task.getTitle());
        assertEquals("javascript", task.getLanguage());
        assertEquals(100, task.getPointsReward());
        assertTrue(task.getDescription().contains("Write a function that returns Fizz"));
        assertTrue(task.getStarterCode().contains("function fizzBuzz(n)"));
        assertTrue(task.getTestCasesJson().contains("expectedOutput\":\"\\\"Fizz\\\"\""));
        assertTrue(task.getTestCasesJson().contains("label\":\"Divisible by 3"));
    }

    @Test
    void testSubmitCodeFirstPassAwardsXp() {
        User user = User.builder()
                .id(1L)
                .username("student")
                .experiencePoints(0)
                .level(1)
                .build();

        CodingTask task = CodingTask.builder()
                .id(1L)
                .title("FizzBuzz")
                .pointsReward(100)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Mock sandbox runCode to return passed
        Map<String, Object> runResult = new HashMap<>();
        runResult.put("passed", true);
        runResult.put("results", Map.of());
        runResult.put("output", "Success");
        when(sandboxExecutionService.executeCode(any(), any(), any())).thenReturn(runResult);

        // First pass: count passed submissions is 0
        when(submissionRepository.countByCodingTaskIdAndUserIdAndStatus(1L, 1L, CodingSubmission.SubmissionStatus.PASSED))
                .thenReturn(0L);

        when(submissionRepository.save(any(CodingSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        codingTaskService.submitCode(1L, 1L, "code", "javascript");

        verify(gamificationService).awardXp(eq(1L), eq(100), contains("Coding task passed: FizzBuzz"));
    }

    @Test
    void testSubmitCodeSubsequentPassDoesNotAwardXp() {
        User user = User.builder()
                .id(1L)
                .username("student")
                .experiencePoints(100)
                .level(1)
                .build();

        CodingTask task = CodingTask.builder()
                .id(1L)
                .title("FizzBuzz")
                .pointsReward(100)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Mock sandbox runCode to return passed
        Map<String, Object> runResult = new HashMap<>();
        runResult.put("passed", true);
        runResult.put("results", Map.of());
        runResult.put("output", "Success");
        when(sandboxExecutionService.executeCode(any(), any(), any())).thenReturn(runResult);

        // Already passed once: count passed submissions is 1
        when(submissionRepository.countByCodingTaskIdAndUserIdAndStatus(1L, 1L, CodingSubmission.SubmissionStatus.PASSED))
                .thenReturn(1L);

        when(submissionRepository.save(any(CodingSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        codingTaskService.submitCode(1L, 1L, "code", "javascript");

        verify(gamificationService, never()).awardXp(anyLong(), anyInt(), anyString());
    }
}
