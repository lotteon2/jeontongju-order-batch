package com.jeontongju.orderbatch.config;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MyMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String filename;

    public MyMultipartFile(byte[] content, String filename) {
        this.content = content;
        this.filename = filename;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getOriginalFilename() {
        return this.filename;
    }

    @Override
    public String getContentType() {
        return "image/png"; // Adjust the content type as needed
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        // Implement this method if needed
    }
}