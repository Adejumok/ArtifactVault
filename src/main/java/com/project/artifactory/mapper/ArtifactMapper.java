package com.project.artifactory.mapper;

import com.project.artifactory.dto.ArtifactResponse;
import com.project.artifactory.entity.Artifact;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ArtifactMapper {
    ArtifactResponse toResponse(Artifact artifact);
}
