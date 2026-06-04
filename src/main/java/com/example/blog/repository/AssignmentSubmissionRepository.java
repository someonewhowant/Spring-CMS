package com.example.blog.repository;

import com.example.blog.entity.AssignmentSubmission;
import com.example.blog.entity.AssignmentSubmission.SubmissionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    List<AssignmentSubmission> findByAssignmentId(Long assignmentId);
    List<AssignmentSubmission> findByUserId(Long userId);
    Optional<AssignmentSubmission> findByAssignmentIdAndUserId(Long assignmentId, Long userId);
    List<AssignmentSubmission> findByStatus(SubmissionStatus status);
    List<AssignmentSubmission> findByAssignmentModuleCourseId(Long courseId);
}
