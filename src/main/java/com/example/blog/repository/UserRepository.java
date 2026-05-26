package com.example.blog.repository;

import com.example.blog.entity.User;
import com.example.blog.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Поиск пользователя по имени (необходим для аутентификации).
     */
    Optional<User> findByUsername(String username);

    /**
     * Поиск пользователей по роли (например, ROLE_STUDENT).
     */
    java.util.List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND (" +
           ":search IS NULL OR :search = '' OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")")
    Page<User> findByRoleAndSearch(
        @Param("role") Role role, 
        @Param("search") String search, 
        Pageable pageable
    );
}
