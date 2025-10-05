package com.project.artifactory.dto;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Data
@Builder
@RequiredArgsConstructor
public class ArtifactResponse {

    private Long id;
    private String name;
    private String version;
    private String repository;
    private String filename;
    private String sha256;
    private long size;
    private String uploader;
    private Instant createdAt;
}
