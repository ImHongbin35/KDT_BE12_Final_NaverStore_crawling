package com.kdt.crawler.service;

import com.kdt.crawler.model.ProductInfo;
import java.sql.*;

public class DatabaseService {

    // --- DB 접속 정보 (환경변수에서 로드) ---
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    // -----------------------------------------

    /**
     * 상품 정보를 데이터베이스에 저장하고 관련 키워드 정보도 함께 저장합니다.
     * 새로운 3-tier 테이블 구조 (product → used_keyword → excluded_keyword)
     */
    public void saveProductInfo(ProductInfo productInfo) {
        if (productInfo == null) {
            System.err.println("❌ ProductInfo가 null입니다.");
            return;
        }
        
        System.out.println("🔍 ProductInfo 저장 시도:");
        System.out.println("   키워드: " + productInfo.getKeyword());
        System.out.println("   상품명: " + productInfo.getProductName());
        System.out.println("   가격: " + productInfo.getPrice());
        
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            conn.setAutoCommit(false); // 트랜잭션 시작
            
            System.out.println("🔗 MySQL 데이터베이스 연결 성공");
            
            // 1단계: product 테이블에 상품 정보 저장
            int productId = saveProduct(conn, productInfo);
            
            // 2단계: used_keyword 테이블에 키워드 정보 저장
            int keywordId = saveUsedKeyword(conn, productId, productInfo.getKeyword());
            
            // 트랜잭션 커밋
            conn.commit();
            System.out.println("✅ 모든 데이터가 성공적으로 저장되었습니다. (Product ID: " + productId + ", Keyword ID: " + keywordId + ")");
            
        } catch (SQLException e) {
            System.err.println("❌ 데이터베이스 오류 발생, 롤백 실행: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("❌ 롤백 실패: " + rollbackEx.getMessage());
                }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    System.err.println("❌ 연결 종료 실패: " + closeEx.getMessage());
                }
            }
        }
    }
    
    /**
     * product 테이블에 상품 정보를 저장하고 생성된 product_id를 반환합니다.
     */
    private int saveProduct(Connection conn, ProductInfo productInfo) throws SQLException {
        String sql = "INSERT INTO product (keyword, product_name, product_name_cn, price, description, " +
                    "rating, product_url, image_url, local_image_path, ocr_text) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, productInfo.getKeyword());
            pstmt.setString(2, productInfo.getProductName());
            pstmt.setString(3, null); // product_name_cn은 현재 null로 설정 (추후 번역 기능 추가 시 활용)
            pstmt.setString(4, productInfo.getPrice());
            pstmt.setString(5, productInfo.getDescription());
            pstmt.setString(6, productInfo.getRating());
            pstmt.setString(7, productInfo.getProductUrl());
            pstmt.setString(8, null); // image_url - 이미지 처리 기능 제거로 null 설정
            pstmt.setString(9, null); // local_image_path - 이미지 처리 기능 제거로 null 설정
            pstmt.setString(10, null); // ocr_text - OCR 기능 제거로 null 설정

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("상품 저장 실패, 영향받은 행이 없습니다.");
            }

            // 생성된 product_id 가져오기
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int productId = generatedKeys.getInt(1);
                    System.out.println("📦 상품 저장 완료 (Product ID: " + productId + ")");
                    return productId;
                } else {
                    throw new SQLException("상품 저장 후 ID 생성 실패");
                }
            }
        }
    }
    
    /**
     * used_keyword 테이블에 키워드 정보를 저장하고 생성된 keyword_id를 반환합니다.
     * MySQL 스키마에서 FK 컬럼명이 product_id입니다.
     */
    private int saveUsedKeyword(Connection conn, int productId, String keyword) throws SQLException {
        String sql = "INSERT INTO used_keyword (product_id, keyword, source) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, productId);
            pstmt.setString(2, keyword);
            pstmt.setString(3, "NAVER_SHOPPING_BEST"); // 키워드 소스 정보

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("키워드 저장 실패, 영향받은 행이 없습니다.");
            }

            // 생성된 keyword_id 가져오기
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int keywordId = generatedKeys.getInt(1);
                    System.out.println("🔑 키워드 저장 완료 (Keyword ID: " + keywordId + ", 키워드: " + keyword + ")");
                    return keywordId;
                } else {
                    throw new SQLException("키워드 저장 후 ID 생성 실패");
                }
            }
        }
    }
    
    /**
     * excluded_keyword 테이블에 제외된 키워드를 저장합니다.
     */
    public void saveExcludedKeyword(int keywordId, String excludedKeyword, String source) {
        String sql = "INSERT INTO excluded_keyword (keyword_id, excluded_keyword, source) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, keywordId);
            pstmt.setString(2, excludedKeyword);
            pstmt.setString(3, source);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("🚫 제외 키워드 저장 완료: " + excludedKeyword);
            }

        } catch (SQLException e) {
            System.err.println("❌ 제외 키워드 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}