package com.example.blog.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.blog.entity.Course;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.Quiz;
import com.example.blog.entity.User;
import com.example.blog.entity.Role;
import com.example.blog.entity.Bookmark;
import com.example.blog.entity.CodingTask;
import com.example.blog.entity.CodingSubmission;
import com.example.blog.entity.Assignment;
import com.example.blog.entity.AssignmentSubmission;
import com.example.blog.entity.PeerReview;
import com.example.blog.entity.UserQuizResult;
import com.example.blog.repository.CourseModuleRepository;
import com.example.blog.repository.CourseRepository;
import com.example.blog.repository.QuizRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.repository.BookmarkRepository;
import com.example.blog.repository.CodingTaskRepository;
import com.example.blog.repository.CodingSubmissionRepository;
import com.example.blog.repository.AssignmentRepository;
import com.example.blog.repository.AssignmentSubmissionRepository;
import com.example.blog.repository.PeerReviewRepository;
import com.example.blog.repository.UserQuizResultRepository;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(
        properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
class CourseServiceIntegrationTest {

    @Autowired private CourseService courseService;

    @Autowired private CourseRepository courseRepository;

    @Autowired private CourseModuleRepository moduleRepository;

    @Autowired private QuizRepository quizRepository;

    @Autowired private UserRepository userRepository;

    @Autowired private BookmarkRepository bookmarkRepository;

    @Autowired private CodingTaskRepository codingTaskRepository;

    @Autowired private CodingSubmissionRepository codingSubmissionRepository;

    @Autowired private AssignmentRepository assignmentRepository;

    @Autowired private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired private PeerReviewRepository peerReviewRepository;

    @Autowired private UserQuizResultRepository userQuizResultRepository;

    @Autowired private QuizService quizService;

    @Test
    @Transactional
    void testDeleteCourseWithModulesAndQuizzes() {
        // Create a course
        Course course =
                Course.builder()
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
        CourseModule module =
                CourseModule.builder()
                        .title("Test Module")
                        .content("Module Content")
                        .course(course)
                        .build();
        module = moduleRepository.save(module);
        course.getModules().add(module);

        // Create a quiz associated with the course
        Quiz quiz =
                Quiz.builder()
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

    @Test
    @Transactional
    void testDeleteCourseCascadesAllAssociatedEntities() {
        // Create teacher and student users
        User teacher = userRepository.save(User.builder()
                .username("teacher1")
                .password("pass")
                .role(Role.TEACHER)
                .fullName("Teacher One")
                .build());

        User student = userRepository.save(User.builder()
                .username("student1")
                .password("pass")
                .role(Role.STUDENT)
                .fullName("Student One")
                .build());

        // Create course
        Course course = Course.builder()
                .title("Full Stack Course")
                .description("Desc")
                .content("Content")
                .teacher(teacher)
                .level("Beginner")
                .duration("1 Hour")
                .modules(new ArrayList<>())
                .quizzes(new ArrayList<>())
                .build();
        course = courseRepository.save(course);
        Long courseId = course.getId();

        // Set last opened course
        student.setLastOpenedCourseId(courseId);
        student = userRepository.save(student);

        // Create bookmark
        Bookmark bookmark = bookmarkRepository.save(Bookmark.builder()
                .user(student)
                .course(course)
                .build());
        Long bookmarkId = bookmark.getId();

        // Create module
        CourseModule module = CourseModule.builder()
                .title("Module 1")
                .content("Content")
                .course(course)
                .build();
        module = moduleRepository.save(module);
        course.getModules().add(module);
        Long moduleId = module.getId();

        // Set last opened module
        student.setLastOpenedModuleId(moduleId);
        student = userRepository.save(student);

        // Create coding task
        CodingTask codingTask = codingTaskRepository.save(CodingTask.builder()
                .title("Task 1")
                .description("Desc")
                .language("python")
                .testCasesJson("[]")
                .module(module)
                .build());
        Long codingTaskId = codingTask.getId();

        // Create coding submission
        CodingSubmission codingSubmission = codingSubmissionRepository.save(CodingSubmission.builder()
                .user(student)
                .codingTask(codingTask)
                .codeSubmitted("print('hello')")
                .status(CodingSubmission.SubmissionStatus.PASSED)
                .build());
        Long codingSubmissionId = codingSubmission.getId();

        // Create assignment
        Assignment assignment = assignmentRepository.save(Assignment.builder()
                .title("Assignment 1")
                .description("Desc")
                .maxScore(100)
                .module(module)
                .build());
        Long assignmentId = assignment.getId();

        // Create assignment submission
        AssignmentSubmission assignmentSubmission = assignmentSubmissionRepository.save(AssignmentSubmission.builder()
                .user(student)
                .assignment(assignment)
                .submissionText("My submission")
                .status(AssignmentSubmission.SubmissionStatus.PENDING)
                .build());
        Long assignmentSubmissionId = assignmentSubmission.getId();

        // Create peer review
        PeerReview peerReview = peerReviewRepository.save(PeerReview.builder()
                .reviewer(teacher)
                .submission(assignmentSubmission)
                .score(90)
                .feedbackComment("Great job")
                .build());
        Long peerReviewId = peerReview.getId();

        // Create quiz
        Quiz quiz = Quiz.builder()
                .title("Quiz 1")
                .course(course)
                .questions(new ArrayList<>())
                .build();
        quiz = quizRepository.save(quiz);
        course.getQuizzes().add(quiz);
        Long quizId = quiz.getId();

        module.setQuiz(quiz);
        module = moduleRepository.save(module);

        // Create user quiz result
        UserQuizResult quizResult = userQuizResultRepository.save(UserQuizResult.builder()
                .user(student)
                .quiz(quiz)
                .score(5)
                .completedAt(java.time.Instant.now())
                .build());
        Long quizResultId = quizResult.getId();

        // Verify everything exists before deletion
        assertTrue(courseRepository.existsById(courseId));
        assertTrue(bookmarkRepository.existsById(bookmarkId));
        assertTrue(moduleRepository.existsById(moduleId));
        assertTrue(codingTaskRepository.existsById(codingTaskId));
        assertTrue(codingSubmissionRepository.existsById(codingSubmissionId));
        assertTrue(assignmentRepository.existsById(assignmentId));
        assertTrue(assignmentSubmissionRepository.existsById(assignmentSubmissionId));
        assertTrue(peerReviewRepository.existsById(peerReviewId));
        assertTrue(quizRepository.existsById(quizId));
        assertTrue(userQuizResultRepository.existsById(quizResultId));

        // Delete course
        courseService.deleteCourse(courseId);

        // Verify cascades
        assertFalse(courseRepository.existsById(courseId));
        assertFalse(bookmarkRepository.existsById(bookmarkId));
        assertFalse(moduleRepository.existsById(moduleId));
        assertFalse(codingTaskRepository.existsById(codingTaskId));
        assertFalse(codingSubmissionRepository.existsById(codingSubmissionId));
        assertFalse(assignmentRepository.existsById(assignmentId));
        assertFalse(assignmentSubmissionRepository.existsById(assignmentSubmissionId));
        assertFalse(peerReviewRepository.existsById(peerReviewId));
        assertFalse(quizRepository.existsById(quizId));
        assertFalse(userQuizResultRepository.existsById(quizResultId));

        // Verify student's navigation state is nullified
        User updatedStudent = userRepository.findById(student.getId()).orElseThrow();
        assertNull(updatedStudent.getLastOpenedCourseId());
        assertNull(updatedStudent.getLastOpenedModuleId());
    }

    @Test
    @Transactional
    void testDeleteModuleCascadesAllAssociatedEntities() {
        User teacher = userRepository.save(User.builder()
                .username("teacher2")
                .password("pass")
                .role(Role.TEACHER)
                .fullName("Teacher Two")
                .build());

        User student = userRepository.save(User.builder()
                .username("student2")
                .password("pass")
                .role(Role.STUDENT)
                .fullName("Student Two")
                .build());

        Course course = courseRepository.save(Course.builder()
                .title("Course 2")
                .description("Desc")
                .content("Content")
                .teacher(teacher)
                .level("Beginner")
                .duration("1 Hour")
                .build());

        CourseModule module = moduleRepository.save(CourseModule.builder()
                .title("Module 2")
                .content("Content")
                .course(course)
                .build());
        Long moduleId = module.getId();

        student.setLastOpenedModuleId(moduleId);
        student = userRepository.save(student);

        CodingTask codingTask = codingTaskRepository.save(CodingTask.builder()
                .title("Task 2")
                .description("Desc")
                .language("python")
                .testCasesJson("[]")
                .module(module)
                .build());
        Long codingTaskId = codingTask.getId();

        CodingSubmission codingSubmission = codingSubmissionRepository.save(CodingSubmission.builder()
                .user(student)
                .codingTask(codingTask)
                .codeSubmitted("print('hello')")
                .status(CodingSubmission.SubmissionStatus.PASSED)
                .build());
        Long codingSubmissionId = codingSubmission.getId();

        Assignment assignment = assignmentRepository.save(Assignment.builder()
                .title("Assignment 2")
                .description("Desc")
                .maxScore(100)
                .module(module)
                .build());
        Long assignmentId = assignment.getId();

        AssignmentSubmission assignmentSubmission = assignmentSubmissionRepository.save(AssignmentSubmission.builder()
                .user(student)
                .assignment(assignment)
                .submissionText("My submission")
                .status(AssignmentSubmission.SubmissionStatus.PENDING)
                .build());
        Long assignmentSubmissionId = assignmentSubmission.getId();

        PeerReview peerReview = peerReviewRepository.save(PeerReview.builder()
                .reviewer(teacher)
                .submission(assignmentSubmission)
                .score(90)
                .feedbackComment("Great job")
                .build());
        Long peerReviewId = peerReview.getId();

        // Delete module
        courseService.deleteModule(moduleId);

        // Verify cascades
        assertFalse(moduleRepository.existsById(moduleId));
        assertFalse(codingTaskRepository.existsById(codingTaskId));
        assertFalse(codingSubmissionRepository.existsById(codingSubmissionId));
        assertFalse(assignmentRepository.existsById(assignmentId));
        assertFalse(assignmentSubmissionRepository.existsById(assignmentSubmissionId));
        assertFalse(peerReviewRepository.existsById(peerReviewId));

        // Verify student's navigation state is nullified
        User updatedStudent = userRepository.findById(student.getId()).orElseThrow();
        assertNull(updatedStudent.getLastOpenedModuleId());
    }

    @Test
    @Transactional
    void testDeleteQuizCascadesAllAssociatedEntities() {
        User teacher = userRepository.save(User.builder()
                .username("teacher3")
                .password("pass")
                .role(Role.TEACHER)
                .fullName("Teacher Three")
                .build());

        User student = userRepository.save(User.builder()
                .username("student3")
                .password("pass")
                .role(Role.STUDENT)
                .fullName("Student Three")
                .build());

        Course course = courseRepository.save(Course.builder()
                .title("Course 3")
                .description("Desc")
                .content("Content")
                .teacher(teacher)
                .level("Beginner")
                .duration("1 Hour")
                .build());

        Quiz quiz = quizRepository.save(Quiz.builder()
                .title("Quiz 3")
                .course(course)
                .questions(new ArrayList<>())
                .build());
        Long quizId = quiz.getId();

        CourseModule module = moduleRepository.save(CourseModule.builder()
                .title("Module 3")
                .content("Content")
                .course(course)
                .quiz(quiz)
                .build());
        Long moduleId = module.getId();

        UserQuizResult quizResult = userQuizResultRepository.save(UserQuizResult.builder()
                .user(student)
                .quiz(quiz)
                .score(5)
                .completedAt(java.time.Instant.now())
                .build());
        Long quizResultId = quizResult.getId();

        // Delete quiz
        quizService.deleteQuiz(quizId);

        // Verify cascades
        assertFalse(quizRepository.existsById(quizId));
        assertFalse(userQuizResultRepository.existsById(quizResultId));

        // Verify module's quiz reference is nullified
        CourseModule updatedModule = moduleRepository.findById(moduleId).orElseThrow();
        assertNull(updatedModule.getQuiz());
    }
}
