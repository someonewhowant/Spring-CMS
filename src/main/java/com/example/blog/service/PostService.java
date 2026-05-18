package com.example.blog.service;

import com.example.blog.entity.Post;
import com.example.blog.entity.Category;
import com.example.blog.entity.Tag;
import org.springframework.data.domain.Page;
import java.util.List;

public interface PostService {
    Page<Post> getAllPosts(int page);
    List<Post> getAllPosts();
    Post getPostById(Long id);
    Page<Post> searchPosts(String query, int page);
    Page<Post> getPostsByCategory(String categorySlug, int page);
    Page<Post> getPostsByTag(String tagSlug, int page);
    List<Category> getAllCategories();
    Category createCategory(Category category);
    void deleteCategory(Long id);
    List<Tag> getAllTags();
    Post createPost(Post post);
    Post updatePost(Long id, Post postDetails);
    void deletePost(Long id);
}
