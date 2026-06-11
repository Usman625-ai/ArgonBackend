package com.ecommerce.multivendor.repository;

import com.ecommerce.multivendor.entity.GlobalSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalSettingsRepository extends JpaRepository<GlobalSettings, Long> {
    Optional<GlobalSettings> findByKey(String key);
}
