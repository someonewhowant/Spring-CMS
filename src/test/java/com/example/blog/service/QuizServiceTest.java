package com.example.blog.service;

import com.example.blog.entity.Quiz;
import com.example.blog.entity.Question;
import com.example.blog.entity.QuestionOption;
import com.example.blog.entity.User;
import com.example.blog.entity.UserQuizResult;
import com.example.blog.entity.Role;
import com.example.blog.repository.CourseRepository;
import com.example.blog.repository.QuestionRepository;
import com.example.blog.repository.QuizRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.repository.UserQuizResultRepository;
import com.example.blog.service.impl.QuizServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuizServiceTest {

    private QuizServiceImpl quizService;

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserQuizResultRepository userQuizResultRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private GamificationService gamificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        quizService = new QuizServiceImpl(quizRepository, questionRepository, courseRepository, userRepository, userQuizResultRepository, notificationService, gamificationService);
    }

    @Test
    void testImportQuizFromMarkdown() {
        String markdown = "# Test Quiz\n\n" +
                "## Question 1\n" +
                "- [ ] Option 1\n" +
                "- [x] Option 2\n" +
                "- [ ] Option 3\n\n" +
                "## Question 2\n" +
                "- [x] Correct\n" +
                "- [ ] Incorrect";

        when(courseRepository.findById(1L)).thenReturn(Optional.of(new com.example.blog.entity.Course()));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quiz quiz = quizService.importQuizFromMarkdown(1L, markdown);

        assertNotNull(quiz);
        assertEquals("Test Quiz", quiz.getTitle());
        assertEquals(2, quiz.getQuestions().size());

        Question q1 = quiz.getQuestions().get(0);
        assertEquals("Question 1", q1.getText());
        assertEquals(3, q1.getOptions().size());
        assertFalse(q1.getOptions().get(0).isCorrect());
        assertTrue(q1.getOptions().get(1).isCorrect());
        assertFalse(q1.getOptions().get(2).isCorrect());

        Question q2 = quiz.getQuestions().get(1);
        assertEquals("Question 2", q2.getText());
        assertEquals(2, q2.getOptions().size());
        assertTrue(q2.getOptions().get(0).isCorrect());
        assertFalse(q2.getOptions().get(1).isCorrect());
    }

    @Test
    void testImportQuizFromGift() {
        String gift = "::Java Basics::Which keyword is used to create a class in Java? {\n" +
                "    ~struct\n" +
                "    =class\n" +
                "    ~create\n" +
                "}\n" +
                "\n" +
                "What is the entry point of a Java program? {\n" +
                "    ~start()\n" +
                "    =main()\n" +
                "    ~init()\n" +
                "}";

        when(courseRepository.findById(1L)).thenReturn(Optional.of(new com.example.blog.entity.Course()));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quiz quiz = quizService.importQuizFromGift(1L, gift);

        assertNotNull(quiz);
        assertEquals("Java Basics", quiz.getTitle());
        assertEquals(2, quiz.getQuestions().size());

        Question q1 = quiz.getQuestions().get(0);
        assertEquals("Which keyword is used to create a class in Java?", q1.getText());
        assertEquals(3, q1.getOptions().size());
        assertFalse(q1.getOptions().get(0).isCorrect());
        assertTrue(q1.getOptions().get(1).isCorrect());

        Question q2 = quiz.getQuestions().get(1);
        assertEquals("What is the entry point of a Java program?", q2.getText());
        assertEquals(3, q2.getOptions().size());
        assertTrue(q2.getOptions().get(1).isCorrect());
    }

    @Test
    void testSaveQuizResult() {
        User user = User.builder()
                .id(1L)
                .username("student")
                .fullName("Test Student")
                .experiencePoints(0)
                .level(1)
                .build();
        
        Quiz quiz = Quiz.builder()
                .id(1L)
                .title("Test Quiz")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(userQuizResultRepository.findByUserIdAndQuizId(1L, 1L)).thenReturn(Optional.empty());
        when(userRepository.findByRole(Role.TEACHER)).thenReturn(new ArrayList<>());

        quizService.saveQuizResult(1L, 1L, 5);

        verify(userQuizResultRepository).save(any(UserQuizResult.class));
        verify(gamificationService).awardXp(eq(1L), eq(70), eq("Quiz Passed with Perfect Score"));
    }
}
