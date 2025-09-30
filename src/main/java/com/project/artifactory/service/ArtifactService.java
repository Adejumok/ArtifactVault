package com.project.artifactory.service;

import com.project.artifactory.dto.UploadMetaRequest;
import com.project.artifactory.entity.Artifact;
import com.project.artifactory.entity.Blob;
import com.project.artifactory.exception.ArtifactAlreadyExistException;
import com.project.artifactory.repository.ArtifactRepository;
import com.project.artifactory.repository.BlobRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Service
public class ArtifactService {

    private final StorageService storageService;
    private final ArtifactRepository artifactRepository;
    private final BlobRepository blobRepository;

    public ArtifactService(StorageService storageService,
                           ArtifactRepository artifactRepository,
                           BlobRepository blobRepository) {
        this.storageService = storageService;
        this.artifactRepository = artifactRepository;
        this.blobRepository = blobRepository;
    }

    public Artifact upload(UploadMetaRequest meta, MultipartFile file, String uploader) throws Exception {
        Blob blob = storageService.store(file);

        if (meta.getChecksum() != null && !meta.getChecksum().isBlank()) {
            String provided = meta.getChecksum().trim().toLowerCase();
            if (!provided.equalsIgnoreCase(blob.getSha256())) {
                throw new IllegalArgumentException("Provided checksum does not match computed SHA-256");
            }
        }

        artifactRepository.findByRepositoryAndNameAndVersionAndFilename(
                meta.getRepository(), meta.getName(), meta.getVersion(), meta.getFilename()
        ).ifPresent(existing -> {
            throw new ArtifactAlreadyExistException("Artifact already exists and repository is immutable by default");
        });

        Artifact art = new Artifact();
        art.setName(meta.getName());
        art.setVersion(meta.getVersion());
        art.setRepository(meta.getRepository());
        art.setFilename(meta.getFilename());
        art.setUploader(uploader);
        art.setSha256(blob.getSha256());
        art.setSize(blob.getSize());
        art.setCreatedAt(Instant.now());

        return artifactRepository.save(art);
    }

    public List<Artifact> listByRepo(String repo) {
        return artifactRepository.findByRepository(repo);
    }

    public Artifact findByChecksum(String checksum) {
        return artifactRepository.findBySha256(checksum)
                .orElseThrow(() -> new RuntimeException("Artifact metadata not found for checksum: " + checksum));
    }
}
