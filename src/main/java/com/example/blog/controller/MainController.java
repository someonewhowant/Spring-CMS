package com.example.blog.controller;

import com.example.blog.entity.*;
import com.example.blog.service.CommentService;
import com.example.blog.service.CourseService;
import com.example.blog.service.MarkdownService;
import com.example.blog.service.PostService;
import com.example.blog.service.QuizService;
import com.example.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final PostService postService;
    private final CourseService courseService;
    private final QuizService quizService;
    private final CommentService commentService;
    private final MarkdownService markdownService;
    private final UserRepository userRepository;
    private final com.example.blog.service.BookmarkService bookmarkService;

    /**
     * Главная страница блога с пагинацией.
     */
    @GetMapping("")
    public String index(Model model) {
        List<Post> posts = postService.getAllPosts().stream()
                .map(this::mapPostForDisplay)
                .collect(Collectors.toList());
        model.addAttribute("data", posts);
        
        // Добавляем последние курсы (топ 3)
        List<Course> recentCourses = courseService.getAllCourses().stream()
                .limit(3)
                .collect(Collectors.toList());
        model.addAttribute("recentCourses", recentCourses);
        
        model.addAttribute("title", "CodeBlog");
        model.addAttribute("description", "A modern blog platform built with Spring Boot 3.");
        return "index";
    }

    /**
     * Все статьи блога с пагинацией.
     */
    @GetMapping("/articles")
    public String articles(@RequestParam(defaultValue = "1") int page, Model model) {
        Page<Post> postPage = postService.getAllPosts(page);
        return populateModelAndReturn(postPage, page, "Articles", "Explore all insights from our blog.", model, "articles");
    }

    /**
     * Просмотр конкретного поста по ID.
     */
    @GetMapping("/post/{id}")
    public String post(@PathVariable Long id, Principal principal, Model model) {
        Post post = postService.getPostById(id);
        String htmlBody = markdownService.convertToHtml(post.getBody());
        
        Post displayPost = Post.builder()
                .id(post.getId())
                .title(post.getTitle())
                .body(htmlBody)
                .imageUrl(post.getImageUrl())
                .createdAt(post.getCreatedAt())
                .category(post.getCategory())
                .tags(post.getTags())
                .build();

        model.addAttribute("post", displayPost);
        model.addAttribute("title", post.getTitle());

        boolean isBookmarked = false;
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                isBookmarked = bookmarkService.isPostBookmarked(user.getId(), id);
            }
        }
        model.addAttribute("isBookmarked", isBookmarked);

        // Комментарии к статье
        List<Comment> comments = commentService.getCommentsByPostId(id);
        model.addAttribute("comments", comments);
        model.addAttribute("commentCount", commentService.getCommentCount(id));
        
        return "post";
    }

    /**
     * Добавление комментария к посту.
     */
    @PostMapping("/post/{id}/comment")
    public String addComment(@PathVariable Long id,
                             @RequestParam("body") String body,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/admin/login";
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Post post = postService.getPostById(id);

        String trimmedBody = body.trim();
        if (trimmedBody.isEmpty() || trimmedBody.length() > 2000) {
            redirectAttributes.addFlashAttribute("commentError", "Comment must be between 1 and 2000 characters.");
            return "redirect:/post/" + id;
        }

        commentService.addComment(post, user, trimmedBody);
        redirectAttributes.addFlashAttribute("commentSuccess", "Comment posted successfully!");
        return "redirect:/post/" + id + "#comments";
    }

    /**
     * Удаление комментария (автор или админ/преподаватель).
     */
    @PostMapping("/post/{postId}/comment/{commentId}/delete")
    public String deleteComment(@PathVariable Long postId,
                                @PathVariable Long commentId,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/admin/login";
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Comment comment = commentService.getCommentById(commentId);

        // Разрешаем удаление только автору или админу/преподавателю
        boolean isOwner = comment.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.TEACHER;
        if (!isOwner && !isAdmin) {
            redirectAttributes.addFlashAttribute("commentError", "You don't have permission to delete this comment.");
            return "redirect:/post/" + postId + "#comments";
        }

        commentService.deleteComment(commentId);
        redirectAttributes.addFlashAttribute("commentSuccess", "Comment deleted.");
        return "redirect:/post/" + postId + "#comments";
    }

    /**
     * Фильтрация по категории.
     */
    @GetMapping("/category/{slug}")
    public String category(@PathVariable String slug, 
                           @RequestParam(defaultValue = "1") int page, 
                           Model model) {
        Page<Post> postPage = postService.getPostsByCategory(slug, page);
        model.addAttribute("currentCategory", slug);
        return populateModelAndReturn(postPage, page, "Category: " + slug, null, model, "index");
    }

    /**
     * Фильтрация по тегу.
     */
    @GetMapping("/tag/{slug}")
    public String tag(@PathVariable String slug, 
                      @RequestParam(defaultValue = "1") int page, 
                      Model model) {
        Page<Post> postPage = postService.getPostsByTag(slug, page);
        return populateModelAndReturn(postPage, page, "Tag: " + slug, null, model, "index");
    }

    /**
     * Поиск постов по ключевому слову.
     */
    @GetMapping("/search")
    public String search(@RequestParam String searchTerm, 
                         @RequestParam(defaultValue = "1") int page, 
                         Model model) {
        Page<Post> searchResults = postService.searchPosts(searchTerm, page);
        List<Post> posts = searchResults.getContent().stream().map(this::mapPostForDisplay).collect(Collectors.toList());
        model.addAttribute("data", posts);
        
        List<Course> courses = courseService.searchCourses(searchTerm);
        model.addAttribute("courses", courses);
        
        Page<User> teacherResults = userRepository.findByRoleAndSearch(Role.TEACHER, searchTerm, org.springframework.data.domain.PageRequest.of(0, 50));
        model.addAttribute("teachers", teacherResults.getContent());

        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("title", "Search Results");
        
        return "search";
    }

    private String populateModelAndReturn(Page<Post> postPage, int page, String title, String description, Model model, String view) {
        List<Post> posts = postPage.getContent().stream().map(this::mapPostForDisplay).collect(Collectors.toList());

        model.addAttribute("data", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postPage.getTotalPages());
        model.addAttribute("title", title);
        if (description != null) {
            model.addAttribute("description", description);
        }
        
        return view;
    }

    private Post mapPostForDisplay(Post post) {
        return Post.builder()
                .id(post.getId())
                .title(post.getTitle())
                .body(markdownService.convertToHtml(post.getBody()).replaceAll("<[^>]*>", ""))
                .imageUrl(post.getImageUrl())
                .createdAt(post.getCreatedAt())
                .category(post.getCategory())
                .tags(post.getTags())
                .build();
    }

    /**
     * Страница "Courses".
     */
    @GetMapping("/courses")
    public String courses(Model model) {
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("title", "Courses");
        return "courses";
    }

    private void populateModuleStatuses(Course course, User user, Model model, Long currentModuleId) {
        Set<Long> completedModuleIds = new HashSet<>();
        Set<Long> lockedModuleIds = new HashSet<>();
        
        if (user != null) {
            List<CourseModule> modules = course.getModules();
            boolean previousCompleted = true;
            
            int lastOpenedOrder = -1;
            if (user.getLastOpenedCourseId() != null && user.getLastOpenedCourseId().equals(course.getId()) && user.getLastOpenedModuleId() != null) {
                for (CourseModule m : modules) {
                    if (m.getId().equals(user.getLastOpenedModuleId())) {
                        lastOpenedOrder = m.getOrderIndex();
                        break;
                    }
                }
            }
            
            for (int i = 0; i < modules.size(); i++) {
                CourseModule m = modules.get(i);
                
                if (!previousCompleted) {
                    lockedModuleIds.add(m.getId());
                }
                
                boolean isCompleted = false;
                if (!lockedModuleIds.contains(m.getId())) {
                    if (m.getQuiz() != null) {
                        isCompleted = quizService.isQuizPassed(user.getId(), m.getQuiz().getId());
                    } else {
                        if ((currentModuleId != null && m.getId().equals(currentModuleId)) || 
                            (lastOpenedOrder >= m.getOrderIndex())) {
                            isCompleted = true;
                        }
                    }
                }
                
                if (isCompleted) {
                    completedModuleIds.add(m.getId());
                }
                
                previousCompleted = !lockedModuleIds.contains(m.getId()) && (m.getQuiz() == null || isCompleted);
            }
        }
        
        model.addAttribute("completedModuleIds", completedModuleIds);
        model.addAttribute("lockedModuleIds", lockedModuleIds);
    }

    @GetMapping("/course/{id}")
    public String courseDetail(@PathVariable Long id, Principal principal, Model model) {
        Course course = courseService.getCourseById(id);
        String htmlContent = markdownService.convertToHtml(course.getContent());
        User user = null;
        boolean isBookmarked = false;

        if (principal != null) {
            user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                user.setLastOpenedCourseId(id);
                user.setLastOpenedModuleId(null);
                userRepository.save(user);
                isBookmarked = bookmarkService.isCourseBookmarked(user.getId(), id);
            }
        }
        
        model.addAttribute("isBookmarked", isBookmarked);
        populateModuleStatuses(course, user, model, null);
        
        model.addAttribute("course", course);
        model.addAttribute("htmlContent", htmlContent);
        model.addAttribute("title", course.getTitle());
        model.addAttribute("isOverview", true);
        model.addAttribute("isLocked", false);
        model.addAttribute("isNextLocked", false);
        return "course-detail";
    }

    /**
     * Страница конкретного модуля курса.
     */
    @GetMapping("/course/{courseId}/module/{moduleId}")
    public String moduleDetail(@PathVariable Long courseId, 
                               @PathVariable Long moduleId, 
                               Principal principal,
                               Model model) {
        Course course = courseService.getCourseById(courseId);
        CourseModule module = courseService.getModuleById(moduleId);
        
        List<CourseModule> modules = course.getModules();
        CourseModule nextModule = null;
        CourseModule prevModule = null;
        
        boolean isLocked = false;
        String lockReason = "";
        User user = null;
        
        if (principal != null) {
            user = userRepository.findByUsername(principal.getName()).orElse(null);
        }
        
        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).getId().equals(moduleId)) {
                if (i > 0) {
                    prevModule = modules.get(i - 1);
                    // Check if previous module had a quiz and if it was passed
                    if (prevModule.getQuiz() != null && user != null) {
                        if (!quizService.isQuizPassed(user.getId(), prevModule.getQuiz().getId())) {
                            isLocked = true;
                            lockReason = "You must score at least 3 points in the quiz for module: " + prevModule.getTitle();
                        }
                    }
                }
                if (i < modules.size() - 1) nextModule = modules.get(i + 1);
                break;
            }
        }

        if (user != null && !isLocked) {
            user.setLastOpenedCourseId(courseId);
            user.setLastOpenedModuleId(moduleId);
            userRepository.save(user);
        }
        
        populateModuleStatuses(course, user, model, moduleId);
        
        boolean isNextLocked = false;
        boolean isCurrentQuizPassed = false;
        if (module.getQuiz() != null && user != null) {
            isCurrentQuizPassed = quizService.isQuizPassed(user.getId(), module.getQuiz().getId());
            if (!isCurrentQuizPassed) {
                isNextLocked = true;
            }
        }
        
        String htmlContent = markdownService.convertToHtml(module.getContent());
        
        model.addAttribute("course", course);
        model.addAttribute("module", module);
        model.addAttribute("htmlContent", htmlContent);
        model.addAttribute("nextModule", nextModule);
        model.addAttribute("prevModule", prevModule);
        model.addAttribute("isLocked", isLocked);
        model.addAttribute("isNextLocked", isNextLocked);
        model.addAttribute("isCurrentQuizPassed", isCurrentQuizPassed);
        model.addAttribute("lockReason", lockReason);
        model.addAttribute("title", module.getTitle() + " - " + course.getTitle());
        model.addAttribute("isOverview", false);
        
        return "course-detail";
    }

    /**
     * Страница прохождения квиза.
     */
    @GetMapping("/quiz/{id}")
    public String takeQuiz(@PathVariable Long id, 
                           @RequestParam(required = false) Long moduleId,
                           Model model) {
        Quiz quiz = quizService.getQuizById(id);
        model.addAttribute("quiz", quiz);
        model.addAttribute("moduleId", moduleId);
        model.addAttribute("title", "Quiz: " + quiz.getTitle());
        return "quiz";
    }

    /**
     * Обработка результатов квиза.
     */
    @PostMapping("/quiz/{id}/submit")
    public String submitQuiz(@PathVariable Long id, 
                             @RequestParam(required = false) Long moduleId,
                             @RequestParam Map<String, String> params, 
                             Principal principal,
                             Model model) {
        Quiz quiz = quizService.getQuizById(id);
        int correctAnswers = 0;
        int totalQuestions = quiz.getQuestions().size();
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Question question : quiz.getQuestions()) {
            String selectedOptionId = params.get("question_" + question.getId());
            boolean isCorrect = false;
            String correctAnswerText = "";
            String selectedAnswerText = "No answer";
            
            for (QuestionOption option : question.getOptions()) {
                if (option.isCorrect()) {
                    correctAnswerText = option.getText();
                }
                if (selectedOptionId != null && selectedOptionId.equals(option.getId().toString())) {
                    selectedAnswerText = option.getText();
                    if (option.isCorrect()) {
                        isCorrect = true;
                        correctAnswers++;
                    }
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("question", question.getText());
            result.put("selected", selectedAnswerText);
            result.put("correct", correctAnswerText);
            result.put("isCorrect", isCorrect);
            results.add(result);
        }
        
        boolean isPassed = false;
        // Save results if user is logged in
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            quizService.saveQuizResult(user.getId(), quiz.getId(), correctAnswers);
            isPassed = correctAnswers >= 3;
            model.addAttribute("isPassed", isPassed);
        }
        
        if (isPassed && moduleId != null) {
            CourseModule module = courseService.getModuleById(moduleId);
            Course course = module.getCourse();
            List<CourseModule> modules = course.getModules();
            CourseModule nextModule = null;
            for (int i = 0; i < modules.size(); i++) {
                if (modules.get(i).getId().equals(moduleId)) {
                    if (i < modules.size() - 1) {
                        nextModule = modules.get(i + 1);
                    }
                    break;
                }
            }
            model.addAttribute("nextModule", nextModule);
            model.addAttribute("courseId", course.getId());
        }
        
        model.addAttribute("quiz", quiz);
        model.addAttribute("moduleId", moduleId);
        model.addAttribute("score", correctAnswers);
        model.addAttribute("total", totalQuestions);
        model.addAttribute("results", results);
        model.addAttribute("title", "Results: " + quiz.getTitle());
        model.addAttribute("submitted", true);
        
        return "quiz";
    }

    /**
     * Статическая страница "About".
     */
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "About Us");
        return "about";
    }
}
