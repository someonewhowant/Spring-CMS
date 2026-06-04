package com.example.blog.service;

import com.example.blog.entity.Assignment;
import com.example.blog.entity.AssignmentSubmission;
import com.example.blog.entity.PeerReview;
import java.util.List;

public interface AssignmentService {
    
    // Assignment management
    Assignment createAssignment(Long moduleId, Assignment assignment);
    Assignment getAssignment(Long id);
    List<Assignment> getAssignmentsByModule(Long moduleId);
    
    // Submission management
    AssignmentSubmission submitAssignment(Long assignmentId, Long userId, String submissionText, String fileUrl);
    AssignmentSubmission getSubmission(Long id);
    AssignmentSubmission getSubmissionByUserAndAssignment(Long userId, Long assignmentId);
    List<AssignmentSubmission> getSubmissionsByAssignment(Long assignmentId);
    
    // Peer Review management
    PeerReview addPeerReview(Long submissionId, Long reviewerId, int score, String feedbackComment);
    List<PeerReview> getPeerReviewsForSubmission(Long submissionId);
    
    // Peer review queue
    List<AssignmentSubmission> getSubmissionsNeedingReview(Long reviewerId);
}
