package com.example.blog.controller;

import com.example.blog.dto.StudentCourseProgressDto;
import com.example.blog.entity.Course;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.Quiz;
import com.example.blog.entity.User;
import com.example.blog.entity.UserQuizResult;
import com.example.blog.entity.Role;
import com.example.blog.repository.UserRepository;
import com.example.blog.repository.UserQuizResultRepository;
import com.example.blog.repository.CourseRepository;
import com.example.blog.repository.QuizRepository;
import com.example.blog.service.CourseService;
import com.example.blog.service.QuizService;
import com.example.blog.service.NotificationService;
import com.example.blog.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final NotificationService notificationService;
    private final GamificationService gamificationService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
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

        List<UserQuizResult> allQuizResults = userQuizResultRepository.findByUserId(user.getId());
        
        int totalAttempts = allQuizResults.size();
        int passedQuizzesCount = 0;
        int totalPoints = 0;
        for (UserQuizResult res : allQuizResults) {
            totalPoints += res.getScore();
            if (res.getScore() >= 3) {
                passedQuizzesCount++;
            }
        }

        // Paginate for the recent quiz results table
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 5);
        org.springframework.data.domain.Page<UserQuizResult> quizResultsPage = userQuizResultRepository.findByUserIdOrderByIdDesc(user.getId(), pageable);
        
        model.addAttribute("quizResults", quizResultsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", quizResultsPage.getTotalPages());
        model.addAttribute("hasNext", quizResultsPage.hasNext());
        model.addAttribute("hasPrev", quizResultsPage.hasPrevious());
        
        model.addAttribute("totalAttempts", totalAttempts);
        model.addAttribute("passedQuizzesCount", passedQuizzesCount);
        model.addAttribute("totalPoints", user.getExperiencePoints() != null ? user.getExperiencePoints() : 0);
        
        // Calculate XP progress for premium UI
        int currentLevel = user.getLevel() != null ? user.getLevel() : 1;
        int requiredXp = Math.max(100, currentLevel * 100);
        int xpProgressPct = (model.getAttribute("totalPoints") != null ? (int)model.getAttribute("totalPoints") : 0) * 100 / requiredXp;
        model.addAttribute("requiredXp", requiredXp);
        model.addAttribute("xpProgressPct", xpProgressPct);
        
        model.addAttribute("unlockedAchievements", gamificationService.getUnlockedAchievements(user.getId()));
        
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
                for (UserQuizResult res : allQuizResults) {
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
            
            Long nextModuleId = null;
            for (CourseModule m : course.getModules()) {
                boolean completed = false;
                if (m.getQuiz() != null) {
                    completed = quizService.isQuizPassed(user.getId(), m.getQuiz().getId());
                } else {
                    if (user.getLastOpenedModuleId() != null) {
                        for (CourseModule other : course.getModules()) {
                            if (other.getId().equals(user.getLastOpenedModuleId())) {
                                if (other.getOrderIndex() >= m.getOrderIndex()) {
                                    completed = true;
                                }
                                break;
                            }
                        }
                    }
                }
                if (!completed) {
                    nextModuleId = m.getId();
                    break;
                }
            }
            if (nextModuleId == null && !course.getModules().isEmpty()) {
                nextModuleId = course.getModules().get(0).getId();
            }
            
            StudentCourseProgressDto dto = new StudentCourseProgressDto(course, pct, status, passedInCourse, totalCourseQuizzes, nextModuleId);
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
        
        // Add notifications
        model.addAttribute("notifications", notificationService.getRecentNotifications(user.getId(), 5));
        model.addAttribute("unreadNotificationsCount", notificationService.getUnreadCount(user.getId()));

        List<User> teachers = userRepository.findByRole(Role.TEACHER);
        model.addAttribute("teachers", teachers);

        return "student/dashboard";
    }

    @GetMapping("/teacher/{id}")
    public String viewTeacherProfile(@PathVariable Long id, Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        User student = userRepository.findByUsername(principal.getName()).orElse(null);
        if (student == null) {
            return "redirect:/admin/login";
        }

        User teacher = userRepository.findById(id).orElse(null);
        if (teacher == null || teacher.getRole() != Role.TEACHER) {
            return "redirect:/student/dashboard";
        }

        model.addAttribute("teacher", teacher);
        model.addAttribute("currentUserRole", "ROLE_" + student.getRole().name());
        model.addAttribute("currentUser", student.getUsername());

        return "student/teacher-profile";
    }

    @GetMapping("/dashboard/quiz-results")
    public String quizResultsFragment(@RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "partials/fragments :: quizResultsTable";
        }
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 5);
        org.springframework.data.domain.Page<UserQuizResult> quizResultsPage = userQuizResultRepository.findByUserIdOrderByIdDesc(user.getId(), pageable);
        
        model.addAttribute("quizResults", quizResultsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", quizResultsPage.getTotalPages());
        model.addAttribute("hasNext", quizResultsPage.hasNext());
        model.addAttribute("hasPrev", quizResultsPage.hasPrevious());
        
        return "student/dashboard :: quizResultsTable";
    }

    @PostMapping("/reset-stats")
    public String resetStats(Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/admin/login";
        }
        gamificationService.resetUserStats(user.getId());
        redirectAttributes.addFlashAttribute("success", "Ваша статистика успешно обнулена!");
        return "redirect:/student/dashboard";
    }
}
