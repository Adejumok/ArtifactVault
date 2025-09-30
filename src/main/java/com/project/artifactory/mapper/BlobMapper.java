package com.project.artifactory.mapper;

import com.project.artifactory.dto.BlobResponse;
import com.project.artifactory.entity.Blob;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BlobMapper {

    BlobResponse toResponse(Blob blob);
}
