package com.example.blog.repository;

import com.example.blog.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    long countByTeacherId(Long teacherId);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Course c WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    java.util.List<Course> searchByKeyword(@org.springframework.data.repository.query.Param("keyword") String keyword);
}
