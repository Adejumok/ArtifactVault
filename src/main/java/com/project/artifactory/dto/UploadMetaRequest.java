package com.project.artifactory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadMetaRequest {

    @NotBlank(message = "name is required")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "version is required")
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+(-[A-Za-z0-9.-]+)?$",
            message = "version must be semver-like (e.g. 1.0.0 or 1.0.0-SNAPSHOT)")
    private String version;

    @NotBlank(message = "repository is required")
    @Size(max = 100)
    private String repository;

    @NotBlank(message = "filename is required")
    @Size(max = 255)
    private String filename;

    private String checksum;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
