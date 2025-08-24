package com.jaiswarsecurities.core.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage.BlobWriteOption;
import com.google.cloud.storage.Storage.BlobTargetOption;
import com.jaiswarsecurities.core.dao.UploadSessionDao;
import com.jaiswarsecurities.core.model.UploadSession;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Slf4j
public class StorageService {

    @Value("${firebase.storage.bucket}")
    private String bucketName;

    private Storage storage;

    @Autowired
    private UploadSessionDao uploadSessionDao;

    @PostConstruct
    private void initialize() throws IOException {
        try {
            InputStream serviceAccount = new ClassPathResource("firebase_service_account.json").getInputStream();
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            StorageOptions options = StorageOptions.newBuilder().setCredentials(credentials).build();
            storage = options.getService();
        } catch (Exception e) {
            log.error("Error initializing Firebase Storage: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = generateFileName(file.getOriginalFilename());
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();
        storage.create(blobInfo, file.getBytes());
        return fileName;
    }

    private String generateFileName(String originalFileName) {
        return UUID.randomUUID().toString() + "-" + originalFileName;
    }

    // Method to upload file and save upload session URI
    public String uploadFileWithSession(MultipartFile file) throws IOException {
        String fileName = generateFileName(file.getOriginalFilename());
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();

        // Perform the upload
        storage.create(blobInfo, file.getBytes());

        // Save the upload session URI (replace with your persistent storage logic)
        String uploadSessionUri = "dummyUploadSessionUri"; // Replace with actual URI if available
        saveUploadSessionUri(fileName, uploadSessionUri);

        return fileName;
    }

    // Method to resume an existing upload
    public String resumeUploadFile(String fileName) throws IOException {
        UploadSession uploadSession = getUploadSession(fileName);

        if (uploadSession == null) {
            throw new IOException("No upload session found for file: " + fileName);
        }

        String uploadSessionUri = uploadSession.getUploadSessionUri();

        // Implement resume upload logic using the uploadSessionUri
        // This will depend on the specific cloud storage provider
        log.info("Resuming upload for file: {} with session URI: {}", fileName, uploadSessionUri);

        // For demonstration purposes, just return the file name
        return fileName;
    }

    // Method to save upload session URI to persistent storage
    private void saveUploadSessionUri(String fileName, String uploadSessionUri) {
        UploadSession uploadSession = UploadSession.builder()
                .id(UUID.randomUUID())
                .fileName(fileName)
                .uploadSessionUri(uploadSessionUri)
                .build();
        uploadSessionDao.saveUploadSession(uploadSession);
        log.info("Saved upload session URI: {} for file: {}", uploadSessionUri, fileName);
    }

    // Method to retrieve upload session URI from persistent storage
    private UploadSession getUploadSession(String fileName) {
        UploadSession uploadSession = uploadSessionDao.getUploadSession(fileName);
        log.info("Retrieved upload session URI: {} for file: {}", uploadSession != null ? uploadSession.getUploadSessionUri() : null, fileName);
        return uploadSession;
    }
}