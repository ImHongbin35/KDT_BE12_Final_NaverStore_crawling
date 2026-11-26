package com.kdt.crawler.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UsedKeyword {
    private int id;
    private String keyword;
    private String usedDate;
    
    public UsedKeyword() {
        this.usedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public UsedKeyword(String keyword) {
        this.keyword = keyword;
        this.usedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public String getUsedDate() {
        return usedDate;
    }
    
    public void setUsedDate(String usedDate) {
        this.usedDate = usedDate;
    }
    
    @Override
    public String toString() {
        return "UsedKeyword{" +
                "id=" + id +
                ", keyword='" + keyword + '\'' +
                ", usedDate='" + usedDate + '\'' +
                '}';
    }
}