package com.jaiswarsecurities.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadSession {

    private UUID id;
    private String fileName;
    private String uploadSessionUri;
}