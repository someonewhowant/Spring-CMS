package com.example.blog.repository;

import com.example.blog.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    
    List<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Optional<Bookmark> findByUserIdAndPostId(Long userId, Long postId);
    
    Optional<Bookmark> findByUserIdAndCourseId(Long userId, Long courseId);
    
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
}
