package com.example.blog.service.impl;

import com.example.blog.entity.Post;
import com.example.blog.entity.Category;
import com.example.blog.entity.Tag;
import com.example.blog.exception.ResourceNotFoundException;
import com.example.blog.repository.CategoryRepository;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.TagRepository;
import com.example.blog.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private static final int PAGE_SIZE = 6;

    @Override
    public Page<Post> getAllPosts(int page) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, PAGE_SIZE, Sort.by("createdAt").descending());
        return postRepository.findAll(pageable);
    }

    @Override
    public List<Post> getAllPosts() {
        return postRepository.findAll(Sort.by("createdAt").descending());
    }

    @Override
    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
    }

    @Override
    public Page<Post> searchPosts(String query, int page) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, PAGE_SIZE, Sort.by("createdAt").descending());
        return postRepository.searchByKeyword(query, pageable);
    }

    @Override
    public Page<Post> getPostsByCategory(String categorySlug, int page) {
        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categorySlug));
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, PAGE_SIZE, Sort.by("createdAt").descending());
        return postRepository.findByCategory(category, pageable);
    }

    @Override
    public Page<Post> getPostsByTag(String tagSlug, int page) {
        Tag tag = tagRepository.findBySlug(tagSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagSlug));
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, PAGE_SIZE, Sort.by("createdAt").descending());
        return postRepository.findByTags(tag, pageable);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll(Sort.by("name").ascending());
    }

    @Override
    @Transactional
    public Category createCategory(Category category) {
        if (category.getSlug() == null || category.getSlug().isEmpty()) {
            category.setSlug(category.getName().toLowerCase().replaceAll("[^a-z0-9]", "-"));
        }
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        categoryRepository.delete(category);
    }

    @Override
    public List<Tag> getAllTags() {
        return tagRepository.findAll(Sort.by("name").ascending());
    }

    @Override
    @Transactional
    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public Post updatePost(Long id, Post postDetails) {
        Post post = getPostById(id);
        
        post.setTitle(postDetails.getTitle());
        post.setBody(postDetails.getBody());
        if (postDetails.getImageUrl() != null) {
            post.setImageUrl(postDetails.getImageUrl());
        }
        post.setCategory(postDetails.getCategory());
        post.setTags(postDetails.getTags());
        
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        Post post = getPostById(id);
        postRepository.delete(post);
    }
}
