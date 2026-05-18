package com.example.blog.service;

import com.example.blog.entity.Course;
import com.example.blog.entity.CourseModule;
import java.util.List;

public interface CourseService {
    List<Course> getAllCourses();
    Course getCourseById(Long id);
    Course createCourse(Course course);
    Course updateCourse(Long id, Course course);
    void deleteCourse(Long id);

    // Module management
    CourseModule addModule(Long courseId, CourseModule module);
    void deleteModule(Long moduleId);
    List<CourseModule> getModulesByCourseId(Long courseId);
    CourseModule getModuleById(Long moduleId);
    CourseModule updateModule(Long moduleId, CourseModule module);
    
    void setModuleQuiz(Long moduleId, Long quizId);
}
