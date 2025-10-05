package com.project.artifactory.controller;

import com.project.artifactory.dto.ArtifactResponse;
import com.project.artifactory.dto.UploadMetaRequest;
import com.project.artifactory.service.ArtifactService;
import com.project.artifactory.service.implementation.StorageServiceImpl;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/artifact")
public class ArtifactController {

    private final ArtifactService artifactService;
    private final StorageServiceImpl storageService;

    public ArtifactController(ArtifactService artifactService, StorageServiceImpl storageService) {
        this.artifactService = artifactService;
        this.storageService = storageService;
    }

    /**
     * Upload endpoint:
     * - multipart form part 'meta' contains JSON metadata
     * - multipart form part 'file' contains binary
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArtifactResponse> upload(
            @RequestPart("meta") @Valid UploadMetaRequest meta,
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Uploader", required = false) String uploader
    ) throws Exception {
        ArtifactResponse saved = artifactService.upload(meta, file, uploader == null ? "anonymous" : uploader);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/artifacts")
    public List<ArtifactResponse> listAll(@RequestParam(value = "repo", required = false, defaultValue = "releases") String repo) {
        return artifactService.listByRepo(repo);
    }

    @GetMapping("/artifacts/checksum/{sha256}")
    public ArtifactResponse getByChecksum(@PathVariable String sha256) {
        return artifactService.findByChecksum(sha256);
    }

    @GetMapping("/blobs/{sha256}")
    public  ResponseEntity<InputStreamResource> downloadBlob(@PathVariable String sha256) throws Exception {
        InputStreamResource inputStreamResource = storageService.downloadBlob(sha256);
        String disposition = "attachment; filename=\"" + URLEncoder.encode(sha256, StandardCharsets.UTF_8) + "\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(inputStreamResource.contentLength())
                .body(new InputStreamResource(inputStreamResource));

    }

}
