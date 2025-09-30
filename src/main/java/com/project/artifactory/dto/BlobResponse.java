package com.project.artifactory.dto;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
public class BlobResponse {
    private Long id;
    private String sha256;
    private long size;
    private String mediaType;
}
