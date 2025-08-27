package com.project.artifactory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "artifacts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "version", "filename", "repository"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artifact {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String repository;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false, length = 64)
    private String sha256;

    private long size;

    private String uploader;

    private Instant createdAt;
}
