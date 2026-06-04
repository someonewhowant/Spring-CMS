package com.example.blog.repository;

import com.example.blog.entity.UserQuizResult;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserQuizResultRepository extends JpaRepository<UserQuizResult, Long> {
    Optional<UserQuizResult> findByUserIdAndQuizId(Long userId, Long quizId);

    java.util.List<UserQuizResult> findByUserId(Long userId);

    java.util.List<UserQuizResult> findByUserIdOrderByCompletedAtAsc(Long userId);

    Page<UserQuizResult> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM UserQuizResult r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /** Count total quiz attempts across all students for a given quiz. */
    long countByQuizId(Long quizId);

    /** Find all results for a given quiz (for teacher analytics). */
    java.util.List<UserQuizResult> findByQuizId(Long quizId);

    /** Find all results for quizzes belonging to a course. */
    @Query("SELECT r FROM UserQuizResult r WHERE r.quiz.course.id = :courseId")
    java.util.List<UserQuizResult> findByCourseId(@Param("courseId") Long courseId);
}
