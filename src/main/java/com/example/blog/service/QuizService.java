package com.example.blog.service;

import com.example.blog.entity.Quiz;
import com.example.blog.entity.Question;
import java.util.List;

public interface QuizService {
    List<Quiz> getQuizzesByCourseId(Long courseId);
    Quiz getQuizById(Long id);
    Quiz createQuiz(Long courseId, Quiz quiz);
    Quiz updateQuiz(Long id, Quiz quiz);
    void deleteQuiz(Long id);
    
    Question addQuestion(Long quizId, Question question);
    void deleteQuestion(Long questionId);
    
    void saveQuizResult(Long userId, Long quizId, int score);
    int getUserScore(Long userId, Long quizId);
    boolean isQuizPassed(Long userId, Long quizId);

    Quiz importQuizFromMarkdown(Long courseId, String content);
    void importQuestionsFromMarkdown(Long quizId, String content);

    Quiz importQuizFromGift(Long courseId, String content);
    void importQuestionsFromGift(Long quizId, String content);
}
