package com.example.blog.service.impl;

import com.example.blog.entity.Assignment;
import com.example.blog.entity.AssignmentSubmission;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.PeerReview;
import com.example.blog.entity.User;
import com.example.blog.repository.AssignmentRepository;
import com.example.blog.repository.AssignmentSubmissionRepository;
import com.example.blog.repository.CourseModuleRepository;
import com.example.blog.repository.PeerReviewRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.AssignmentService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final PeerReviewRepository peerReviewRepository;
    private final CourseModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final com.example.blog.service.GamificationService gamificationService;

    @Override
    public Assignment createAssignment(Long moduleId, Assignment assignment) {
        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleId));
        assignment.setModule(module);
        return assignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public Assignment getAssignment(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> getAssignmentsByModule(Long moduleId) {
        return assignmentRepository.findByModuleId(moduleId);
    }

    @Override
    public AssignmentSubmission submitAssignment(Long assignmentId, Long userId, String submissionText, String fileUrl) {
        Assignment assignment = getAssignment(assignmentId);
        
        if (assignment.getDeadline() != null && java.time.LocalDateTime.now().isAfter(assignment.getDeadline())) {
            throw new IllegalStateException("Cannot submit: the deadline for this assignment has passed.");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Optional<AssignmentSubmission> existingOpt = submissionRepository.findByAssignmentIdAndUserId(assignmentId, userId);
        AssignmentSubmission submission;
        if (existingOpt.isPresent()) {
            submission = existingOpt.get();
            submission.setSubmissionText(submissionText);
            submission.setFileUrl(fileUrl);
            submission.setStatus(AssignmentSubmission.SubmissionStatus.PENDING);
            // Optionally, reset score/reviews if resubmitting
        } else {
            submission = AssignmentSubmission.builder()
                    .assignment(assignment)
                    .user(user)
                    .submissionText(submissionText)
                    .fileUrl(fileUrl)
                    .status(AssignmentSubmission.SubmissionStatus.PENDING)
                    .build();
        }
        return submissionRepository.save(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentSubmission getSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentSubmission getSubmissionByUserAndAssignment(Long userId, Long assignmentId) {
        return submissionRepository.findByAssignmentIdAndUserId(assignmentId, userId)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmission> getSubmissionsByAssignment(Long assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId);
    }

    @Override
    public PeerReview addPeerReview(Long submissionId, Long reviewerId, int score, String feedbackComment) {
        AssignmentSubmission submission = getSubmission(submissionId);
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerId));

        if (submission.getUser().getId().equals(reviewerId)) {
            throw new IllegalArgumentException("Cannot review your own submission.");
        }
        
        if (peerReviewRepository.existsBySubmissionIdAndReviewerId(submissionId, reviewerId)) {
            throw new IllegalArgumentException("You have already reviewed this submission.");
        }

        PeerReview review = PeerReview.builder()
                .submission(submission)
                .reviewer(reviewer)
                .score(score)
                .feedbackComment(feedbackComment)
                .build();
        
        PeerReview saved = peerReviewRepository.save(review);
        
        // Update submission status if needed, e.g., if it has at least 2 reviews, mark it GRADED
        List<PeerReview> existingReviews = peerReviewRepository.findBySubmissionId(submissionId);
        if (existingReviews.size() >= 2) {
            submission.setStatus(AssignmentSubmission.SubmissionStatus.GRADED);
            double avgScore = existingReviews.stream().mapToInt(PeerReview::getScore).average().orElse(0.0);
            submission.setScore((int) Math.round(avgScore));
            submissionRepository.save(submission);
            
            // Gamification points for successfully finishing the task
            gamificationService.awardXp(submission.getUser().getId(), (int) Math.round(avgScore) * 10, "Assignment graded");
        } else if (submission.getStatus() == AssignmentSubmission.SubmissionStatus.PENDING) {
            submission.setStatus(AssignmentSubmission.SubmissionStatus.IN_REVIEW);
            submissionRepository.save(submission);
        }

        // Award XP to the reviewer for completing a review.
        gamificationService.awardXp(reviewerId, 25, "Completed peer review");

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeerReview> getPeerReviewsForSubmission(Long submissionId) {
        return peerReviewRepository.findBySubmissionId(submissionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmission> getSubmissionsNeedingReview(Long reviewerId) {
        // Find all submissions that are PENDING or IN_REVIEW
        List<AssignmentSubmission> candidates = submissionRepository.findAll().stream()
            .filter(sub -> sub.getStatus() == AssignmentSubmission.SubmissionStatus.PENDING || 
                           sub.getStatus() == AssignmentSubmission.SubmissionStatus.IN_REVIEW)
            .filter(sub -> !sub.getUser().getId().equals(reviewerId)) // not my own
            .filter(sub -> !peerReviewRepository.existsBySubmissionIdAndReviewerId(sub.getId(), reviewerId)) // not already reviewed by me
            .collect(Collectors.toList());
            
        return candidates;
    }
}
