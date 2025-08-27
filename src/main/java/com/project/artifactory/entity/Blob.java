package com.project.artifactory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blob {
    @Id
    @Column(length = 64)
    private String sha256;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private String localPath;

    private String mediaType;
}
