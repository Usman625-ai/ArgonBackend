package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);
    Optional<Address> findByUserIdAndDefaultAddressTrue(Long userId);

    @Modifying @Transactional
    @Query("UPDATE Address a SET a.defaultAddress = false WHERE a.user.id = :userId")
    void clearDefaultAddress(@Param("userId") Long userId);

    boolean existsByUserIdAndId(Long userId, Long id);
}
