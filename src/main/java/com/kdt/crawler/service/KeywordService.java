package com.kdt.crawler.service;

import com.kdt.crawler.model.UsedKeyword;
import com.kdt.crawler.util.LanguageConverter;
import com.kdt.crawler.util.SimilarityService;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;

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
     * 키워드의 유효성을 검증 - 싸다구몰에서 실제 상품을 찾을 수 있는지 확인
     * 
     * @param page Playwright 페이지 객체
     * @param keyword 검증할 키워드
     * @return 키워드가 유효한 상품을 찾으면 true, 아니면 false
     */
    public boolean validateKeyword(Page page, String keyword) {
        return validateKeyword(page, keyword, null);
    }
    
    /**
     * 키워드의 유효성을 검증 - 원본 키워드와 대체 키워드 둘 다 고려하여 유사도 검증
     * 
     * @param page Playwright 페이지 객체
     * @param searchKeyword 검색에 사용할 키워드
     * @param originalKeyword 원본 키워드 (유사도 검증에 추가로 사용)
     * @return 키워드가 유효한 상품을 찾으면 true, 아니면 false
     */
    public boolean validateKeyword(Page page, String searchKeyword, String originalKeyword) {
        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            System.out.println("⚠️ 빈 키워드는 검증할 수 없습니다.");
            return false;
        }
        
        System.out.println("🔍 키워드 '" + searchKeyword + "' 유효성 검증 시작...");
        if (originalKeyword != null && !originalKeyword.equals(searchKeyword)) {
            System.out.println("   (원본 키워드: '" + originalKeyword + "'와 함께 유사도 검증)");
        }
        
        try {
            // 싸다구몰 검색 페이지로 이동
            page.navigate("https://ssadagu.kr");
            page.waitForLoadState();
            
            // 검색창 찾기
            String[] searchSelectors = {
                "input[name='ss_tx']",
                "input[type='text'][placeholder*='검색']",
                ".search-input",
                "input.form-control"
            };
            
            Locator searchBox = null;
            for (String selector : searchSelectors) {
                if (page.locator(selector).count() > 0) {
                    searchBox = page.locator(selector).first();
                    break;
                }
            }
            
            if (searchBox == null) {
                System.out.println("❌ 검색창을 찾을 수 없어 키워드 검증 실패");
                return false;
            }
            
            // 검색 실행
            searchBox.clear();
            searchBox.fill(searchKeyword);
            searchBox.press("Enter");
            page.waitForLoadState();
            page.waitForTimeout(2000); // 검색 결과 로딩 대기
            
            // 첫 번째 상품 클릭
            boolean clickSuccess = clickFirstProduct(page);
            if (!clickSuccess) {
                System.out.println("❌ 키워드 '" + searchKeyword + "': 첫 번째 상품 클릭 실패");
                return false;
            }
            
            // 상품 페이지 로딩 대기
            page.waitForLoadState();
            page.waitForTimeout(2000);
            
            // 상품 페이지에서 제목 추출
            String productTitle = extractProductPageTitle(page);
            
            if (productTitle == null || productTitle.trim().isEmpty()) {
                System.out.println("❌ 키워드 '" + searchKeyword + "': 상품 페이지에서 제목을 찾을 수 없음");
                return false;
            }
            
            System.out.println("🔍 상품 페이지 제목 추출 완료: " + productTitle);
            
            // 키워드와 상품 제목 연관성 검사 (원본 키워드와 검색 키워드 둘 다 고려)
            boolean isRelated = isKeywordRelatedToProduct(searchKeyword, originalKeyword, productTitle);
            
            if (isRelated) {
                System.out.println("✅ 키워드 '" + searchKeyword + "' 검증 성공");
                System.out.println("   매칭된 상품: " + productTitle);
                return true;
            } else {
                System.out.println("❌ 키워드 '" + searchKeyword + "' 검증 실패: 상품 제목과 연관성 부족");
                System.out.println("   찾은 상품: " + productTitle);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ 키워드 검증 중 오류 발생: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 키워드의 대체 언어 버전으로 유효성 검증 (한글→영어 또는 영어→한글)
     * 
     * @param page Playwright 페이지 객체
     * @param originalKeyword 원본 키워드
     * @return 대체 언어로 검증 성공하면 대체 키워드, 실패하면 null
     */
    public String validateWithAlternativeLanguage(Page page, String originalKeyword) {
        String alternativeKeyword = LanguageConverter.getAlternativeLanguage(originalKeyword);
        
        if (alternativeKeyword == null) {
            System.out.println("🔍 키워드 '" + originalKeyword + "'의 대체 언어 매핑이 없습니다.");
            return null;
        }
        
        System.out.println("🔄 대체 언어로 검증 시도: " + 
                          LanguageConverter.formatConversionResult(originalKeyword, alternativeKeyword));
        
        // 대체 키워드도 30일 중복 검사 수행
        boolean isUsedRecently = isKeywordUsedRecently(alternativeKeyword);
        if (isUsedRecently) {
            System.out.println("❌ 대체 키워드 '" + alternativeKeyword + "'는 최근 30일 내 사용됨 - 검증 중단");
            return null;
        }
        
        System.out.println("✅ 대체 키워드 '" + alternativeKeyword + "' - 30일 중복 검사 통과");
        
        // 대체 키워드로 검색하되, 원본 키워드와 함께 유사도 검증
        if (validateKeyword(page, alternativeKeyword, originalKeyword)) {
            return alternativeKeyword;
        }
        
        return null;
    }
    
    /**
     * 첫 번째 상품을 클릭하는 메서드
     * 
     * @param page Playwright 페이지 객체
     * @return 클릭 성공 시 true, 실패 시 false
     */
    private boolean clickFirstProduct(Page page) {
        // 상품 링크를 찾기 위한 여러 선택자
        String[] productSelectors = {
            "div.product_item a[href*='view.php']",
            "div.product_item a",
            ".product_item a",
            ".product-item a",
            "a[href*='view.php']"
        };
        
        for (String selector : productSelectors) {
            try {
                Locator elements = page.locator(selector);
                if (elements.count() > 0) {
                    System.out.println("🔍 첫 번째 상품 클릭 시도: " + selector);
                    elements.first().click();
                    return true;
                }
            } catch (Exception e) {
                // 선택자 실패 시 다음 선택자 시도
                System.out.println("⚠️ 선택자 실패: " + selector + " - " + e.getMessage());
                continue;
            }
        }
        
        return false;
    }
    
    /**
     * 상품 페이지에서 제목을 추출하는 메서드
     * 
     * @param page Playwright 페이지 객체
     * @return 상품 제목 (찾지 못하면 null)
     */
    private String extractProductPageTitle(Page page) {
        // 상품 페이지 제목을 찾기 위한 여러 선택자
        String[] titleSelectors = {
            ".product_title",
            ".goods_name", 
            ".item_name",
            ".product-title",
            ".goods-title",
            "h1",
            "h2",
            ".title"
        };
        
        for (String selector : titleSelectors) {
            try {
                Locator elements = page.locator(selector);
                if (elements.count() > 0) {
                    String title = elements.first().textContent();
                    if (title != null && !title.trim().isEmpty()) {
                        System.out.println("📝 상품 제목 선택자 성공: " + selector);
                        return title.trim();
                    }
                }
            } catch (Exception e) {
                // 선택자 실패 시 다음 선택자 시도
                continue;
            }
        }
        
        return null;
    }
    
    /**
     * 키워드와 상품 제목의 연관성을 검사하는 메서드 (단일 키워드)
     * Komoran 형태소 분석기를 활용한 고도화된 검증 로직 사용
     * 
     * @param keyword 검색 키워드
     * @param productTitle 상품 제목
     * @return 연관성이 있으면 true, 아니면 false
     */
    private boolean isKeywordRelatedToProduct(String keyword, String productTitle) {
        return isKeywordRelatedToProduct(keyword, null, productTitle);
    }
    
    /**
     * 키워드와 상품 제목의 연관성을 검사하는 메서드 (이중 키워드)
     * 새로운 3단계 유사도 검증 로직 사용
     * 
     * @param searchKeyword 검색에 사용된 키워드
     * @param originalKeyword 원본 키워드 (null 가능)
     * @param productTitle 상품 제목
     * @return 연관성이 있으면 true, 아니면 false
     */
    private boolean isKeywordRelatedToProduct(String searchKeyword, String originalKeyword, String productTitle) {
        System.out.println("🔍 새로운 3단계 유사도 검증 시작");
        System.out.println("   검색 키워드: " + searchKeyword);
        if (originalKeyword != null && !originalKeyword.equals(searchKeyword)) {
            System.out.println("   원본 키워드: " + originalKeyword);
        }
        System.out.println("   상품명: " + productTitle);
        
        // 이중 검증을 위해 검증할 키워드 목록을 준비 (중복 제거)
        Set<String> keywordsToValidate = new LinkedHashSet<>();
        keywordsToValidate.add(searchKeyword);
        if (originalKeyword != null && !originalKeyword.equals(searchKeyword)) {
            keywordsToValidate.add(originalKeyword);
        }
        
        // 각 키워드에 대해 새로운 SimilarityService.isRelevant() 사용
        for (String keyword : keywordsToValidate) {
            System.out.println("   [검증 대상: '" + keyword + "']");
            
            if (SimilarityService.isRelevant(keyword, productTitle)) {
                System.out.println("   ✅ 키워드 '" + keyword + "' 유사도 검증 성공");
                return true;
            }
            System.out.println("   ❌ 키워드 '" + keyword + "' 유사도 검증 실패");
        }
        
        System.out.println("❌ 모든 키워드 검증 실패");
        return false;
    }
}