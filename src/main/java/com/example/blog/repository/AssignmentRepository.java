package com.example.blog.repository;

import com.example.blog.entity.Assignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByModuleId(Long moduleId);
    List<Assignment> findByModuleCourseId(Long courseId);
}
