package com.worktrack.service.project;


import com.worktrack.config.FileStorageProperties;
import com.worktrack.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties properties;

    public FileStorageServiceImpl(FileStorageProperties properties) {
        this.properties = properties;
    }

    public String storeFile(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path targetPath = Paths.get(properties.getUploadDir()).resolve(fileName);
            Files.createDirectories(targetPath.getParent());
            Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new FileStorageException("Dosya yüklenemedi", e);
        }
    }

    public Resource loadFile(String filePath) {
        try {
            return new UrlResource(Paths.get(filePath).toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Dosya yüklenemedi", e);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Dosya silinemedi", e);
        }
    }
}
