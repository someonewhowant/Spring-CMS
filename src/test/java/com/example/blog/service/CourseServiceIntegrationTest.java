package com.example.blog.service;

import com.example.blog.entity.Course;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.Quiz;
import com.example.blog.repository.CourseModuleRepository;
import com.example.blog.repository.CourseRepository;
import com.example.blog.repository.QuizRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
class CourseServiceIntegrationTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseModuleRepository moduleRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Test
    @Transactional
    void testDeleteCourseWithModulesAndQuizzes() {
        // Create a course
        Course course = Course.builder()
                .title("Test Course")
                .description("Description")
                .content("Content")
                .level("Beginner")
                .duration("1 Hour")
                .modules(new ArrayList<>())
                .quizzes(new ArrayList<>())
                .build();
        course = courseRepository.save(course);

        // Create a module associated with the course
        CourseModule module = CourseModule.builder()
                .title("Test Module")
                .content("Module Content")
                .course(course)
                .build();
        module = moduleRepository.save(module);
        course.getModules().add(module);

        // Create a quiz associated with the course
        Quiz quiz = Quiz.builder()
                .title("Test Quiz")
                .course(course)
                .questions(new ArrayList<>())
                .build();
        quiz = quizRepository.save(quiz);
        course.getQuizzes().add(quiz);

        Long courseId = course.getId();
        Long moduleId = module.getId();
        Long quizId = quiz.getId();

        // Verify entities exist
        assertTrue(courseRepository.existsById(courseId));
        assertTrue(moduleRepository.existsById(moduleId));
        assertTrue(quizRepository.existsById(quizId));

        // Delete the course
        courseService.deleteCourse(courseId);

        // Verify course and all its cascaded associations (modules and quizzes) are deleted
        assertFalse(courseRepository.existsById(courseId));
        assertFalse(moduleRepository.existsById(moduleId));
        assertFalse(quizRepository.existsById(quizId));
    }
}
