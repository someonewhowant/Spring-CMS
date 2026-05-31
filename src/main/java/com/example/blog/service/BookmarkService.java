package com.example.blog.service;

import com.example.blog.entity.Bookmark;
import java.util.List;

public interface BookmarkService {
    void togglePostBookmark(Long userId, Long postId);
    void toggleCourseBookmark(Long userId, Long courseId);
    
    List<Bookmark> getUserBookmarks(Long userId);
    
    boolean isPostBookmarked(Long userId, Long postId);
    boolean isCourseBookmarked(Long userId, Long courseId);
}
