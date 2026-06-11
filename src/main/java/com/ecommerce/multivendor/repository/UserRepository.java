package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.enums.Role;
import com.ecommerce.multivendor.enums.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Page<User> findByRole(Role role, Pageable pageable);
    List<User> findByRole(Role role);

    Page<User> findByRoleAndSellerStatus(Role role, SellerStatus status, Pageable pageable);
    List<User> findByRoleAndSellerStatus(Role role, SellerStatus status);

    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<User> searchByRoleAndKeyword(@Param("role") Role role,
                                      @Param("q") String q,
                                      Pageable pageable);

    // Dashboard stats
    long countByRole(Role role);
    long countByRoleAndSellerStatus(Role role, SellerStatus status);
    long countByActive(boolean active);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.active = true")
    long countActiveByRole(@Param("role") Role role);
}
