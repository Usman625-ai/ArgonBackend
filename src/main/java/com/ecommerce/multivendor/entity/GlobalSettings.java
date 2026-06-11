package com.ecommerce.multivendor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "global_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GlobalSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", unique = true, nullable = false)
    private String key;

    @Column(name = "setting_value")
    private String value;
}
