package com.example.blog.service.impl;

import com.example.blog.entity.Course;
import com.example.blog.entity.CourseModule;
import com.example.blog.entity.Quiz;
import com.example.blog.entity.User;
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
import com.example.blog.service.CourseService;
import com.example.blog.service.GamificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CodingTaskRepository codingTaskRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final PeerReviewRepository peerReviewRepository;
    private final UserQuizResultRepository userQuizResultRepository;
    private final GamificationService gamificationService;

    @Override
    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        courses.forEach(c -> {
            c.getModules().forEach(m -> {
                if (m.getQuiz() != null) {
                    m.getQuiz().getTitle();
                }
            });
            c.getQuizzes().forEach(q -> {
                q.getQuestions().forEach(question -> {
                    question.getOptions().size();
                });
            });
        });
        return courses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Course> searchCourses(String keyword) {
        List<Course> courses = courseRepository.searchByKeyword(keyword);
        courses.forEach(c -> {
            c.getModules().forEach(m -> {
                if (m.getQuiz() != null) {
                    m.getQuiz().getTitle();
                }
            });
            c.getQuizzes().forEach(q -> {
                q.getQuestions().forEach(question -> {
                    question.getOptions().size();
                });
            });
        });
        return courses;
    }

    @Override
    public Course getCourseById(Long id) {
        return courseRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Course getCourseWithDetails(Long id) {
        Course course = getCourseById(id);
        course.getModules().forEach(m -> {
            if (m.getQuiz() != null) {
                m.getQuiz().getTitle();
                if (m.getQuiz().getQuestions() != null) {
                    m.getQuiz().getQuestions().forEach(q -> {
                        if (q.getOptions() != null) {
                            q.getOptions().size();
                        }
                    });
                }
            }
        });
        course.getQuizzes().forEach(q -> {
            q.getQuestions().forEach(question -> {
                if (question.getOptions() != null) {
                    question.getOptions().size();
                }
            });
        });
        return course;
    }

    @Override
    @Transactional
    public Course createCourse(Course course, User teacher) {
        course.setTeacher(teacher);
        Course saved = courseRepository.save(course);
        gamificationService.evaluateAchievements(teacher.getId());
        return saved;
    }

    @Override
    @Transactional
    public Course updateCourse(Long id, Course courseDetails) {
        Course course = getCourseById(id);
        course.setTitle(courseDetails.getTitle());
        course.setDescription(courseDetails.getDescription());
        course.setContent(courseDetails.getContent());
        course.setImageUrl(courseDetails.getImageUrl());
        course.setLevel(courseDetails.getLevel());
        course.setDuration(courseDetails.getDuration());
        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        bookmarkRepository.deleteByCourseId(id);

        userRepository.findAll().forEach(user -> {
            boolean updated = false;
            if (user.getLastOpenedCourseId() != null && user.getLastOpenedCourseId().equals(id)) {
                user.setLastOpenedCourseId(null);
                updated = true;
            }
            if (user.getLastOpenedModuleId() != null) {
                final Long modId = user.getLastOpenedModuleId();
                boolean belongs = moduleRepository.findById(modId)
                        .map(m -> m.getCourse().getId().equals(id))
                        .orElse(false);
                if (belongs) {
                    user.setLastOpenedModuleId(null);
                    updated = true;
                }
            }
            if (updated) {
                userRepository.save(user);
            }
        });

        List<CourseModule> modules = moduleRepository.findByCourseIdOrderByOrderIndexAsc(id);
        for (CourseModule module : modules) {
            List<CodingTask> tasks = codingTaskRepository.findByModuleId(module.getId());
            for (CodingTask task : tasks) {
                List<CodingSubmission> submissions = codingSubmissionRepository.findByCodingTaskId(task.getId());
                codingSubmissionRepository.deleteAll(submissions);
                codingTaskRepository.delete(task);
            }
            
            List<Assignment> assignments = assignmentRepository.findByModuleId(module.getId());
            for (Assignment assignment : assignments) {
                List<AssignmentSubmission> submissions = assignmentSubmissionRepository.findByAssignmentId(assignment.getId());
                for (AssignmentSubmission submission : submissions) {
                    List<PeerReview> reviews = peerReviewRepository.findBySubmissionId(submission.getId());
                    peerReviewRepository.deleteAll(reviews);
                    assignmentSubmissionRepository.delete(submission);
                }
                assignmentRepository.delete(assignment);
            }
        }

        List<Quiz> quizzes = quizRepository.findByCourseId(id);
        for (Quiz quiz : quizzes) {
            List<UserQuizResult> results = userQuizResultRepository.findByQuizId(quiz.getId());
            userQuizResultRepository.deleteAll(results);
        }

        moduleRepository.deleteAll(modules);
        quizRepository.deleteAll(quizzes);

        Course course = getCourseById(id);
        courseRepository.delete(course);
    }

    @Override
    @Transactional
    public CourseModule addModule(Long courseId, CourseModule module) {
        Course course = getCourseById(courseId);
        module.setCourse(course);

        // Auto-set order index if not set
        if (module.getOrderIndex() == 0) {
            int maxOrder =
                    course.getModules().stream()
                            .mapToInt(CourseModule::getOrderIndex)
                            .max()
                            .orElse(-1);
            module.setOrderIndex(maxOrder + 1);
        }

        return moduleRepository.save(module);
    }

    @Override
    @Transactional
    public void deleteModule(Long moduleId) {
        userRepository.findAll().forEach(user -> {
            if (user.getLastOpenedModuleId() != null && user.getLastOpenedModuleId().equals(moduleId)) {
                user.setLastOpenedModuleId(null);
                userRepository.save(user);
            }
        });

        List<CodingTask> tasks = codingTaskRepository.findByModuleId(moduleId);
        for (CodingTask task : tasks) {
            List<CodingSubmission> submissions = codingSubmissionRepository.findByCodingTaskId(task.getId());
            codingSubmissionRepository.deleteAll(submissions);
            codingTaskRepository.delete(task);
        }
        
        List<Assignment> assignments = assignmentRepository.findByModuleId(moduleId);
        for (Assignment assignment : assignments) {
            List<AssignmentSubmission> submissions = assignmentSubmissionRepository.findByAssignmentId(assignment.getId());
            for (AssignmentSubmission submission : submissions) {
                List<PeerReview> reviews = peerReviewRepository.findBySubmissionId(submission.getId());
                peerReviewRepository.deleteAll(reviews);
                assignmentSubmissionRepository.delete(submission);
            }
            assignmentRepository.delete(assignment);
        }

        moduleRepository.deleteById(moduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseModule> getModulesByCourseId(Long courseId) {
        List<CourseModule> modules = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        modules.forEach(m -> {
            if (m.getCodingTasks() != null) {
                m.getCodingTasks().size();
            }
            if (m.getAssignments() != null) {
                m.getAssignments().size();
            }
        });
        return modules;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseModule getModuleById(Long moduleId) {
        CourseModule module = moduleRepository
                .findById(moduleId)
                .orElseThrow(() -> new RuntimeException("Module not found with id: " + moduleId));
        if (module.getQuiz() != null) {
            module.getQuiz().getTitle();
            if (module.getQuiz().getQuestions() != null) {
                module.getQuiz().getQuestions().forEach(q -> {
                    if (q.getOptions() != null) {
                        q.getOptions().size();
                    }
                });
            }
        }
        return module;
    }

    @Override
    @Transactional
    public CourseModule updateModule(Long moduleId, CourseModule moduleDetails) {
        CourseModule module = getModuleById(moduleId);
        module.setTitle(moduleDetails.getTitle());
        module.setContent(moduleDetails.getContent());
        if (moduleDetails.getOrderIndex() != 0) {
            module.setOrderIndex(moduleDetails.getOrderIndex());
        }
        return moduleRepository.save(module);
    }

    @Override
    @Transactional
    public void setModuleQuiz(Long moduleId, Long quizId) {
        CourseModule module = getModuleById(moduleId);
        if (quizId != null) {
            Quiz quiz =
                    quizRepository
                            .findById(quizId)
                            .orElseThrow(() -> new RuntimeException("Quiz not found"));
            module.setQuiz(quiz);
        } else {
            module.setQuiz(null);
        }
        moduleRepository.save(module);
    }
}
