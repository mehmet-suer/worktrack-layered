package com.worktrack.service.project;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {

    String storeFile(MultipartFile file);
    Resource loadFile(String filePath);
    void deleteFile(String filePath);
}
