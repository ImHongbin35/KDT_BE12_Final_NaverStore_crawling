package com.kdt.crawler.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

public class NaverShoppingBestService {

    /**
     * 네이버 쇼핑 베스트 상품 페이지에서 중복되지 않은 키워드를 크롤링합니다.
     * @param page Playwright Page 객체
     * @param keywordService 중복 검사 서비스
     * @return 중복되지 않은 키워드 문자열
     */
    public String getBestKeyword(Page page, com.kdt.crawler.service.KeywordService keywordService) {
        try {
            System.out.println("📊 네이버 쇼핑 베스트 상품 페이지에서 키워드 수집 중...");
            
            // 1. 네이버 쇼핑 베스트 페이지 접속
            page.navigate("https://snxbest.naver.com/product/best/buy");
            System.out.println("🌐 네이버 쇼핑 베스트 페이지 접속 완료");
            
            // 페이지 로딩 대기
            page.waitForLoadState(LoadState.LOAD);
            page.waitForTimeout(2000);
            
            // 2. 베스트키워드 버튼 클릭
            System.out.println("🔄 베스트키워드 버튼 클릭 중...");
            String bestKeywordSelector = "a[href='/keyword/best']";
            
            if (page.locator(bestKeywordSelector).count() > 0) {
                page.locator(bestKeywordSelector).click();
                System.out.println("✅ 베스트키워드 버튼 클릭 성공");
                page.waitForTimeout(1500);
            } else {
                System.err.println("❌ [단계 2 - 베스트키워드 버튼 클릭 실패] 선택자를 찾지 못했습니다: " + bestKeywordSelector);
                System.err.println("💡 문제 해결 방법: 네이버 쇼핑 페이지 구조가 변경되었거나 로딩이 완료되지 않았을 수 있습니다.");
                return null;
            }
            
            // 3. 인기키워드 버튼 클릭
            System.out.println("🔄 인기키워드 버튼 클릭 중...");
            String bestTabSelector = ".tabListResponsive_link__GhTVh.tabListResponsive_active__EQKv4";
            
            if (page.locator(bestTabSelector).count() > 0) {
                page.locator(bestTabSelector).click();
                System.out.println("✅ 인기키워드 버튼 클릭 성공");
                page.waitForTimeout(1500);
            } else {
                System.err.println("❌ [단계 3 - 인기키워드 버튼 클릭 실패] 선택자를 찾지 못했습니다: " + bestTabSelector);
                System.err.println("💡 문제 해결 방법: 인기키워드 페이지로 이동이 실패했거나 페이지 구조가 변경되었을 수 있습니다.");
                return null;
            }
            
            // 4. 대분류 카테고리 버튼 클릭
            System.out.println("🔄 대분류 카테고리 버튼 클릭 중...");
            String categoryButtonSelector = "button.titleResponsive_custom_button__CA_BD";
            
            if (page.locator(categoryButtonSelector).count() > 0) {
                page.locator(categoryButtonSelector).first().click();
                System.out.println("✅ 대분류 카테고리 버튼 클릭 성공");
                page.waitForTimeout(1000);
            } else {
                System.err.println("❌ [단계 4 - 대분류 카테고리 버튼 클릭 실패] 선택자를 찾지 못했습니다: " + categoryButtonSelector);
                System.err.println("💡 문제 해결 방법: 인기키워드 페이지로 이동이 실패했거나 페이지 구조가 변경되었을 수 있습니다.");
                return null;
            }
            
            // 5. 전체 연령 항목 클릭
            System.out.println("🔄 전체 연령 항목 클릭 중...");
            String allAgeSelector = "label[for=\"ALL\"]";
            
            if (page.locator(allAgeSelector).count() > 0) {
                page.locator(allAgeSelector).click();
                System.out.println("✅ 전체 연령 항목 클릭 성공");
                page.waitForTimeout(2000);
            } else {
                System.err.println("❌ [단계 5 - 전체 연령 항목 클릭 실패] 선택자를 찾지 못했습니다: " + allAgeSelector);
                System.err.println("💡 문제 해결 방법: 대분류 카테고리 레이어가 열리지 않았거나 페이지 구조가 변경되었을 수 있습니다.");
                return null;
            }
            
            // 6. 순위별 키워드 수집 (1위부터 최대 20위까지)
            System.out.println("🔍 순위별 키워드 수집 및 중복 검사 중...");
            
            // 최종 페이지 로딩 대기
            page.waitForTimeout(1000);
            
            // 최대 20위까지 확인
            for (int rank = 1; rank <= 20; rank++) {
                String keyword = extractKeywordByRank(page, rank);
                
                if (keyword != null && isValidKeyword(keyword)) {
                    System.out.println("✅ " + rank + "위 키워드 발견: " + keyword);
                    
                    // 중복 검사
                    boolean isUsedRecently = keywordService.isKeywordUsedRecently(keyword);
                    
                    if (!isUsedRecently) {
                        System.out.println("🎉 최종 선택된 키워드: " + keyword + " (순위: " + rank + "위, 최근 30일 내 중복 없음)");
                        return keyword;
                    } else {
                        System.out.println("❌ " + rank + "위 키워드 '" + keyword + "'는 최근 30일 내 사용됨 - 다음 순위 확인");
                    }
                } else {
                    System.out.println("⚠️ " + rank + "위 키워드 추출 실패 또는 유효하지 않음");
                }
            }
            
            // 모든 순위 확인 후에도 찾지 못한 경우
            System.err.println("❌ [단계 6 - 키워드 추출 실패] 1-20위 모든 키워드가 최근 30일 내 사용되었거나 추출에 실패했습니다.");
            System.err.println("💡 문제 해결 방법:");
            System.err.println("   1. 네이버 쇼핑 베스트 페이지 구조가 변경되었을 수 있습니다.");
            System.err.println("   2. 키워드 선택자를 업데이트해야 할 수 있습니다.");
            System.err.println("   3. used_keywords 테이블을 확인하여 중복 데이터를 정리해야 할 수 있습니다.");
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ [네이버 쇼핑 베스트 크롤링 전체 실패] 예외 발생: " + e.getMessage());
            System.err.println("💡 문제 해결 방법:");
            System.err.println("   1. 네트워크 연결을 확인하세요.");
            System.err.println("   2. 네이버 쇼핑 사이트 접속을 확인하세요: https://snxbest.naver.com/product/best/buy");
            System.err.println("   3. 브라우저 드라이버가 정상적으로 작동하는지 확인하세요.");
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 특정 순위의 키워드를 추출합니다.
     * @param page Playwright Page 객체
     * @param rank 추출할 순위 (1부터 시작)
     * @return 해당 순위의 키워드 문자열, 실패 시 null
     */
    private String extractKeywordByRank(Page page, int rank) {
        try {
            // data-shp-contents-rank 속성을 사용한 선택자 (인기키워드 페이지용)
            String rankSelector = "[data-shp-contents-rank=\"" + rank + "\"]";
            
            if (page.locator(rankSelector).count() > 0) {
                // HTML 구조 분석: <div data-shp-contents-rank="1">...<strong class="rankingTitleResponsive_title__z9c4V">라부부</strong>...</div>
                String[] keywordSelectors = {
                    rankSelector + " strong.rankingTitleResponsive_title__z9c4V",
                    rankSelector + " .rankingTitleResponsive_title__z9c4V",
                    rankSelector + " strong.rankingTitleResponsive_title__eQVqr",
                    rankSelector + " .rankingTitleResponsive_title__eQVqr",
                    rankSelector + " [class*='rankingTitleResponsive_title']",
                    rankSelector + " strong"
                };
                
                for (String selector : keywordSelectors) {
                    try {
                        if (page.locator(selector).count() > 0) {
                            String keyword = page.locator(selector).first().textContent();
                            if (keyword != null && !keyword.trim().isEmpty()) {
                                return keyword.trim();
                            }
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
                
                // 최후의 수단: JavaScript로 해당 요소 내에서 텍스트 추출
                try {
                    Object result = page.evaluate("() => {" +
                        "const element = document.querySelector('[data-shp-contents-rank=\"" + rank + "\"] strong.rankingTitleResponsive_title__z9c4V');" +
                        "return element ? element.textContent.trim() : null;" +
                    "}");
                    
                    if (result instanceof String && !((String) result).isEmpty()) {
                        return (String) result;
                    }
                } catch (Exception e) {
                    // JavaScript 실행 실패 시 무시
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 키워드 유효성 검증
     */
    private boolean isValidKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        keyword = keyword.trim();
        
        // 길이 체크
        if (keyword.length() < 2 || keyword.length() > 20) {
            return false;
        }
        
        // 숫자만 있는 키워드 제외
        if (keyword.matches("^[0-9]+$")) {
            return false;
        }
        
        // 특수문자나 HTML 태그 제외
        if (keyword.contains("<") || keyword.contains(">") || keyword.contains("&") || 
            keyword.contains("javascript") || keyword.contains("function")) {
            return false;
        }
        
        // 네비게이션 관련 키워드 제외
        String[] invalidKeywords = {
            "더보기", "검색", "베스트", "인기", "순위", "전체", "카테고리", 
            "필터", "정렬", "페이지", "이동", "클릭", "선택", "버튼",
            "레이어", "열기", "닫기", "메뉴", "네비게이션"
        };
        
        for (String invalid : invalidKeywords) {
            if (keyword.toLowerCase().contains(invalid.toLowerCase())) {
                return false;
            }
        }
        
        return true;
    }
    
}