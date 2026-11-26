package com.kdt.crawler;

import com.kdt.crawler.model.CrawlJobType;
import com.kdt.crawler.model.ProductInfo;
import com.kdt.crawler.service.DatabaseService;
import com.kdt.crawler.service.KeywordService;
import com.kdt.crawler.service.NaverShoppingBestService;
import com.kdt.crawler.service.SsadaguCrawlerService;
import com.kdt.crawler.util.FileUtils;
import com.microsoft.playwright.*;
import java.util.Collections;
import java.util.List;

public class SsadaguCrawler {

    public static void main(String[] args) {
        // 크롤링 타입 설정 (실행 인자 방식)
        CrawlJobType crawlType = CrawlJobType.AUTOMATIC; // 기본값

        if (args.length > 0) {
            try {
                crawlType = CrawlJobType.valueOf(args[0].toUpperCase());
                System.out.println("✅ 실행 인자로 크롤링 타입 설정: " + crawlType.name());
            } catch (IllegalArgumentException e) {
                System.err.println("⚠️ 경고: 유효하지 않은 타입 '" + args[0] + "'. 기본값 AUTOMATIC으로 실행합니다.");
                System.err.println("💡 사용법: java -jar crawler.jar [AUTOMATIC|MANUAL]");
            }
        } else {
            System.out.println("ℹ️ 실행 인자 없음. 기본값 AUTOMATIC으로 실행합니다.");
        }

        System.out.println("🚀 === 싸다구몰 상품 정보 크롤러 시작 (" + crawlType.name() + ") ===");
        
        try (Playwright playwright = Playwright.create()) {
            // 브라우저 설정
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(false) // 브라우저 화면 보이도록 설정
                    .setSlowMo(2000)    // 액션 간 2초 대기 (안정성 향상)
                    .setArgs(Collections.singletonList("--lang=ko-KR"));

            Browser browser = playwright.chromium().launch(launchOptions);
            
            // 새로운 컨텍스트와 페이지 생성
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080));
            
            Page page = context.newPage();
            
            // 서비스 클래스 인스턴스 생성
            NaverShoppingBestService bestService = new NaverShoppingBestService();
            SsadaguCrawlerService crawlerService = new SsadaguCrawlerService();
            DatabaseService dbService = new DatabaseService();
            KeywordService keywordService = new KeywordService();
            
            try {
                // 1단계: 네이버 쇼핑 베스트에서 20개 키워드 가져오기
                System.out.println("\n📊 1단계: 네이버 쇼핑 베스트에서 1-20위 키워드 수집 중...");
                FileUtils.saveLog("네이버 쇼핑 베스트 API 크롤링 시작");
                
                java.util.List<String> bestKeywords = bestService.getBestKeywords();
                
                if (bestKeywords.isEmpty()) {
                    System.out.println("⚠️ 베스트 키워드를 가져오지 못했습니다. 프로그램을 종료합니다.");
                    FileUtils.saveLog("네이버 쇼핑 베스트 키워드 수집 실패 - 프로그램 종료");
                    return;
                } else {
                    System.out.println("✅ 베스트 키워드 " + bestKeywords.size() + "개를 수집했습니다");
                    FileUtils.saveLog("네이버 쇼핑 베스트 키워드 수집 완료: " + bestKeywords.size() + "개");
                    
                    // 키워드 목록 출력
                    System.out.println("\n=== 수집된 네이버 쇼핑 베스트 키워드 ===");
                    for (int i = 0; i < bestKeywords.size(); i++) {
                        System.out.println((i + 1) + "위: " + bestKeywords.get(i));
                    }
                }
                
                // 2단계: 키워드들을 순차적으로 검증하여 첫 번째 유효한 키워드 선택
                System.out.println("\n🔍 2단계: 키워드 순차 검증 시작...");
                String selectedKeyword = null;
                
                for (int i = 0; i < bestKeywords.size(); i++) {
                    String keyword = bestKeywords.get(i);
                    int rank = i + 1;
                    
                    System.out.println("\n🔍 " + rank + "위 키워드 '" + keyword + "' 검증 시작...");
                    FileUtils.saveLog("키워드 검증 시작: " + rank + "위 " + keyword);
                    
                    try {
                        // 2-1. 기본 유효성 검증 (간단한 필터링)
                        if (!isBasicValidKeyword(keyword)) {
                            System.out.println("❌ " + rank + "위 키워드 '" + keyword + "' 기본 유효성 검증 실패 - 다음 키워드 확인");
                            continue;
                        }
                        
                        // 2-2. 30일 중복 검사
                        boolean isUsedRecently = keywordService.isKeywordUsedRecently(keyword);
                        if (isUsedRecently) {
                            System.out.println("❌ " + rank + "위 키워드 '" + keyword + "'는 최근 30일 내 사용됨 - 다음 키워드 확인");
                            continue;
                        }
                        System.out.println("✅ " + rank + "위 키워드 '" + keyword + "' - 30일 중복 검사 통과");
                        
                        // 2-3. 실제 상품 연관성 검사 (Playwright 사용)
                        System.out.println("🔍 " + rank + "위 키워드 '" + keyword + "' 실제 상품 연관성 검사 중...");
                        if (keywordService.validateKeyword(page, keyword)) {
                            System.out.println("🎉 최종 선택된 키워드: " + keyword + " (순위: " + rank + "위, 모든 검증 통과)");
                            selectedKeyword = keyword;
                            FileUtils.saveLog("최종 선택된 키워드: " + rank + "위 " + keyword + " (모든 검증 통과)");
                            break;
                        } else {
                            System.out.println("❌ 키워드 '" + keyword + "' 실제 상품 연관성 검사 실패 - 대체 언어 시도");
                            
                            // 2-4. 한/영 변환 시도
                            String alternativeKeyword = keywordService.validateWithAlternativeLanguage(page, keyword);
                            if (alternativeKeyword != null) {
                                System.out.println("🎉 대체 언어 키워드로 최종 선택: " + alternativeKeyword + " (원본: " + keyword + ", 순위: " + rank + "위)");
                                selectedKeyword = alternativeKeyword;
                                FileUtils.saveLog("최종 선택된 키워드: " + alternativeKeyword + " (대체언어, 원본: " + keyword + ")");
                                break;
                            }
                            
                            System.out.println("❌ 키워드 '" + keyword + "' 대체 언어 검증도 실패 - 다음 키워드 확인");
                        }
                        
                    } catch (Exception e) {
                        System.err.println("❌ " + rank + "위 키워드 '" + keyword + "' 검증 중 오류 발생: " + e.getMessage());
                        continue;
                    }
                }
                
                // 모든 키워드 검증 실패한 경우
                if (selectedKeyword == null) {
                    System.err.println("❌ 1-20위 키워드 중 모든 검증을 통과한 키워드가 없습니다. 프로그램을 종료합니다.");
                    FileUtils.saveLog("모든 키워드 검증 실패 - 프로그램 종료");
                    return;
                }
                
                // 선택된 키워드 확인 (실제 저장은 상품 정보와 함께 6단계에서 수행)
                System.out.println("\n✅ 3단계: 선택된 키워드 확인 완료: '" + selectedKeyword + "'");
                
                // 4단계: 싸다구몰에서 상품 정보 크롤링
                System.out.println("\n🛒 4단계: 싸다구몰에서 '" + selectedKeyword + "' 상품 정보 수집 중...");
                FileUtils.saveLog("싸다구몰 크롤링 시작 - 키워드: " + selectedKeyword);
                
                ProductInfo productInfo = crawlerService.crawlProductInfo(page, selectedKeyword);
                
                // 5단계: 결과 출력 및 검증
                System.out.println("\n📋 5단계: 수집된 정보 검증 및 출력");
                validateAndDisplayResults(productInfo);


                // txt파일 저장 -> DB 저장 변경

                // crawl_type 설정 (실행 인자로 받은 타입 사용)
                productInfo.setCrawlType(crawlType.name());

                // 6단계: 데이터베이스에 저장
                System.out.println("\n💾 6단계: 결과 DB 저장 중...");
                dbService.saveProductInfo(productInfo); // 상품 정보를 product 테이블에 저장
                FileUtils.saveLog("크롤링 완료 및 DB 저장 - " + productInfo.getProductName()); // 로그는 그대로 파일에 저장
                //
                System.out.println("\n🎉 === 크롤링 작업이 완료되었습니다! ===");
                System.out.println("💾 키워드 및 상품 정보가 데이터베이스에 저장되었습니다.");
                System.out.println("🔍 키워드: " + selectedKeyword);
                System.out.println("📦 상품명: " + productInfo.getProductName());
                System.out.println("💰 가격: " + productInfo.getPrice());
                
            } catch (Exception e) {
                System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
                FileUtils.saveLog("크롤링 오류: " + e.getMessage());
                e.printStackTrace();
                
                // 오류 발생 시에도 기본 정보를 데이터베이스에 저장
                ProductInfo errorInfo = new ProductInfo();
                errorInfo.setKeyword("오류 발생");
                errorInfo.setProductName("크롤링 실패");
                errorInfo.setPrice("정보 없음");
                errorInfo.setCrawlType(crawlType.name());
                errorInfo.setCrawledAt(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                try {
                    dbService.saveProductInfo(errorInfo);
                    keywordService.saveUsedKeyword("오류 발생");
                } catch (Exception dbException) {
                    System.err.println("❌ 오류 정보 데이터베이스 저장 실패: " + dbException.getMessage());
                    // 데이터베이스 저장이 실패하면 파일로 저장
                    FileUtils.saveProductInfo(errorInfo);
                }
                
            } finally {
                // 브라우저 정리
                System.out.println("\n🔧 브라우저 정리 중...");
                page.waitForTimeout(3000); // 결과 확인을 위한 잠시 대기
                browser.close();
                System.out.println("✅ 브라우저가 정리되었습니다.");
            }
            
        } catch (PlaywrightException e) {
            System.err.println("❌ Playwright 초기화 오류: " + e.getMessage());
            System.err.println("💡 해결 방법:");
            System.err.println("   1. PlaywrightInstaller.java를 먼저 실행하세요.");
            System.err.println("   2. 인터넷 연결을 확인하세요.");
            System.err.println("   3. 바이러스 백신 소프트웨어를 일시 중지하세요.");
            e.printStackTrace();
        }
    }
    
    /**
     * 기본 키워드 유효성 검증 (간단한 필터링)
     */
    private static boolean isBasicValidKeyword(String keyword) {
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
    
    private static void validateAndDisplayResults(ProductInfo productInfo) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 수집된 상품 정보");
        System.out.println("=".repeat(60));
        
        // 키워드
        System.out.println("🔍 검색 키워드: " + productInfo.getKeyword());
        
        // 상품명
        String productName = productInfo.getProductName();
        if (productName != null && !productName.equals("정보 수집 실패") && !productName.equals("정보 없음")) {
            System.out.println("✅ 상품명: " + productName);
        } else {
            System.out.println("❌ 상품명: " + (productName != null ? productName : "정보 없음"));
        }
        
        // 가격
        String price = productInfo.getPrice();
        if (price != null && !price.equals("가격 정보 없음") && !price.equals("정보 없음")) {
            System.out.println("✅ 가격: " + price);
        } else {
            System.out.println("❌ 가격: " + (price != null ? price : "정보 없음"));
        }
        
        // 평점
        String rating = productInfo.getRating();
        if (rating != null && !rating.equals("평점 정보 없음") && !rating.equals("정보 없음")) {
            System.out.println("✅ 평점: " + rating);
        } else {
            System.out.println("❌ 평점: " + (rating != null ? rating : "정보 없음"));
        }
        
        // 재구매율 (설명에서 추출)
        String description = productInfo.getDescription();
        if (description != null && description.contains("재구매율")) {
            System.out.println("✅ 재구매율: " + description);
        }
        
        // 판매개수 (리뷰수에서 추출) - 새로운 테이블 구조에서 제거됨
        // String reviewCount = productInfo.getReviewCount();
        // if (reviewCount != null && !reviewCount.equals("판매 정보 없음") && !reviewCount.equals("정보 없음")) {
        //     System.out.println("✅ 판매개수: " + reviewCount);
        // }
        
        // URL
        String url = productInfo.getProductUrl();
        if (url != null && !url.equals("URL 정보 없음") && !url.equals("정보 없음")) {
            System.out.println("✅ 상품 URL: " + url);
        }
        
        
        // 수집 시간
        System.out.println("⏰ 수집 시간: " + productInfo.getCrawledAt());
        
        System.out.println("=".repeat(60));
        
        // 성공/실패 판단
        boolean isSuccess = (productName != null && !productName.equals("정보 수집 실패") && !productName.equals("정보 없음")) &&
                           (price != null && !price.equals("가격 정보 없음") && !price.equals("정보 없음"));
        
        if (isSuccess) {
            System.out.println("🎉 크롤링 성공! 주요 정보가 정상적으로 수집되었습니다.");
        } else {
            System.out.println("⚠️ 크롤링 부분 성공! 일부 정보만 수집되었습니다.");
            System.out.println("💡 다음 사항을 확인해보세요:");
            System.out.println("   - 싸다구몰 사이트의 구조가 변경되었을 수 있습니다.");
            System.out.println("   - 검색 결과에 상품이 없을 수 있습니다.");
            System.out.println("   - 네트워크 연결 상태를 확인하세요.");
        }
    }
}
