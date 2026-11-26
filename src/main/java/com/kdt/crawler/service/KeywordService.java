package com.kdt.crawler.service;

import com.kdt.crawler.model.UsedKeyword;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.time.LocalDate;

public class KeywordService {
    
    // --- DB 접속 정보 (환경변수에서 로드, DatabaseService와 동일한 설정) ---
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    // -----------------------------------------
    
    /**
     * used_keywords 테이블에 키워드 저장
     */
    public void saveUsedKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            System.err.println("❌ 키워드가 비어있어서 저장할 수 없습니다.");
            return;
        }
        
        UsedKeyword usedKeyword = new UsedKeyword(keyword.trim());
        saveUsedKeyword(usedKeyword);
    }
    
    /**
     * UsedKeyword 객체를 used_keywords 테이블에 저장
     */
    public void saveUsedKeyword(UsedKeyword usedKeyword) {
        String sql = "INSERT INTO used_keywords (keyword, used_date) VALUES (?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, usedKeyword.getKeyword());
            pstmt.setString(2, usedKeyword.getUsedDate());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✅ 키워드 '" + usedKeyword.getKeyword() + "'가 used_keywords 테이블에 저장되었습니다.");
            } else {
                System.err.println("❌ 키워드 저장에 실패했습니다: " + usedKeyword.getKeyword());
            }
            
        } catch (SQLException e) {
            System.err.println("❌ used_keywords 테이블 저장 오류: " + e.getMessage());
            System.err.println("   키워드: " + usedKeyword.getKeyword());
            e.printStackTrace();
        }
    }
    
    /**
     * 키워드가 최근 30일 내에 사용되었는지 확인 (트렌드성 반영을 위한 30일 중복 방지)
     * @param keyword 확인할 키워드
     * @return 최근 30일 내에 사용되었으면 true, 아니면 false
     */
    public boolean isKeywordUsedRecently(String keyword) {
        return isKeywordUsedRecently(keyword, 30);
    }
    
    /**
     * 키워드가 최근 daysAgo일 내에 사용되었는지 확인
     * @param keyword 확인할 키워드
     * @param daysAgo 확인할 기간 (일)
     * @return 최근 기간 내에 사용되었으면 true, 아니면 false
     */
    public boolean isKeywordUsedRecently(String keyword, int daysAgo) {
        // 중복 검사를 시작할 날짜 계산 (오늘로부터 daysAgo일 전)
        LocalDate cutoffDate = LocalDate.now().minusDays(daysAgo);
        
        String sql = "SELECT COUNT(*) FROM used_keywords WHERE keyword = ? AND used_date >= ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, keyword);
            pstmt.setString(2, cutoffDate.toString());
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("🔍 키워드 '" + keyword + "' 최근 " + daysAgo + "일 내 사용 횟수: " + count);
                return count > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ 키워드 중복 체크 오류: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 키워드 중복 체크 (당일 중복 체크용 - 기존 기능 유지)
     */
    public boolean isKeywordAlreadyUsed(String keyword) {
        String sql = "SELECT COUNT(*) FROM used_keywords WHERE keyword = ? AND DATE(used_date) = CURDATE()";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, keyword);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ 키워드 중복 체크 오류: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
}