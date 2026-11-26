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
        String sql = "INSERT INTO product (keyword, productName, price, originalPrice, discountRate, " +
                "description, seller, rating, reviewCount, deliveryInfo, productUrl, imageUrl, crawledAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, productInfo.getKeyword());
            pstmt.setString(2, productInfo.getProductName());
            pstmt.setString(3, productInfo.getPrice());
            pstmt.setString(4, productInfo.getOriginalPrice());
            pstmt.setString(5, productInfo.getDiscountRate());
            pstmt.setString(6, productInfo.getDescription());
            pstmt.setString(7, productInfo.getSeller());
            pstmt.setString(8, productInfo.getRating());
            pstmt.setString(9, productInfo.getReviewCount());
            pstmt.setString(10, productInfo.getDeliveryInfo());
            pstmt.setString(11, productInfo.getProductUrl());
            pstmt.setString(12, productInfo.getImageUrl());
            pstmt.setString(13, productInfo.getCrawledAt());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✅ 데이터베이스에 상품 정보가 성공적으로 저장되었습니다.");
            } else {
                System.err.println("❌ 데이터베이스 저장에 실패했습니다.");
            }

        } catch (SQLException e) {
            System.err.println("❌ 데이터베이스 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}