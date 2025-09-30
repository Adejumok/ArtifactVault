package com.project.artifactory.service;

import com.project.artifactory.dto.BlobResponse;
import com.project.artifactory.entity.Blob;
import com.project.artifactory.mapper.BlobMapper;
import com.project.artifactory.repository.BlobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class StorageService {

    private final Path root;
    private final long maxBytes;
    private final BlobRepository blobRepository;
    private final BlobMapper blobMapper;

    public StorageService(@Value("${artifact.storage.location}") String rootDir,
                          @Value("${artifact.upload.max-bytes}") long maxBytes,
                          BlobRepository blobRepository, BlobMapper blobMapper) {
        this.root = Paths.get(rootDir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        this.blobRepository = blobRepository;
        this.blobMapper = blobMapper;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(root);
    }

    public BlobResponse findBlob(Long id) {
        Optional<Blob> blob = blobRepository.findById(id);
        return blob.map(blobMapper::toResponse).orElse(null);
    }

    public InputStreamResource downloadBlob(String sha256) throws Exception {
        Blob blob = blobRepository.findById(sha256)
                .orElseThrow(() -> new RuntimeException("Blob not found"));

        InputStream in = loadBlobAsStream(blob.getSha256());

        return new InputStreamResource(in);

    }


    /**
     * Stores uploaded file to CAS storage while computing SHA-256.
     * Returns Blob (saved) and computed sha256 hex.
     */
    public Blob store(MultipartFile file) throws IOException {
        if (file.getSize() > maxBytes) {
            throw new IOException("File too large. Max: " + maxBytes);
        }

        // Stream to temp file and compute sha256 while streaming
        Path tmp = Files.createTempFile("upload-", ".tmp");
        MessageDigest md;
        try (InputStream in = file.getInputStream();
             OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING)) {
            md = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream dis = new DigestInputStream(in, md)) {
                long copied = StreamUtils.copy(dis, out);
            }
        } catch (Exception e) {
            Files.deleteIfExists(tmp);
            throw new IOException("Failed to store file", e);
        }

        String sha256 = HexFormat.of().formatHex(md.digest());
        Path blobPath = computeBlobPath(sha256);
        Files.createDirectories(blobPath.getParent());

        if (Files.exists(blobPath)) {
            Files.deleteIfExists(tmp);
            return blobRepository.findById(sha256)
                    .orElseGet(() -> {
                        Blob blob = new Blob();
                        blob.setSha256(sha256);
                        blob.setSize(file.getSize());
                        blob.setLocalPath(blobPath.toString());
                        blob.setMediaType(file.getContentType());

                        return blobRepository.save(blob);
                    });
        }

        // Move temp file to final location (atomic where possible)
        try {
            Files.move(tmp, blobPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, blobPath, StandardCopyOption.REPLACE_EXISTING);
        }

        Blob blob = new Blob();
        blob.setSha256(sha256);
        blob.setSize(Files.size(blobPath));
        blob.setLocalPath(blobPath.toString());
        blob.setMediaType(file.getContentType());

        return blobRepository.save(blob);
    }

    public Path computeBlobPath(String sha256) {
        String a = sha256.substring(0, 2);
        String b = sha256.substring(2, 4);
        return root.resolve(a).resolve(b).resolve(sha256);
    }

    public InputStream loadBlobAsStream(String sha256) throws FileNotFoundException {
        Path p = computeBlobPath(sha256);
        if (!Files.exists(p)) {
            throw new FileNotFoundException("Blob not found: " + sha256);
        }
        try {
            return Files.newInputStream(p, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to open blob: " + e.getMessage());
        }
    }

}
