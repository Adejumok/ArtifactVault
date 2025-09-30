package com.project.artifactory.repository;

import com.project.artifactory.entity.Blob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlobRepository extends JpaRepository<Blob, String> {
    Optional<Blob> findById(Long id);
}

