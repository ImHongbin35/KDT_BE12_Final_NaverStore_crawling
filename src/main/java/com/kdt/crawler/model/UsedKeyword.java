package com.kdt.crawler.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UsedKeyword {
    private int keywordId;
    private String keyword;
    private String createdAt;
    private String source;
    
    public UsedKeyword() {
        this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public UsedKeyword(String keyword) {
        this.keyword = keyword;
        this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    // Getters and Setters
    public int getKeywordId() {
        return keywordId;
    }
    
    public void setKeywordId(int keywordId) {
        this.keywordId = keywordId;
    }
    
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    @Override
    public String toString() {
        return "UsedKeyword{" +
                "keywordId=" + keywordId +
                ", keyword='" + keyword + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}