package com.project.artifactory.repository;

import com.project.artifactory.entity.Blob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlobRepository extends JpaRepository<Blob, String> {
}

