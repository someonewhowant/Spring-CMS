package com.example.blog.controller;

import com.example.blog.dto.StudentCourseProgressDto;
import com.example.blog.entity.Course;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.Question;
import com.example.blog.entity.QuestionOption;
import com.example.blog.entity.Quiz;
import com.example.blog.entity.UserQuizResult;
import com.example.blog.entity.Role;
import com.example.blog.repository.CourseRepository;
import com.example.blog.repository.QuizRepository;
import com.example.blog.service.CourseService;
import com.example.blog.service.FileStorageService;
import com.example.blog.service.QuizService;
import com.example.blog.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.blog.repository.UserRepository;
import com.example.blog.repository.UserQuizResultRepository;
import java.security.Principal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final CourseService courseService;
    private final QuizService quizService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final UserQuizResultRepository userQuizResultRepository;
    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final NotificationService notificationService;
    private final com.example.blog.service.GamificationService gamificationService;

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        com.example.blog.entity.User user = userRepository.findByUsername(principal.getName()).orElse(null);
        model.addAttribute("user", user);

        java.util.List<Course> courses = courseService.getAllCourses();
        int totalCourses = courses.size();
        int totalQuizzes = 0;
        for (Course c : courses) {
            totalQuizzes += quizService.getQuizzesByCourseId(c.getId()).size();
        }

        List<com.example.blog.entity.User> allStudents = userRepository.findByRole(Role.STUDENT);
        int totalStudents = allStudents.size();

        // Fetch top students
        List<StudentProgressDto> topStudents = new ArrayList<>();
        for (com.example.blog.entity.User student : allStudents) {
            List<UserQuizResult> results = userQuizResultRepository.findByUserId(student.getId());
            if (!results.isEmpty()) {
                double total = 0.0;
                for (UserQuizResult res : results) {
                    total += res.getScore();
                }
                double avg = Math.round((total / results.size()) * 10.0) / 10.0;
                topStudents.add(new StudentProgressDto(student, results.size(), avg, results));
            }
        }
        topStudents.sort((a, b) -> Double.compare(b.getAverageScore(), a.getAverageScore()));
        if (topStudents.size() > 5) {
            topStudents = topStudents.subList(0, 5);
        }
        model.addAttribute("topStudents", topStudents);

        model.addAttribute("message", "Добро пожаловать в командный центр преподавателя!");
        model.addAttribute("title", "Teacher Cabinet");
        model.addAttribute("coursesCount", totalCourses);
        model.addAttribute("quizzesCount", totalQuizzes);
        model.addAttribute("studentsCount", totalStudents);
        model.addAttribute("currentUser", principal.getName());
        model.addAttribute("currentUserRole", "ROLE_" + (user != null ? user.getRole().name() : "TEACHER"));
        
        // Add notifications
        if (user != null) {
            model.addAttribute("notifications", notificationService.getRecentNotifications(user.getId(), 5));
            model.addAttribute("unreadNotificationsCount", notificationService.getUnreadCount(user.getId()));
        }

        return "teacher/dashboard";
    }

    /**
     * Управление курсами.
     */
    @GetMapping("/courses")
    public String manageCourses(Model model) {
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("title", "Manage Courses");
        return "teacher/courses";
    }

    /**
     * Форма добавления курса.
     */
    @GetMapping("/add-course")
    public String addCourseForm(Model model) {
        model.addAttribute("title", "Add Course");
        return "teacher/add-course";
    }

    /**
     * Обработка создания курса.
     */
    @PostMapping("/add-course")
    public String addCourse(@ModelAttribute Course course, 
                            @RequestParam("image") MultipartFile image,
                            @RequestParam(value = "markdownFile", required = false) MultipartFile markdownFile,
                            Principal principal) throws IOException {
        com.example.blog.entity.User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (!image.isEmpty()) {
            course.setImageUrl(fileStorageService.storeFile(image));
        }
        if (markdownFile != null && !markdownFile.isEmpty()) {
            course.setContent(new String(markdownFile.getBytes(), StandardCharsets.UTF_8));
        }
        courseService.createCourse(course, user);
        return "redirect:/teacher/courses";
    }

    /**
     * Форма редактирования курса.
     */
    @GetMapping("/edit-course/{id}")
    public String editCourseForm(@PathVariable Long id, Model model) {
        model.addAttribute("course", courseService.getCourseById(id));
        model.addAttribute("title", "Edit Course");
        return "teacher/edit-course";
    }

    /**
     * Обработка обновления курса.
     */
    @PostMapping("/edit-course/{id}")
    public String updateCourse(@PathVariable Long id, 
                               @ModelAttribute Course course, 
                               @RequestParam("image") MultipartFile image,
                               @RequestParam(value = "markdownFile", required = false) MultipartFile markdownFile) throws IOException {
        if (!image.isEmpty()) {
            course.setImageUrl(fileStorageService.storeFile(image));
        }
        if (markdownFile != null && !markdownFile.isEmpty()) {
            course.setContent(new String(markdownFile.getBytes(), StandardCharsets.UTF_8));
        }
        courseService.updateCourse(id, course);
        return "redirect:/teacher/courses";
    }

    /**
     * Удаление курса.
     */
    @GetMapping("/delete-course/{id}")
    public String deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return "redirect:/teacher/courses";
    }

    /**
     * Управление модулями курса.
     */
    @GetMapping("/courses/{id}/modules")
    public String manageModules(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id);
        model.addAttribute("course", course);
        model.addAttribute("modules", courseService.getModulesByCourseId(id));
        model.addAttribute("quizzes", quizService.getQuizzesByCourseId(id));
        model.addAttribute("title", "Manage Modules: " + course.getTitle());
        return "teacher/modules";
    }

    /**
     * Установка квиза для модуля.
     */
    @PostMapping("/courses/{courseId}/modules/{moduleId}/quiz")
    public String setModuleQuiz(@PathVariable Long courseId,
                                @PathVariable Long moduleId,
                                @RequestParam(value = "quizId", required = false) Long quizId) {
        courseService.setModuleQuiz(moduleId, quizId);
        return "redirect:/teacher/courses/" + courseId + "/modules";
    }

    /**
     * Добавление модуля.
     */
    @PostMapping("/courses/{id}/modules")
    public String addModule(@PathVariable Long id,
                            @RequestParam("title") String title,
                            @RequestParam(value = "content", required = false) String content,
                            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        CourseModule module = new CourseModule();
        module.setTitle(title);
        
        if (file != null && !file.isEmpty()) {
            module.setContent(new String(file.getBytes(), StandardCharsets.UTF_8));
        } else {
            module.setContent(content);
        }
        
        CourseModule savedModule = courseService.addModule(id, module);
        
        // Notify all students about the new module
        String message = "New module added to course: " + savedModule.getCourse().getTitle() + " - " + title;
        String link = "/course/" + id + "/module/" + savedModule.getId();
        userRepository.findByRole(Role.STUDENT).forEach(student -> 
            notificationService.createNotification(student, message, link)
        );
        
        return "redirect:/teacher/courses/" + id + "/modules";
    }

    /**
     * Форма редактирования модуля.
     */
    @GetMapping("/courses/{courseId}/modules/{moduleId}/edit")
    public String editModuleForm(@PathVariable Long courseId, @PathVariable Long moduleId, Model model) {
        Course course = courseService.getCourseById(courseId);
        CourseModule module = courseService.getModuleById(moduleId);
        model.addAttribute("course", course);
        model.addAttribute("module", module);
        model.addAttribute("title", "Edit Module: " + module.getTitle());
        return "teacher/edit-module";
    }

    /**
     * Обработка обновления модуля.
     */
    @PostMapping("/courses/{courseId}/modules/{moduleId}/edit")
    public String updateModule(@PathVariable Long courseId,
                               @PathVariable Long moduleId,
                               @RequestParam("title") String title,
                               @RequestParam(value = "content", required = false) String content,
                               @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        CourseModule module = new CourseModule();
        module.setTitle(title);

        if (file != null && !file.isEmpty()) {
            module.setContent(new String(file.getBytes(), StandardCharsets.UTF_8));
        } else {
            module.setContent(content);
        }

        courseService.updateModule(moduleId, module);
        return "redirect:/teacher/courses/" + courseId + "/modules";
    }

    /**
     * Удаление модуля.
     */
    @GetMapping("/delete-module/{courseId}/{moduleId}")
    public String deleteModule(@PathVariable Long courseId, @PathVariable Long moduleId) {
        courseService.deleteModule(moduleId);
        return "redirect:/teacher/courses/" + courseId + "/modules";
    }

    /**
     * Управление квизами курса.
     */
    @GetMapping("/courses/{id}/quizzes")
    public String manageQuizzes(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id);
        model.addAttribute("course", course);
        model.addAttribute("quizzes", quizService.getQuizzesByCourseId(id));
        model.addAttribute("title", "Manage Quizzes: " + course.getTitle());
        return "teacher/quizzes";
    }

    /**
     * Добавление квиза.
     */
    @PostMapping("/courses/{id}/quizzes")
    public String addQuiz(@PathVariable Long id, 
                          @ModelAttribute Quiz quiz,
                          @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        Quiz savedQuiz;
        if (file != null && !file.isEmpty()) {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String fileName = file.getOriginalFilename();
            if (fileName != null && fileName.toLowerCase().endsWith(".gift")) {
                savedQuiz = quizService.importQuizFromGift(id, content);
            } else {
                savedQuiz = quizService.importQuizFromMarkdown(id, content);
            }
            
            if (quiz.getTitle() != null && !quiz.getTitle().isEmpty()) {
                savedQuiz.setTitle(quiz.getTitle());
                quizService.updateQuiz(savedQuiz.getId(), savedQuiz);
            }
        } else {
            savedQuiz = quizService.createQuiz(id, quiz);
        }

        // Notify all students about the new quiz
        String message = "New interactive quiz available in course: " + savedQuiz.getCourse().getTitle() + " - " + savedQuiz.getTitle();
        String link = "/course/" + id;
        userRepository.findByRole(Role.STUDENT).forEach(student -> 
            notificationService.createNotification(student, message, link)
        );

        return "redirect:/teacher/courses/" + id + "/quizzes";
    }

    /**
     * Удаление квиза.
     */
    @GetMapping("/delete-quiz/{courseId}/{quizId}")
    public String deleteQuiz(@PathVariable Long courseId, @PathVariable Long quizId) {
        quizService.deleteQuiz(quizId);
        return "redirect:/teacher/courses/" + courseId + "/quizzes";
    }

    /**
     * Управление вопросами квиза.
     */
    @GetMapping("/quizzes/{id}/questions")
    public String manageQuestions(@PathVariable Long id, Model model) {
        Quiz quiz = quizService.getQuizById(id);
        model.addAttribute("quiz", quiz);
        model.addAttribute("title", "Manage Questions: " + quiz.getTitle());
        return "teacher/questions";
    }

    /**
     * Добавление вопроса.
     */
    @PostMapping("/quizzes/{id}/questions")
    public String addQuestion(@PathVariable Long id,
                              @RequestParam(value = "text", required = false) String text,
                              @RequestParam(value = "optionText", required = false) List<String> optionTexts,
                              @RequestParam(value = "correctOptionIndex", required = false) Integer correctIndex,
                              @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        
        if (file != null && !file.isEmpty()) {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String fileName = file.getOriginalFilename();
            if (fileName != null && fileName.toLowerCase().endsWith(".gift")) {
                quizService.importQuestionsFromGift(id, content);
            } else {
                quizService.importQuestionsFromMarkdown(id, content);
            }
        } else if (text != null && optionTexts != null && correctIndex != null) {
            Question question = new Question();
            question.setText(text);
            
            List<QuestionOption> options = new ArrayList<>();
            for (int i = 0; i < optionTexts.size(); i++) {
                QuestionOption option = new QuestionOption();
                option.setText(optionTexts.get(i));
                option.setCorrect(i == correctIndex);
                option.setQuestion(question);
                options.add(option);
            }
            question.setOptions(options);
            
            quizService.addQuestion(id, question);
        }
        
        return "redirect:/teacher/quizzes/" + id + "/questions";
    }

    /**
     * Удаление вопроса.
     */
    @GetMapping("/delete-question/{quizId}/{questionId}")
    public String deleteQuestion(@PathVariable Long quizId, @PathVariable Long questionId) {
        quizService.deleteQuestion(questionId);
        return "redirect:/teacher/quizzes/" + quizId + "/questions";
    }

    @GetMapping("/students/{id}")
    public String viewStudentProfile(@PathVariable Long id, Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        com.example.blog.entity.User teacher = userRepository.findByUsername(principal.getName()).orElse(null);
        com.example.blog.entity.User student = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Student not found"));

        model.addAttribute("user", teacher);
        model.addAttribute("student", student);
        model.addAttribute("currentUser", principal.getName());
        model.addAttribute("currentUserRole", "ROLE_" + (teacher != null ? teacher.getRole().name() : "TEACHER"));
        model.addAttribute("title", "Student Profile: " + student.getFullName());

        List<UserQuizResult> quizResults = userQuizResultRepository.findByUserId(student.getId());
        model.addAttribute("quizResults", quizResults);

        // Calculate detailed course progress summary
        List<StudentCourseProgressDto> courseProgress = new ArrayList<>();
        List<Course> allCourses = courseService.getAllCourses();

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
                if (pct == 100) status = "COMPLETED";
                else if (hasAttemptsInCourse || (student.getLastOpenedCourseId() != null && student.getLastOpenedCourseId().equals(course.getId()))) status = "ACTIVE";
            } else {
                if (student.getLastOpenedCourseId() != null && student.getLastOpenedCourseId().equals(course.getId())) {
                    List<CourseModule> modules = course.getModules();
                    if (!modules.isEmpty() && student.getLastOpenedModuleId() != null) {
                        if (student.getLastOpenedModuleId().equals(modules.get(modules.size() - 1).getId())) {
                            pct = 100;
                            status = "COMPLETED";
                        } else {
                            pct = 50;
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
                    completed = quizService.isQuizPassed(student.getId(), m.getQuiz().getId());
                } else {
                    if (student.getLastOpenedModuleId() != null) {
                        for (CourseModule other : course.getModules()) {
                            if (other.getId().equals(student.getLastOpenedModuleId())) {
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
            
            courseProgress.add(new StudentCourseProgressDto(course, pct, status, passedInCourse, totalCourseQuizzes, nextModuleId));
        }

        model.addAttribute("courseProgress", courseProgress);

        return "teacher/student-profile";
    }

    /**
     * Просмотр студентов Академии и метрик их успеваемости.
     */
    @GetMapping("/students")
    public String browseStudents(Principal principal,
                                 @RequestParam(value = "search", required = false) String search,
                                 Model model) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        com.example.blog.entity.User user = userRepository.findByUsername(principal.getName()).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("currentUser", principal.getName());
        model.addAttribute("currentUserRole", "ROLE_" + (user != null ? user.getRole().name() : "TEACHER"));
        model.addAttribute("title", "Browse Students");

        List<com.example.blog.entity.User> allStudents = userRepository.findByRole(Role.STUDENT);
        List<StudentProgressDto> studentProgressList = new ArrayList<>();

        for (com.example.blog.entity.User student : allStudents) {
            // Фильтр поиска
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase().trim();
                boolean matchesName = student.getFullName() != null && student.getFullName().toLowerCase().contains(searchLower);
                boolean matchesUsername = student.getUsername() != null && student.getUsername().toLowerCase().contains(searchLower);
                boolean matchesEmail = student.getEmail() != null && student.getEmail().toLowerCase().contains(searchLower);
                if (!matchesName && !matchesUsername && !matchesEmail) {
                    continue;
                }
            }

            List<UserQuizResult> results = userQuizResultRepository.findByUserId(student.getId());
            int completedCount = results.size();
            double avgScore = 0.0;
            if (completedCount > 0) {
                double total = 0.0;
                for (UserQuizResult res : results) {
                    total += res.getScore();
                }
                avgScore = Math.round((total / completedCount) * 10.0) / 10.0;
            }
            studentProgressList.add(new StudentProgressDto(student, completedCount, avgScore, results));
        }

        model.addAttribute("students", studentProgressList);
        model.addAttribute("searchQuery", search);
        return "teacher/students";
    }

    @PostMapping("/reset-stats")
    public String resetAllStudentStats(Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        gamificationService.resetAllStudentStats();
        redirectAttributes.addFlashAttribute("success", "Вся статистика студентов академии успешно сброшена!");
        return "redirect:/teacher/dashboard";
    }

    @PostMapping("/students/{id}/reset-stats")
    public String resetSingleStudentStats(@PathVariable Long id, Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/admin/login";
        }
        gamificationService.resetUserStats(id);
        redirectAttributes.addFlashAttribute("success", "Статистика студента успешно сброшена!");
        return "redirect:/teacher/students/" + id;
    }

    @lombok.Getter
    @lombok.RequiredArgsConstructor
    public static class StudentProgressDto {
        private final com.example.blog.entity.User student;
        private final int completedQuizzesCount;
        private final double averageScore;
        private final List<UserQuizResult> quizResults;

        public String getPerformanceRating() {
            if (completedQuizzesCount == 0) return "NO_SUBMISSIONS";
            if (completedQuizzesCount >= 5 && averageScore >= 4.5) return "ELITE_DEV";
            if (completedQuizzesCount >= 3 && averageScore >= 3.5) return "ADVANCED_CODER";
            return "APPRENTICE";
        }
    }
}
