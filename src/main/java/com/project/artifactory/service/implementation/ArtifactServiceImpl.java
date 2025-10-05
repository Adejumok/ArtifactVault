package com.project.artifactory.service.implementation;

import com.project.artifactory.dto.ArtifactResponse;
import com.project.artifactory.dto.UploadMetaRequest;
import com.project.artifactory.entity.Artifact;
import com.project.artifactory.entity.Blob;
import com.project.artifactory.exception.ArtifactAlreadyExistException;
import com.project.artifactory.mapper.ArtifactMapper;
import com.project.artifactory.repository.ArtifactRepository;
import com.project.artifactory.repository.BlobRepository;
import com.project.artifactory.service.ArtifactService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArtifactServiceImpl implements ArtifactService {

    private final StorageServiceImpl storageService;
    private final ArtifactRepository artifactRepository;
    private final BlobRepository blobRepository;
    private final ArtifactMapper artifactMapper;

    public ArtifactServiceImpl(StorageServiceImpl storageService,
                               ArtifactRepository artifactRepository,
                               BlobRepository blobRepository, ArtifactMapper artifactMapper) {
        this.storageService = storageService;
        this.artifactRepository = artifactRepository;
        this.blobRepository = blobRepository;
        this.artifactMapper = artifactMapper;
    }

    @Override
    public ArtifactResponse upload(UploadMetaRequest meta, MultipartFile file, String uploader) throws Exception {
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

        artifactRepository.save(art);

        return artifactMapper.toResponse(art);
    }

    @Override
    public List<ArtifactResponse> listByRepo(String repo) {
        List<Artifact> artifactList= artifactRepository.findByRepository(repo);
        return artifactList.stream().map(artifactMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public ArtifactResponse findByChecksum(String checksum) {
        Artifact artifact = artifactRepository.findBySha256(checksum)
                .orElseThrow(() -> new RuntimeException("Artifact metadata not found for checksum: " + checksum));
        return artifactMapper.toResponse(artifact);
    }
}
