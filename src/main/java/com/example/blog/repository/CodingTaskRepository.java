package com.example.blog.repository;

import com.example.blog.entity.CodingTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodingTaskRepository extends JpaRepository<CodingTask, Long> {

    List<CodingTask> findByModuleId(Long moduleId);
}
