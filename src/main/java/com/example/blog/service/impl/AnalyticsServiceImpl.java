package com.example.blog.service.impl;

import com.example.blog.entity.CodingSubmission;
import com.example.blog.entity.CodingSubmission.SubmissionStatus;
import com.example.blog.entity.CodingTask;
import com.example.blog.entity.Quiz;
import com.example.blog.entity.Role;
import com.example.blog.entity.User;
import com.example.blog.entity.UserQuizResult;
import com.example.blog.repository.CodingSubmissionRepository;
import com.example.blog.repository.CodingTaskRepository;
import com.example.blog.repository.QuizRepository;
import com.example.blog.repository.UserQuizResultRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.AnalyticsService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final UserQuizResultRepository userQuizResultRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final CodingTaskRepository codingTaskRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Override
    public Map<String, Object> getXpProgressionData(Long userId, int days) {
        List<UserQuizResult> results =
                userQuizResultRepository.findByUserIdOrderByCompletedAtAsc(userId);
        List<CodingSubmission> submissions = codingSubmissionRepository.findByUserId(userId);

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        // Build a map of date -> total score earned that day
        Map<LocalDate, Integer> dailyScores = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            dailyScores.put(startDate.plusDays(i), 0);
        }

        for (UserQuizResult r : results) {
            LocalDate date =
                    r.getCompletedAt() != null
                            ? r.getCompletedAt().atZone(ZONE).toLocalDate()
                            : today; // fallback for legacy records without timestamp
            if (!date.isBefore(startDate) && !date.isAfter(today)) {
                dailyScores.merge(date, r.getScore() * 10, Integer::sum);
            }
        }

        // Add coding task XP (only earliest pass per task)
        Map<Long, CodingSubmission> earliestPassByTask = new HashMap<>();
        for (CodingSubmission s : submissions) {
            if (s.getStatus() == SubmissionStatus.PASSED) {
                earliestPassByTask.merge(s.getCodingTask().getId(), s, (existing, candidate) ->
                    candidate.getSubmittedAt().isBefore(existing.getSubmittedAt()) ? candidate : existing
                );
            }
        }

        for (CodingSubmission s : earliestPassByTask.values()) {
            LocalDate date = s.getSubmittedAt() != null
                    ? s.getSubmittedAt().toLocalDate()
                    : today;
            if (!date.isBefore(startDate) && !date.isAfter(today)) {
                dailyScores.merge(date, s.getCodingTask().getPointsReward(), Integer::sum);
            }
        }

        // Convert to cumulative XP
        List<String> labels = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        int cumulative = 0;

        // Add XP earned before the window
        for (UserQuizResult r : results) {
            LocalDate date =
                    r.getCompletedAt() != null
                            ? r.getCompletedAt().atZone(ZONE).toLocalDate()
                            : today;
            if (date.isBefore(startDate)) {
                cumulative += r.getScore() * 10;
            }
        }

        for (CodingSubmission s : earliestPassByTask.values()) {
            LocalDate date = s.getSubmittedAt() != null
                    ? s.getSubmittedAt().toLocalDate()
                    : today;
            if (date.isBefore(startDate)) {
                cumulative += s.getCodingTask().getPointsReward();
            }
        }

        for (Map.Entry<LocalDate, Integer> entry : dailyScores.entrySet()) {
            cumulative += entry.getValue();
            labels.add(entry.getKey().format(DATE_FMT));
            data.add(cumulative);
        }


        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> getScoresByCourse(Long userId) {
        List<UserQuizResult> results = userQuizResultRepository.findByUserId(userId);

        // Group by course name and compute average score
        Map<String, List<Integer>> courseScores = new LinkedHashMap<>();
        for (UserQuizResult r : results) {
            String courseName = r.getQuiz().getCourse().getTitle();
            courseScores.computeIfAbsent(courseName, k -> new ArrayList<>()).add(r.getScore());
        }

        List<String> labels = new ArrayList<>(courseScores.keySet());
        List<Double> data =
                courseScores.values().stream()
                        .map(
                                scores -> {
                                    double avg =
                                            scores.stream()
                                                            .mapToInt(Integer::intValue)
                                                            .average()
                                                            .orElse(0.0)
                                                    * 20; // convert to percentage (out of 5 -> %)
                                    return Math.round(avg * 10.0) / 10.0;
                                })
                        .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }

    @Override
    public List<Map<String, Object>> getActivityHeatmapData(Long userId) {
        List<UserQuizResult> results =
                userQuizResultRepository.findByUserIdOrderByCompletedAtAsc(userId);
        
        List<CodingSubmission> submissions =
                codingSubmissionRepository.findByUserId(userId);

        Map<String, Integer> dailyCounts = new LinkedHashMap<>();
        
        for (UserQuizResult r : results) {
            LocalDate date =
                    r.getCompletedAt() != null
                            ? r.getCompletedAt().atZone(ZONE).toLocalDate()
                            : LocalDate.now();
            String key = date.format(ISO_FMT);
            dailyCounts.merge(key, 1, Integer::sum);
        }

        for (CodingSubmission s : submissions) {
            LocalDate date =
                    s.getSubmittedAt() != null
                            ? s.getSubmittedAt().toLocalDate()
                            : LocalDate.now();
            String key = date.format(ISO_FMT);
            dailyCounts.merge(key, 1, Integer::sum);
        }

        List<Map<String, Object>> heatmap = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : dailyCounts.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", entry.getKey());
            point.put("count", entry.getValue());
            heatmap.add(point);
        }
        return heatmap;
    }


    @Override
    public Map<String, Object> getPassFailSummary(Long userId) {
        List<UserQuizResult> results = userQuizResultRepository.findByUserId(userId);

        int passed = 0;
        int failed = 0;
        for (UserQuizResult r : results) {
            if (r.getScore() >= 3) {
                passed++;
            } else {
                failed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("passed", passed);
        result.put("failed", failed);
        return result;
    }

    // --- Teacher analytics ---

    @Override
    public Map<String, Object> getStudentLevelDistribution() {
        List<User> students = userRepository.findByRole(Role.STUDENT);

        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (User student : students) {
            int level = student.getLevel() != null ? student.getLevel() : 1;
            String label = "Level " + level;
            distribution.merge(label, 1, Integer::sum);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", new ArrayList<>(distribution.keySet()));
        result.put("data", new ArrayList<>(distribution.values()));
        return result;
    }

    @Override
    public Map<String, Object> getAverageScoresByQuiz() {
        List<Quiz> allQuizzes = quizRepository.findAll();

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        for (Quiz quiz : allQuizzes) {
            List<UserQuizResult> results = userQuizResultRepository.findByQuizId(quiz.getId());
            if (!results.isEmpty()) {
                double avg =
                        results.stream().mapToInt(UserQuizResult::getScore).average().orElse(0.0);
                labels.add(quiz.getTitle());
                data.add(Math.round(avg * 10.0) / 10.0);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }

    @Override
    public List<Map<String, Object>> getHardestQuizzes(int limit) {
        List<Quiz> allQuizzes = quizRepository.findAll();

        List<Map<String, Object>> quizStats = new ArrayList<>();
        for (Quiz quiz : allQuizzes) {
            List<UserQuizResult> results = userQuizResultRepository.findByQuizId(quiz.getId());
            if (results.isEmpty()) {
                continue;
            }
            long passedCount = results.stream().filter(r -> r.getScore() >= 3).count();
            double passRate = (passedCount * 100.0) / results.size();

            Map<String, Object> stat = new HashMap<>();
            stat.put("title", quiz.getTitle());
            stat.put("courseName", quiz.getCourse().getTitle());
            stat.put("passRate", Math.round(passRate * 10.0) / 10.0);
            stat.put("totalAttempts", results.size());
            quizStats.add(stat);
        }

        // Sort by pass rate ascending (hardest first)
        quizStats.sort(
                (a, b) ->
                        Double.compare(
                                (double) a.get("passRate"), (double) b.get("passRate")));

        return quizStats.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getCodingTaskStats(Long userId) {
        List<CodingSubmission> submissions = codingSubmissionRepository.findByUserId(userId);

        // Deduplicate: keep only the best (PASSED beats FAILED) submission per task
        Map<Long, CodingSubmission> bestByTask = new LinkedHashMap<>();
        for (CodingSubmission s : submissions) {
            Long taskId = s.getCodingTask().getId();
            bestByTask.merge(taskId, s, (existing, candidate) -> {
                if (candidate.getStatus() == SubmissionStatus.PASSED) return candidate;
                return existing;
            });
        }

        int totalAttempted = bestByTask.size();
        long totalPassed = bestByTask.values().stream()
                .filter(s -> s.getStatus() == SubmissionStatus.PASSED)
                .count();

        // XP earned from coding tasks
        int xpFromCode = bestByTask.values().stream()
                .filter(s -> s.getStatus() == SubmissionStatus.PASSED)
                .mapToInt(s -> s.getCodingTask().getPointsReward())
                .sum();

        // Per-task breakdown for the table
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (CodingSubmission s : bestByTask.values()) {
            Map<String, Object> row = new HashMap<>();
            row.put("title", s.getCodingTask().getTitle());
            row.put("language", s.getCodingTask().getLanguage());
            row.put("status", s.getStatus().name());
            row.put("xp", s.getCodingTask().getPointsReward());
            tasks.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalAttempted", totalAttempted);
        result.put("totalPassed", totalPassed);
        result.put("xpFromCode", xpFromCode);
        result.put("tasks", tasks);
        return result;
    }

    @Override
    public Map<String, Object> getCodingTaskPlatformStats() {
        List<CodingTask> allTasks = codingTaskRepository.findAll();

        List<String> labels = new ArrayList<>();
        List<Double> passRates = new ArrayList<>();
        List<Integer> totalSubmissions = new ArrayList<>();

        for (CodingTask task : allTasks) {
            List<CodingSubmission> subs = codingSubmissionRepository.findByCodingTaskId(task.getId());
            if (subs.isEmpty()) continue;

            long passed = subs.stream().filter(s -> s.getStatus() == SubmissionStatus.PASSED).count();
            double passRate = (passed * 100.0) / subs.size();

            labels.add(task.getTitle());
            passRates.add(Math.round(passRate * 10.0) / 10.0);
            totalSubmissions.add(subs.size());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("passRates", passRates);
        result.put("totalSubmissions", totalSubmissions);
        return result;
    }
}
