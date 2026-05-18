package com.example.blog.service.impl;

import com.example.blog.entity.Course;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.Quiz;
import com.example.blog.repository.CourseModuleRepository;
import com.example.blog.repository.CourseRepository;
import com.example.blog.repository.QuizRepository;
import com.example.blog.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final QuizRepository quizRepository;

    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
    }

    @Override
    @Transactional
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public Course updateCourse(Long id, Course courseDetails) {
        Course course = getCourseById(id);
        course.setTitle(courseDetails.getTitle());
        course.setDescription(courseDetails.getDescription());
        course.setContent(courseDetails.getContent());
        course.setImageUrl(courseDetails.getImageUrl());
        course.setLevel(courseDetails.getLevel());
        course.setDuration(courseDetails.getDuration());
        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        List<CourseModule> modules = moduleRepository.findByCourseIdOrderByOrderIndexAsc(id);
        moduleRepository.deleteAll(modules);
        
        List<Quiz> quizzes = quizRepository.findByCourseId(id);
        quizRepository.deleteAll(quizzes);

        Course course = getCourseById(id);
        courseRepository.delete(course);
    }

    @Override
    @Transactional
    public CourseModule addModule(Long courseId, CourseModule module) {
        Course course = getCourseById(courseId);
        module.setCourse(course);
        
        // Auto-set order index if not set
        if (module.getOrderIndex() == 0) {
            int maxOrder = course.getModules().stream()
                    .mapToInt(CourseModule::getOrderIndex)
                    .max()
                    .orElse(-1);
            module.setOrderIndex(maxOrder + 1);
        }
        
        return moduleRepository.save(module);
    }

    @Override
    @Transactional
    public void deleteModule(Long moduleId) {
        moduleRepository.deleteById(moduleId);
    }

    @Override
    public List<CourseModule> getModulesByCourseId(Long courseId) {
        return moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }

    @Override
    public CourseModule getModuleById(Long moduleId) {
        return moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found with id: " + moduleId));
    }

    @Override
    @Transactional
    public CourseModule updateModule(Long moduleId, CourseModule moduleDetails) {
        CourseModule module = getModuleById(moduleId);
        module.setTitle(moduleDetails.getTitle());
        module.setContent(moduleDetails.getContent());
        if (moduleDetails.getOrderIndex() != 0) {
            module.setOrderIndex(moduleDetails.getOrderIndex());
        }
        return moduleRepository.save(module);
    }

    @Override
    @Transactional
    public void setModuleQuiz(Long moduleId, Long quizId) {
        CourseModule module = getModuleById(moduleId);
        if (quizId != null) {
            Quiz quiz = quizRepository.findById(quizId)
                    .orElseThrow(() -> new RuntimeException("Quiz not found"));
            module.setQuiz(quiz);
        } else {
            module.setQuiz(null);
        }
        moduleRepository.save(module);
    }
}
