package com.example.blog.repository;

import com.example.blog.entity.PeerReview;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeerReviewRepository extends JpaRepository<PeerReview, Long> {
    List<PeerReview> findBySubmissionId(Long submissionId);
    List<PeerReview> findByReviewerId(Long reviewerId);
    boolean existsBySubmissionIdAndReviewerId(Long submissionId, Long reviewerId);
}
