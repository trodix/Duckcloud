package com.trodix.documentstorage.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CreatedFileResponse {

    private String originalFileName;
    private String bucketName;
    private String storageName;
    
}
