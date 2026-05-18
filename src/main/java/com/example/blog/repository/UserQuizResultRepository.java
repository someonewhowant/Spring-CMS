package com.example.blog.repository;

import com.example.blog.entity.UserQuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserQuizResultRepository extends JpaRepository<UserQuizResult, Long> {
    Optional<UserQuizResult> findByUserIdAndQuizId(Long userId, Long quizId);
}
