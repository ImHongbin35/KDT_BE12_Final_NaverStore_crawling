package com.kdt.crawler.service;

import com.kdt.crawler.model.ProductInfo;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseService {

    // --- DB 접속 정보 (환경변수에서 로드) ---
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    // -----------------------------------------

    public void saveProductInfo(ProductInfo productInfo) {
        if (productInfo == null) {
            System.err.println("❌ ProductInfo가 null입니다.");
            return;
        }
        
        System.out.println("🔍 ProductInfo 저장 시도:");
        System.out.println("   키워드: " + productInfo.getKeyword());
        System.out.println("   상품명: " + productInfo.getProductName());
        System.out.println("   중국어 상품명: " + productInfo.getProductNameCn());
        System.out.println("   가격: " + productInfo.getPrice());
        System.out.println("   수집시간: " + productInfo.getCrawledAt());
        
        String sql = "INSERT INTO product (keyword, product_name, product_name_cn, price, original_price, discount_rate, " +
                "description, seller, rating, review_count, delivery_info, product_url, image_url, local_image_path, ocr_text, crawled_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            System.out.println("🔗 데이터베이스 연결 성공");

            pstmt.setString(1, productInfo.getKeyword());
            pstmt.setString(2, productInfo.getProductName());
            pstmt.setString(3, productInfo.getProductNameCn());
            pstmt.setString(4, productInfo.getPrice());
            pstmt.setString(5, productInfo.getOriginalPrice());
            pstmt.setString(6, productInfo.getDiscountRate());
            pstmt.setString(7, productInfo.getDescription());
            pstmt.setString(8, productInfo.getSeller());
            pstmt.setString(9, productInfo.getRating());
            pstmt.setString(10, productInfo.getReviewCount());
            pstmt.setString(11, productInfo.getDeliveryInfo());
            pstmt.setString(12, productInfo.getProductUrl());
            pstmt.setString(13, productInfo.getImageUrl());
            pstmt.setString(14, productInfo.getLocalImagePath());
            pstmt.setString(15, productInfo.getOcrText());
            pstmt.setString(16, productInfo.getCrawledAt());

            System.out.println("📝 SQL 실행 중...");
            int affectedRows = pstmt.executeUpdate();
            System.out.println("📊 영향받은 행 수: " + affectedRows);

            if (affectedRows > 0) {
                System.out.println("✅ 데이터베이스에 상품 정보가 성공적으로 저장되었습니다.");
            } else {
                System.err.println("❌ 데이터베이스 저장에 실패했습니다. (영향받은 행 없음)");
            }

        } catch (SQLException e) {
            System.err.println("❌ 데이터베이스 오류: " + e.getMessage());
            System.err.println("❌ SQL State: " + e.getSQLState());
            System.err.println("❌ Error Code: " + e.getErrorCode());
            e.printStackTrace();
        }
    }
}