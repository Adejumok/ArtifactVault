package com.project.artifactory.repository;

import com.project.artifactory.entity.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ArtifactRepository extends JpaRepository<Artifact, Long> {
    Optional<Artifact> findBySha256(String sha256);
    Optional<Artifact> findByRepositoryAndNameAndVersionAndFilename(String repository, String name, String version, String filename);
    List<Artifact> findByRepository(String repository);
}

