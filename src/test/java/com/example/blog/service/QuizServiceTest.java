package com.example.blog.service;

import com.example.blog.entity.Quiz;
import com.example.blog.entity.Question;
import com.example.blog.entity.QuestionOption;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        quizService = new QuizServiceImpl(quizRepository, questionRepository, courseRepository, userRepository, userQuizResultRepository);
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
}
