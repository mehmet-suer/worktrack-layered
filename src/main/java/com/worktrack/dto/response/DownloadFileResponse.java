package com.worktrack.dto.response;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record DownloadFileResponse(Resource file, String fileName, MediaType contentType) {}