package com.example.blog.service.impl;

import com.example.blog.entity.Quiz;
import com.example.blog.entity.Role;
import com.example.blog.entity.User;
import com.example.blog.entity.UserQuizResult;
import com.example.blog.repository.QuizRepository;
import com.example.blog.repository.UserQuizResultRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.AnalyticsService;
import java.time.Instant;
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

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Override
    public Map<String, Object> getXpProgressionData(Long userId, int days) {
        List<UserQuizResult> results =
                userQuizResultRepository.findByUserIdOrderByCompletedAtAsc(userId);

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

        Map<String, Integer> dailyCounts = new LinkedHashMap<>();
        for (UserQuizResult r : results) {
            LocalDate date =
                    r.getCompletedAt() != null
                            ? r.getCompletedAt().atZone(ZONE).toLocalDate()
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
}
