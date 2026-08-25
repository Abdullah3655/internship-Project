package com.recruitment.candidateservice.service;

import com.recruitment.candidateservice.config.FileUploadProperties;
import com.recruitment.candidateservice.exception.InvalidFileException;
import com.recruitment.candidateservice.exception.StorageException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt");

    private final Path uploadRoot;

    public FileStorageService(FileUploadProperties properties) {
        this.uploadRoot = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
    }

    public StoredFile storeCv(UUID candidateId, MultipartFile file) {
        if (file == null) {
            throw new InvalidFileException("A CV file is required");
        }
        String original = file.getOriginalFilename() == null ? "cv" : file.getOriginalFilename();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new InvalidFileException("Could not read CV file");
        }
        if (bytes.length == 0) {
            throw new InvalidFileException(
                    "CV file is empty (0 bytes): " + original
                            + ". In Postman use Body → form-data → key 'files' with type File (not Text), then Select Files."
            );
        }
        return storeCv(candidateId, original, bytes, file.getContentType());
    }

    public StoredFile storeCv(UUID candidateId, String originalFilename, byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new InvalidFileException("A CV file is required");
        }
        String original = originalFilename == null || originalFilename.isBlank() ? "cv" : originalFilename;
        String extension = extensionOf(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException("CV must be a pdf, doc, docx, or txt file");
        }

        Path directory = uploadRoot.resolve(candidateId.toString());
        try {
            Files.createDirectories(directory);
            String storedName = UUID.randomUUID() + "." + extension;
            Path destination = directory.resolve(storedName);
            Files.write(destination, bytes);
            return new StoredFile(original, destination.toString(), contentType, bytes.length);
        } catch (IOException ex) {
            throw new StorageException("Could not store CV on server", ex);
        }
    }

    public Resource loadAsResource(String storagePath) {
        Path path = Path.of(storagePath).toAbsolutePath().normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new InvalidFileException("Invalid document storage path");
        }
        if (!Files.isRegularFile(path)) {
            throw new InvalidFileException("Document file is missing on server");
        }
        return new FileSystemResource(path);
    }

    public void deleteStoredFile(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        Path path = Path.of(storagePath).toAbsolutePath().normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new InvalidFileException("Invalid document storage path");
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new StorageException("Could not delete CV file on server", ex);
        }
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String originalFilename, String storagePath, String contentType, long sizeBytes) {
    }
}
