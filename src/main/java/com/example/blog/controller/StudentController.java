package com.example.blog.controller;

import com.example.blog.entity.Course;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.Quiz;
import com.example.blog.entity.User;
import com.example.blog.entity.UserQuizResult;
import com.example.blog.repository.UserRepository;
import com.example.blog.repository.UserQuizResultRepository;
import com.example.blog.repository.CourseRepository;
import com.example.blog.repository.QuizRepository;
import com.example.blog.service.CourseService;
import com.example.blog.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final UserRepository userRepository;
    private final UserQuizResultRepository userQuizResultRepository;
    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final CourseService courseService;
    private final QuizService quizService;

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/admin/login";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user.getUsername());
        model.addAttribute("currentUserRole", "ROLE_" + user.getRole().name());

        List<UserQuizResult> quizResults = userQuizResultRepository.findByUserId(user.getId());
        model.addAttribute("quizResults", quizResults);
        
        int totalAttempts = quizResults.size();
        int passedQuizzesCount = 0;
        int totalPoints = 0;
        for (UserQuizResult res : quizResults) {
            totalPoints += res.getScore();
            if (res.getScore() >= 3) {
                passedQuizzesCount++;
            }
        }
        
        model.addAttribute("totalAttempts", totalAttempts);
        model.addAttribute("passedQuizzesCount", passedQuizzesCount);
        model.addAttribute("totalPoints", totalPoints);
        
        double averageScore = 0.0;
        if (totalAttempts > 0) {
            averageScore = Math.round(((double) totalPoints / totalAttempts) * 10.0) / 10.0;
        }
        model.addAttribute("averageScore", averageScore);

        // Performance rating logic
        String performanceRating = "NO_SUBMISSIONS";
        if (totalAttempts >= 5 && averageScore >= 4.5) performanceRating = "ELITE_DEV";
        else if (totalAttempts >= 3 && averageScore >= 3.5) performanceRating = "ADVANCED_CODER";
        else if (totalAttempts > 0) performanceRating = "APPRENTICE";
        model.addAttribute("performanceRating", performanceRating);

        long totalCoursesCount = courseRepository.count();
        long totalQuizzesCount = quizRepository.count();
        model.addAttribute("totalCoursesCount", totalCoursesCount);
        model.addAttribute("totalQuizzesCount", totalQuizzesCount);

        // Fetch last opened course and module for Continue Learning widget
        Course lastCourse = null;
        CourseModule lastModule = null;
        if (user.getLastOpenedCourseId() != null) {
            try {
                lastCourse = courseService.getCourseById(user.getLastOpenedCourseId());
                if (user.getLastOpenedModuleId() != null) {
                    lastModule = courseService.getModuleById(user.getLastOpenedModuleId());
                    if (lastModule == null || lastModule.getCourse() == null || !lastModule.getCourse().getId().equals(lastCourse.getId())) {
                        lastModule = null;
                    }
                }
            } catch (Exception e) {
                lastCourse = null;
                lastModule = null;
            }
        }
        model.addAttribute("lastCourse", lastCourse);
        model.addAttribute("lastModule", lastModule);

        // Course classification and progress logic
        List<Course> allCourses = courseService.getAllCourses();
        List<StudentCourseProgressDto> activeCourses = new java.util.ArrayList<>();
        List<StudentCourseProgressDto> completedCourses = new java.util.ArrayList<>();
        List<StudentCourseProgressDto> availableCourses = new java.util.ArrayList<>();

        for (Course course : allCourses) {
            List<Quiz> courseQuizzes = quizRepository.findByCourseId(course.getId());
            int totalCourseQuizzes = courseQuizzes.size();
            int passedInCourse = 0;
            boolean hasAttemptsInCourse = false;
            
            for (Quiz q : courseQuizzes) {
                for (UserQuizResult res : quizResults) {
                    if (res.getQuiz().getId().equals(q.getId())) {
                        hasAttemptsInCourse = true;
                        if (res.getScore() >= 3) {
                            passedInCourse++;
                        }
                        break;
                    }
                }
            }
            
            int pct = 0;
            String status = "AVAILABLE";
            
            if (totalCourseQuizzes > 0) {
                pct = (passedInCourse * 100) / totalCourseQuizzes;
                if (pct == 100) {
                    status = "COMPLETED";
                } else if (hasAttemptsInCourse || (user.getLastOpenedCourseId() != null && user.getLastOpenedCourseId().equals(course.getId()))) {
                    status = "ACTIVE";
                }
            } else {
                // No quizzes, check if any module was opened
                if (user.getLastOpenedCourseId() != null && user.getLastOpenedCourseId().equals(course.getId())) {
                    List<CourseModule> modules = course.getModules();
                    if (!modules.isEmpty() && user.getLastOpenedModuleId() != null) {
                        if (user.getLastOpenedModuleId().equals(modules.get(modules.size() - 1).getId())) {
                            pct = 100;
                            status = "COMPLETED";
                        } else {
                            pct = 50; // Simple fallback
                            status = "ACTIVE";
                        }
                    } else {
                        pct = 10;
                        status = "ACTIVE";
                    }
                }
            }
            
            StudentCourseProgressDto dto = new StudentCourseProgressDto(course, pct, status, passedInCourse, totalCourseQuizzes);
            if ("COMPLETED".equals(status)) {
                completedCourses.add(dto);
            } else if ("ACTIVE".equals(status)) {
                activeCourses.add(dto);
            } else {
                availableCourses.add(dto);
            }
        }
        
        model.addAttribute("activeCourses", activeCourses);
        model.addAttribute("completedCourses", completedCourses);
        model.addAttribute("availableCourses", availableCourses);

        model.addAttribute("message", "Добро пожаловать в командный центр студента!");
        model.addAttribute("title", "Student Cabinet");
        
        return "student/dashboard";
    }

    @lombok.Getter
    @lombok.RequiredArgsConstructor
    public static class StudentCourseProgressDto {
        private final Course course;
        private final int progressPercentage;
        private final String status;
        private final int passedQuizzesCount;
        private final int totalQuizzesCount;
    }
}
