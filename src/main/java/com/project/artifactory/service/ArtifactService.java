package com.project.artifactory.service;

import com.project.artifactory.dto.ArtifactResponse;
import com.project.artifactory.dto.UploadMetaRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ArtifactService {
    ArtifactResponse upload(UploadMetaRequest meta, MultipartFile file, String uploader) throws Exception;
    List<ArtifactResponse> listByRepo(String repo);
    ArtifactResponse findByChecksum(String checksum);

}
