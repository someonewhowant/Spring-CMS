package com.example.blog.service;

import com.example.blog.entity.CodingSubmission;
import com.example.blog.entity.CodingTask;
import java.util.List;
import java.util.Map;

public interface CodingTaskService {

    CodingTask createTask(Long moduleId, CodingTask task);

    CodingTask getTask(Long id);

    List<CodingTask> getTasksByModule(Long moduleId);

    void deleteTask(Long id);

    /**
     * Executes user code against the task's test cases without saving a submission.
     * Returns a map with "passed" (boolean), "results" (list of test outcomes), "output" (console).
     */
    Map<String, Object> runCode(Long taskId, String code, String language);

    /**
     * Submits user code for final grading. Saves the submission and awards XP if all tests pass.
     */
    CodingSubmission submitCode(Long taskId, Long userId, String code, String language);

    boolean hasUserPassedTask(Long taskId, Long userId);

    CodingTask importTaskFromMarkdown(Long moduleId, String content);
}
