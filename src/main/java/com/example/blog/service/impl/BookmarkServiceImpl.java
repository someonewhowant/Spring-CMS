package com.example.blog.service.impl;

import com.example.blog.entity.Bookmark;
import com.example.blog.entity.Course;
import com.example.blog.entity.Post;
import com.example.blog.entity.User;
import com.example.blog.repository.BookmarkRepository;
import com.example.blog.repository.CourseRepository;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public void togglePostBookmark(Long userId, Long postId) {
        Optional<Bookmark> existing = bookmarkRepository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
        } else {
            User user = userRepository.findById(userId).orElseThrow();
            Post post = postRepository.findById(postId).orElseThrow();
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .post(post)
                    .build();
            bookmarkRepository.save(bookmark);
        }
    }

    @Override
    @Transactional
    public void toggleCourseBookmark(Long userId, Long courseId) {
        Optional<Bookmark> existing = bookmarkRepository.findByUserIdAndCourseId(userId, courseId);
        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
        } else {
            User user = userRepository.findById(userId).orElseThrow();
            Course course = courseRepository.findById(courseId).orElseThrow();
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .course(course)
                    .build();
            bookmarkRepository.save(bookmark);
        }
    }

    @Override
    public List<Bookmark> getUserBookmarks(Long userId) {
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public boolean isPostBookmarked(Long userId, Long postId) {
        return bookmarkRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Override
    public boolean isCourseBookmarked(Long userId, Long courseId) {
        return bookmarkRepository.existsByUserIdAndCourseId(userId, courseId);
    }
}
