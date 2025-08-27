package com.project.artifactory.controller;

import com.project.artifactory.dto.UploadMetaRequest;
import com.project.artifactory.entity.Artifact;
import com.project.artifactory.entity.Blob;
import com.project.artifactory.service.ArtifactService;
import com.project.artifactory.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/artifact")
public class ArtifactController {

    private final ArtifactService artifactService;
    private final StorageService storageService;

    public ArtifactController(ArtifactService artifactService, StorageService storageService) {
        this.artifactService = artifactService;
        this.storageService = storageService;
    }

    /**
     * Upload endpoint:
     * - multipart form part 'meta' contains JSON metadata
     * - multipart form part 'file' contains binary
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Artifact> upload(
            @RequestPart("meta") @Valid UploadMetaRequest meta,
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Uploader", required = false) String uploader
    ) throws Exception {
        Artifact saved = artifactService.upload(meta, file, uploader == null ? "anonymous" : uploader);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/artifacts")
    public List<Artifact> listAll(@RequestParam(value = "repo", required = false, defaultValue = "releases") String repo) {
        return artifactService.listByRepo(repo);
    }

    @GetMapping("/artifacts/checksum/{sha256}")
    public Artifact getByChecksum(@PathVariable String sha256) {
        return artifactService.findByChecksum(sha256);
    }

    /**
     * Stream blob by checksum.
     * Example: GET /api/blobs/{sha256}
     */
    @GetMapping("/blobs/{sha256}")
    public ResponseEntity<?> downloadBlob(@PathVariable String sha256) throws Exception {
        Blob blob = storageService.getBlob(sha256).orElseThrow(() -> new RuntimeException("Blob not found"));
        try (InputStream in = storageService.loadBlobAsStream(sha256)) {
            String filename = sha256;
            String disp = "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disp)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(blob.getSize())
                    .body(in.readAllBytes());
        }
    }

    /** for streaming large files*/
    @GetMapping("/blobs/{sha256}/stream")
    public ResponseEntity<StreamingResponseBody> downloadBlobStream(@PathVariable String sha256) throws Exception {
        Blob blob = storageService.getBlob(sha256).orElseThrow(() -> new RuntimeException("Blob not found"));
        InputStream in = storageService.loadBlobAsStream(sha256);
        StreamingResponseBody body = outputStream -> {
            try (InputStream is = in; OutputStream os = outputStream) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = is.read(buf)) != -1) {
                    os.write(buf, 0, r);
                }
                os.flush();
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(blob.getSize())
                .body(body);
    }

}
