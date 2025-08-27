package com.project.artifactory.service;

import com.project.artifactory.entity.Blob;
import com.project.artifactory.repository.BlobRepository;
import org.springframework.beans.factory.annotation.Value;
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

    public StorageService(@Value("${artifact.storage.location}") String rootDir,
                          @Value("${artifact.upload.max-bytes}") long maxBytes,
                          BlobRepository blobRepository) {
        this.root = Paths.get(rootDir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        this.blobRepository = blobRepository;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(root);
    }

    public Optional<Blob> getBlob(String sha256) {
        return blobRepository.findById(sha256);
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
                // copied bytes already written
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
                        try {
                            Blob b = Blob.builder()
                                    .sha256(sha256)
                                    .size(Files.size(blobPath))
                                    .localPath(blobPath.toString())
                                    .mediaType(file.getContentType())
                                    .build();
                            return blobRepository.save(b);
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
        }

        // Move temp file to final location (atomic where possible)
        try {
            Files.move(tmp, blobPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, blobPath, StandardCopyOption.REPLACE_EXISTING);
        }

        Blob blob = Blob.builder()
                .sha256(sha256)
                .size(Files.size(blobPath))
                .localPath(blobPath.toString())
                .mediaType(file.getContentType())
                .build();

        return blobRepository.save(blob);
    }

    public Path computeBlobPath(String sha256) {
        // two-level fanout: ab/cd/<sha256>
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
