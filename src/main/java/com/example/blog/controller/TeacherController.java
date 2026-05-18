package com.example.blog.controller;

import com.example.blog.entity.Course;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.Question;
import com.example.blog.entity.QuestionOption;
import com.example.blog.entity.Quiz;
import com.example.blog.service.CourseService;
import com.example.blog.service.FileStorageService;
import com.example.blog.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.blog.repository.UserRepository;
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

        model.addAttribute("message", "Добро пожаловать в командный центр преподавателя!");
        model.addAttribute("title", "Teacher Cabinet");
        model.addAttribute("coursesCount", totalCourses);
        model.addAttribute("quizzesCount", totalQuizzes);
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
                            @RequestParam(value = "markdownFile", required = false) MultipartFile markdownFile) throws IOException {
        if (!image.isEmpty()) {
            course.setImageUrl(fileStorageService.storeFile(image));
        }
        if (markdownFile != null && !markdownFile.isEmpty()) {
            course.setContent(new String(markdownFile.getBytes(), StandardCharsets.UTF_8));
        }
        courseService.createCourse(course);
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
        
        courseService.addModule(id, module);
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
        if (file != null && !file.isEmpty()) {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String fileName = file.getOriginalFilename();
            Quiz importedQuiz;
            if (fileName != null && fileName.toLowerCase().endsWith(".gift")) {
                importedQuiz = quizService.importQuizFromGift(id, content);
            } else {
                importedQuiz = quizService.importQuizFromMarkdown(id, content);
            }
            
            if (quiz.getTitle() != null && !quiz.getTitle().isEmpty()) {
                importedQuiz.setTitle(quiz.getTitle());
                quizService.updateQuiz(importedQuiz.getId(), importedQuiz);
            }
        } else {
            quizService.createQuiz(id, quiz);
        }
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
}
