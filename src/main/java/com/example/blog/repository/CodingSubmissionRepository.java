package com.example.blog.repository;

import com.example.blog.entity.CodingSubmission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodingSubmissionRepository extends JpaRepository<CodingSubmission, Long> {

    List<CodingSubmission> findByCodingTaskIdAndUserId(Long codingTaskId, Long userId);

    Optional<CodingSubmission> findFirstByCodingTaskIdAndUserIdAndStatus(
            Long codingTaskId, Long userId, CodingSubmission.SubmissionStatus status);

    List<CodingSubmission> findByUserId(Long userId);

    List<CodingSubmission> findByCodingTaskId(Long codingTaskId);

    long countByCodingTaskIdAndUserIdAndStatus(
            Long codingTaskId, Long userId, CodingSubmission.SubmissionStatus status);
}
