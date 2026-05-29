package com.example.blog.service;

import com.example.blog.entity.Comment;
import com.example.blog.entity.Post;
import com.example.blog.entity.User;

import java.util.List;

public interface CommentService {
    Comment addComment(Post post, User user, String body);
    List<Comment> getCommentsByPostId(Long postId);
    long getCommentCount(Long postId);
    void deleteComment(Long commentId);
    Comment getCommentById(Long commentId);
}
