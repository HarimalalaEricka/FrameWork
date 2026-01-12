package com.framework.model;

public class UploadedFile {
    private String fileName;
    private String contentType;
    private long size;
    private byte[] content;
    
    public UploadedFile() {
        this.fileName = "";
        this.contentType = "";
        this.size = 0;
        this.content = new byte[0];
    }
    
    // Getters et setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    
    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }
    
    // Méthodes utilitaires
    public boolean isEmpty() {
        return fileName == null || fileName.isEmpty() || size == 0;
    }
    
    public String getSizeFormatted() {
        if (size < 1024) return size + " bytes";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }
    
    @Override
    public String toString() {
        return String.format("UploadedFile{fileName='%s', contentType='%s', size=%d}", 
                           fileName, contentType, size);
    }
}