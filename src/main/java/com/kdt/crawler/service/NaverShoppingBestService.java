package com.kdt.crawler.service;

import com.kdt.crawler.model.KeywordRankPair;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import java.util.ArrayList;
import java.util.List;

public class NaverShoppingBestService {

    /**
     * 네이버 쇼핑 베스트 상품 페이지에서 검증된 키워드를 순차적으로 찾아 반환합니다.
     * 
     * 검증 과정:
     * 1. 30일 중복 방지 검사
     * 2. 키워드 유효성 검증 (싸다구몰에서 실제 상품 연관성 검사)
     * 3. 한/영 변환 시도 (선택사항)
     * 
     * @param page Playwright Page 객체
     * @param keywordService 키워드 중복 검사 및 검증 서비스
     * @return 검증된 키워드 문자열, 실패 시 null
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
            
            // 6. 네이버 쇼핑 베스트에서 1-20위 키워드를 한번에 모두 추출
            System.out.println("🔍 1-20위 키워드를 한번에 수집 중...");
            
            // 최종 페이지 로딩 대기
            page.waitForTimeout(1000);
            
            // 1-20위 키워드를 모두 추출하여 리스트에 저장 (임시)
            List<KeywordRankPair> keywordList = new ArrayList<>();
            for (int rank = 1; rank <= 20; rank++) {
                String keyword = extractKeywordByRank(page, rank);
                if (keyword != null && isValidKeyword(keyword)) {
                    keywordList.add(new KeywordRankPair(keyword, rank));
                    System.out.println("✅ " + rank + "위 키워드 추출 성공: " + keyword);
                } else {
                    System.out.println("⚠️ " + rank + "위 키워드 추출 실패 또는 기본 유효성 검사 실패");
                }
            }
            
            System.out.println("📊 총 " + keywordList.size() + "개 키워드 추출 완료");
            
            if (keywordList.isEmpty()) {
                System.err.println("❌ 추출된 키워드가 없습니다.");
                return null;
            }
            
            // 7. 추출된 키워드들을 순차적으로 검증 (나머지 19개는 버림)
            System.out.println("🔍 추출된 키워드들을 순차적으로 검증 시작...");
            
            int validationAttempts = 0;
            final int MAX_VALIDATION_ATTEMPTS = 20; // 안전장치: 최대 20회 검증 시도
            
            for (KeywordRankPair keywordPair : keywordList) {
                if (validationAttempts >= MAX_VALIDATION_ATTEMPTS) {
                    System.err.println("⚠️ [안전장치 발동] 최대 검증 시도 횟수에 도달");
                    break;
                }
                
                String keyword = keywordPair.keyword;
                int rank = keywordPair.rank;
                
                System.out.println("\n🔍 " + rank + "위 키워드 '" + keyword + "' 검증 시작...");
                
                // 단계 1: 30일 중복 검사
                boolean isUsedRecently = keywordService.isKeywordUsedRecently(keyword);
                
                if (isUsedRecently) {
                    System.out.println("❌ " + rank + "위 키워드 '" + keyword + "'는 최근 30일 내 사용됨 - 다음 키워드 확인");
                    continue;
                }
                
                System.out.println("🔍 키워드 '" + keyword + "' - 30일 중복 검사 통과, 유효성 검증 시작...");
                validationAttempts++;
                
                // 단계 2: 키워드 유효성 검증 (실제 상품 연관성 검사)
                if (keywordService.validateKeyword(page, keyword)) {
                    System.out.println("🎉 최종 선택된 키워드: " + keyword + " (순위: " + rank + "위, 모든 검증 통과)");
                    return keyword; // 선택된 1개만 반환, 나머지 19개는 자동으로 버려짐
                } else {
                    System.out.println("❌ 키워드 '" + keyword + "' 유효성 검증 실패 - 대체 언어 시도");
                    
                    // 단계 3: 한/영 변환 시도
                    String alternativeKeyword = keywordService.validateWithAlternativeLanguage(page, keyword);
                    if (alternativeKeyword != null) {
                        System.out.println("🎉 대체 언어 키워드로 최종 선택: " + alternativeKeyword + " (원본: " + keyword + ", 순위: " + rank + "위)");
                        return alternativeKeyword; // 선택된 1개만 반환, 나머지 19개는 자동으로 버려짐
                    }
                    
                    System.out.println("❌ 키워드 '" + keyword + "' 대체 언어 검증도 실패 - 다음 키워드 확인");
                }
            }
            
            // 안전장치 도달 시 추가 로그
            if (validationAttempts >= MAX_VALIDATION_ATTEMPTS) {
                System.err.println("⚠️ [안전장치 발동] 키워드 검증 시도 횟수가 " + MAX_VALIDATION_ATTEMPTS + "회에 도달하여 작업을 중단합니다.");
            }
            
            // 모든 순위 확인 후에도 찾지 못한 경우
            System.err.println("❌ [키워드 검증 실패] 1-20위 키워드 중 모든 검증을 통과한 키워드를 찾지 못했습니다.");
            System.err.println("📊 검증 통계:");
            System.err.println("   - 검증 시도 횟수: " + validationAttempts + "/" + MAX_VALIDATION_ATTEMPTS);
            System.err.println("💡 문제 해결 방법:");
            System.err.println("   1. 네이버 쇼핑 베스트 페이지 구조가 변경되었을 수 있습니다.");
            System.err.println("   2. 싸다구몰 사이트 구조가 변경되어 키워드 검증이 실패할 수 있습니다.");
            System.err.println("   3. used_keywords 테이블을 확인하여 중복 데이터를 정리해야 할 수 있습니다.");
            System.err.println("   4. 언어 변환 매핑 테이블에 더 많은 키워드를 추가해야 할 수 있습니다.");
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