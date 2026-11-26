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
    
    // --- DB 접속 정보 (환경변수에서 로드; 이 폴더는 DatabaseService가 Oracle을 쓰도록 바뀌어 더 이상 동일하지 않음) ---
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
    // -----------------------------------------
    
    /**
     * used_keyword 테이블에 키워드 저장
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
     * UsedKeyword 객체를 used_keyword 테이블에 저장
     */
    public void saveUsedKeyword(UsedKeyword usedKeyword) {
        String sql = "INSERT INTO used_keyword (keyword, created_at) VALUES (?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, usedKeyword.getKeyword());
            pstmt.setString(2, usedKeyword.getCreatedAt());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("✅ 키워드 '" + usedKeyword.getKeyword() + "'가 used_keyword 테이블에 저장되었습니다.");
            } else {
                System.err.println("❌ 키워드 저장에 실패했습니다: " + usedKeyword.getKeyword());
            }
            
        } catch (SQLException e) {
            System.err.println("❌ used_keyword 테이블 저장 오류: " + e.getMessage());
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
        
        String sql = "SELECT COUNT(*) FROM used_keyword WHERE keyword = ? AND created_at >= ?";
        
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
    
    /**
     * 3글자 이상 키워드 검증 (종합 검증: AI 하이브리드 + 모든 길이별 규칙 적용)
     */
    private boolean validateLongKeyword(String keyword, String productTitle) {
        System.out.println("   🔍 3글자 이상 키워드 종합 검증 시작");
        
        // 1단계: AI 전체 검증
        if (SimilarityService.isRelevant(keyword, productTitle)) {
            System.out.println("   ✅ [1단계] AI 전체 검증 통과");
            return true;
        }
        
        // 2단계: 부분 문자열 조합 분석
        if (isRelevantBySubstringAnalysis(keyword, productTitle)) {
            System.out.println("   ✅ [2단계] 부분 문자열 조합 분석 통과");
            return true;
        }
        
        // 3단계: 개별 글자 빈도수 검증 (1글자 규칙 적용)
        System.out.println("   🔍 [3단계] 개별 글자 빈도수 검증 시작");
        if (validateLongKeywordByCharFrequency(keyword, productTitle)) {
            System.out.println("   ✅ [3단계] 개별 글자 빈도수 검증 통과");
            return true;
        }
        
        // 4단계: 연속된 2~4글자 조합 빈도수 검증 (확장된 조합 규칙 적용)
        System.out.println("   🔍 [4단계] 연속된 2~4글자 조합 빈도수 검증 시작");
        if (validateLongKeywordByMultiCharFrequency(keyword, productTitle)) {
            System.out.println("   ✅ [4단계] 연속된 2~4글자 조합 빈도수 검증 통과");
            return true;
        }
        
        System.out.println("   ❌ 모든 단계 검증 실패");
        return false;
    }
    
    /**
     * 3글자 이상 키워드의 개별 글자들의 빈도수 검증 (1글자 규칙 확장 적용)
     */
    private boolean validateLongKeywordByCharFrequency(String keyword, String productTitle) {
        // 상품명을 단어 단위로 분리 (특수문자, 공백 기준)
        String[] titleWords = productTitle.toLowerCase().split("[^가-힣a-zA-Z0-9]+");
        
        int passedCharCount = 0;
        int totalCharCount = keyword.length();
        
        // 각 글자별로 빈도수 검사
        for (int i = 0; i < keyword.length(); i++) {
            String singleChar = String.valueOf(keyword.charAt(i));
            long count = Arrays.stream(titleWords)
                .filter(word -> word.equals(singleChar.toLowerCase()))
                .count();
            
            System.out.printf("      - 글자 '%s' 포함 횟수: %d회\\n", singleChar, count);
            
            if (count >= 2) {
                passedCharCount++;
            }
        }
        
        System.out.println("      📊 2회 이상 포함된 글자: " + passedCharCount + "/" + totalCharCount);
        
        // 전체 글자 수의 30% 이상이 2회 이상 등장하면 통과
        double passRate = (double) passedCharCount / totalCharCount;
        boolean isPassed = passRate >= 0.3;
        
        System.out.printf("      📈 통과율: %.1f%% (기준: 30%% 이상) → %s\\n", 
            passRate * 100, isPassed ? "통과" : "실패");
        
        return isPassed;
    }
    
    /**
     * 3글자 이상 키워드의 연속된 2~4글자 조합들의 빈도수 검증 (확장된 조합 규칙 적용)
     */
    private boolean validateLongKeywordByMultiCharFrequency(String keyword, String productTitle) {
        if (keyword.length() < 2) {
            return false;
        }
        
        // 상품명을 단어 단위로 분리 (특수문자, 공백 기준)
        String[] titleWords = productTitle.toLowerCase().split("[^가-힣a-zA-Z0-9]+");
        
        int totalPassedCount = 0;
        int totalCombinationCount = 0;
        
        // 2글자부터 4글자까지 또는 키워드 길이까지 (둘 중 작은 값)
        int maxLength = Math.min(4, keyword.length());
        
        for (int length = 2; length <= maxLength; length++) {
            System.out.printf("      🔍 %d글자 조합 검증 시작\\n", length);
            
            int passedCount = 0;
            int combinationCount = keyword.length() - length + 1;
            
            // 해당 길이의 모든 연속 조합 검사
            for (int i = 0; i <= keyword.length() - length; i++) {
                String combination = keyword.substring(i, i + length);
                
                // 조합의 각 글자가 상품명에 몇 번 등장하는지 체크
                int charPassedCount = 0;
                System.out.printf("        - %d글자 조합 '%s': ", length, combination);
                
                for (int j = 0; j < combination.length(); j++) {
                    String singleChar = String.valueOf(combination.charAt(j));
                    long count = Arrays.stream(titleWords)
                        .filter(word -> word.equals(singleChar.toLowerCase()))
                        .count();
                    
                    System.out.printf("'%s'(%d회) ", singleChar, count);
                    
                    if (count >= 2) {
                        charPassedCount++;
                    }
                }
                
                // 조합의 글자 중 30% 이상이 2회 이상 등장하면 이 조합은 통과
                double charPassRate = (double) charPassedCount / combination.length();
                if (charPassRate >= 0.3) {
                    passedCount++;
                    System.out.println(" → ✅ 조합 통과");
                } else {
                    System.out.println(" → ❌ 조합 실패");
                }
            }
            
            totalPassedCount += passedCount;
            totalCombinationCount += combinationCount;
            
            System.out.printf("      📊 %d글자 조합 통과: %d/%d\\n", length, passedCount, combinationCount);
        }
        
        System.out.printf("      📊 전체 조합 통과: %d/%d\\n", totalPassedCount, totalCombinationCount);
        
        // 전체 조합의 35% 이상이 통과하면 성공
        double totalPassRate = (double) totalPassedCount / totalCombinationCount;
        boolean isPassed = totalPassRate >= 0.35;
        
        System.out.printf("      📈 전체 통과율: %.1f%% (기준: 35%% 이상) → %s\\n", 
            totalPassRate * 100, isPassed ? "통과" : "실패");
        
        return isPassed;
    }
    
    /**
     * 2글자 키워드 검증
     */
    private boolean validateTwoCharKeyword(String keyword, String productTitle) {
        String char1 = String.valueOf(keyword.charAt(0));
        String char2 = String.valueOf(keyword.charAt(1));
        
        // 상품명을 단어 단위로 분리 (특수문자, 공백 기준)
        String[] titleWords = productTitle.toLowerCase().split("[^가-힣a-zA-Z0-9]+");
        
        int passedCharCount = 0;
        
        // 첫 번째 글자 빈도수 검사
        long count1 = Arrays.stream(titleWords)
            .filter(word -> word.equals(char1.toLowerCase()))
            .count();
        System.out.printf("      - 글자 '%s' 포함 횟수: %d회\n", char1, count1);
        if (count1 >= 2) {
            passedCharCount++;
        }
        
        // 두 번째 글자 빈도수 검사
        long count2 = Arrays.stream(titleWords)
            .filter(word -> word.equals(char2.toLowerCase()))
            .count();
        System.out.printf("      - 글자 '%s' 포함 횟수: %d회\n", char2, count2);
        if (count2 >= 2) {
            passedCharCount++;
        }
        
        System.out.println("   📊 2회 이상 포함된 글자 종류 수: " + passedCharCount + "개");
        return passedCharCount >= 1; // 2회 이상 포함된 글자가 1종류 이상이면 성공
    }
    
    /**
     * 1글자 키워드 검증
     */
    private boolean validateOneCharKeyword(String keyword, String productTitle) {
        // 상품명을 단어 단위로 분리 (특수문자, 공백 기준)
        String[] titleWords = productTitle.toLowerCase().split("[^가-힣a-zA-Z0-9]+");
        
        // 키워드(한 글자)가 몇 번 나타나는지 카운트
        long count = Arrays.stream(titleWords)
            .filter(word -> word.equals(keyword.toLowerCase()))
            .count();
        
        System.out.printf("   📊 단어 '%s' 포함 횟수: %d회\n", keyword, count);
        return count >= 2; // 2번 이상 나타나면 성공
    }
    
    /**
     * 부분 문자열 조합 분석 (키워드 분해 후 유사도 검증)
     */
    private boolean isRelevantBySubstringAnalysis(String keyword, String productTitle) {
        System.out.println("🔄 [백업 로직] 부분 문자열 조합 분석 시작...");
        
        // 1. 부분 문자열 풀 생성
        List<String> substrings = new ArrayList<>();
        for (int i = 0; i < keyword.length(); i++) {
            for (int j = i + 1; j <= keyword.length(); j++) {
                substrings.add(keyword.substring(i, j));
            }
        }
        
        // 2. 최적화: 긴 문자열부터, 2글자 이상만 검사하도록 정렬
        substrings.sort((s1, s2) -> s2.length() - s1.length());
        
        int matchCount = 0;
        Set<String> matchedSubstrings = new HashSet<>();
        
        // 3. 순차적 검증
        for (String sub : substrings) {
            // 너무 짧거나, 원래 키워드와 같은 경우는 건너뛰기 (1단계에서 이미 했으므로)
            if (sub.length() < 2 || sub.equals(keyword)) {
                continue;
            }
            
            System.out.println("      - 부분 문자열 '" + sub + "' 검증 중...");
            if (SimilarityService.isRelevant(sub, productTitle)) {
                // 더 긴 조각이 이미 매칭되었다면, 짧은 조각은 무시 (예: '배드민턴'이 매칭되면 '배드'는 카운트 안함)
                boolean isRedundant = false;
                for (String matched : matchedSubstrings) {
                    if (matched.contains(sub)) {
                        isRedundant = true;
                        break;
                    }
                }
                
                if (!isRedundant) {
                    System.out.println("   🎯 부분 문자열 매칭: " + sub);
                    matchCount++;
                    matchedSubstrings.add(sub);
                    
                    // 하나라도 성공하면 즉시 성공 반환
                    System.out.println("   ✅ 부분 문자열 '" + sub + "'에서 연관성 발견!");
                    return true;
                }
            }
        }
        
        System.out.println("   ❌ 모든 부분 문자열 조합에서 연관성 찾지 못함.");
        return false;
    }
}