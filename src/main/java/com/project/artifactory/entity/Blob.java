package com.project.artifactory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blobs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blob {

    @Id
    @Column(nullable = false)
    private Long id;

    @Column(length = 64)
    private String sha256;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private String localPath;

    private String mediaType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }
}
