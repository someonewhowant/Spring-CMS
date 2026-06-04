package com.example.blog.service;

import java.util.List;
import java.util.Map;

/**
 * Service for aggregating analytics data for student and teacher dashboards. Provides
 * pre-computed data structures optimized for Chart.js consumption.
 */
public interface AnalyticsService {

    /**
     * Returns daily XP progression data for a student over the last N days.
     *
     * @param userId the student's user ID
     * @param days number of days to look back
     * @return map with "labels" (dates) and "data" (cumulative XP values)
     */
    Map<String, Object> getXpProgressionData(Long userId, int days);

    /**
     * Returns quiz score distribution for a student grouped by course.
     *
     * @param userId the student's user ID
     * @return map with "labels" (course names) and "data" (average scores per course)
     */
    Map<String, Object> getScoresByCourse(Long userId);

    /**
     * Returns activity heatmap data for a student (quiz completions per day).
     *
     * @param userId the student's user ID
     * @return list of maps, each containing "date" and "count"
     */
    List<Map<String, Object>> getActivityHeatmapData(Long userId);

    /**
     * Returns a summary of quiz pass/fail rates for a student.
     *
     * @param userId the student's user ID
     * @return map with "passed" and "failed" counts
     */
    Map<String, Object> getPassFailSummary(Long userId);

    // --- Teacher analytics ---

    /**
     * Returns the distribution of students across levels.
     *
     * @return map with "labels" (level names) and "data" (student counts)
     */
    Map<String, Object> getStudentLevelDistribution();

    /**
     * Returns average quiz scores across all students for each quiz.
     *
     * @return map with "labels" (quiz titles) and "data" (average scores)
     */
    Map<String, Object> getAverageScoresByQuiz();

    /**
     * Returns the most difficult questions — quizzes with the lowest pass rates.
     *
     * @param limit max number of results
     * @return list of maps with quiz title and pass rate
     */
    List<Map<String, Object>> getHardestQuizzes(int limit);
}
