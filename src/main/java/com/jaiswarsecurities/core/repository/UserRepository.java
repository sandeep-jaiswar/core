package com.jaiswarsecurities.core.repository;

import com.jaiswarsecurities.core.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    Optional<User> findByTradingAccountNumber(String tradingAccountNumber);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByTradingAccountNumber(String tradingAccountNumber);

    Optional<User> findByPasswordResetToken(String token);
    
    Optional<User> findByEmailVerificationToken(String token);

    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.accountLocked = false")
    List<User> findActiveUsers();

    @Query("SELECT u FROM User u WHERE u.lastLoginDate < :date")
    List<User> findUsersInactiveSince(@Param("date") LocalDateTime date);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.kycStatus = :status")
    List<User> findByKycStatus(@Param("status") User.KycStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdDate >= :date")
    long countUsersRegisteredAfter(@Param("date") LocalDateTime date);
}
