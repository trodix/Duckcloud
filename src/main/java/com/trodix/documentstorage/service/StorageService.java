package com.trodix.documentstorage.service;

import com.trodix.documentstorage.model.FileStoreMetadata;
import io.minio.*;
import io.minio.messages.Bucket;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class StorageService {

    public static final String ROOT_BUCKET = "root";

    private final MinioClient minioClient;

    @PostConstruct
    private void init() throws Exception {
        final boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(ROOT_BUCKET).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(ROOT_BUCKET).build());
        } else {
            log.info("Bucket '{}' already exists.", ROOT_BUCKET);
        }
    }

    public List<Bucket> listBuckets() {
        try {
            return minioClient.listBuckets();
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public ObjectWriteResponse uploadFile(final FileStoreMetadata fileStoreMetadata, final byte[] content) {
        try {
            final PutObjectArgs obj = PutObjectArgs.builder()
                    .bucket(ROOT_BUCKET)
                    .object(Path.of(fileStoreMetadata.getDirectoryPath(), fileStoreMetadata.getUuid()).toString())
                    .contentType(fileStoreMetadata.getContentType())
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .build();
            final ObjectWriteResponse response = minioClient.putObject(obj);
            log.debug("Created new object in storage: {}", response.object());
            return response;
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public byte[] getFile(final String path, final String name) {
        final GetObjectArgs args = GetObjectArgs.builder().bucket(ROOT_BUCKET).object(Path.of(path, name).toString()).build();
        try (final InputStream obj = minioClient.getObject(args)) {
            return IOUtils.toByteArray(obj);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
