package com.kdt.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class NaverShoppingBestService {

    /**
     * 네이버 쇼핑 베스트 API에서 1-20위 키워드를 순수하게 가져오기만 합니다.
     * 검증은 메인 크롤러에서 별도로 수행합니다.
     * 
     * @return 1-20위 키워드 리스트, 실패 시 빈 리스트
     */
    public java.util.List<String> getBestKeywords() {
        try {
            System.out.println("📊 네이버 쇼핑 베스트 API에서 1-20위 키워드 수집 중...");
            
            // API를 통해 20개 키워드 모두 추출
            java.util.List<String> keywords = fetchKeywordsFromAPI();
            
            if (keywords.isEmpty()) {
                System.err.println("❌ API에서 키워드 추출 실패");
                return new java.util.ArrayList<>();
            }
            
            System.out.println("✅ API에서 " + keywords.size() + "개 키워드 추출 성공");
            return keywords;
            
        } catch (Exception e) {
            System.err.println("❌ [네이버 쇼핑 베스트 API 호출 실패] 예외 발생: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * 네이버 쇼핑 베스트 API에서 1-20위 키워드를 모두 추출합니다.
     * @return 1-20위 키워드 리스트, 실패 시 빈 리스트
     */
    private java.util.List<String> fetchKeywordsFromAPI() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            
            // API URL 구성 (20개 키워드 전체)
            String apiUrl = "https://snxbest.naver.com/api/v1/snxbest/keyword/rank"
                    + "?ageType=ALL"
                    + "&categoryId=A"
                    + "&sortType=KEYWORD_POPULAR"
                    + "&periodType=DAILY";
            
            // HTTP GET 요청 생성
            HttpGet httpGet = new HttpGet(apiUrl);
            
            // 헤더 설정 (Chrome 브라우저로 위장)
            httpGet.setHeader("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            httpGet.setHeader("Referer", 
                "https://snxbest.naver.com/keyword/best");
            httpGet.setHeader("Accept", "*/*");
            httpGet.setHeader("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
            httpGet.setHeader("Accept-Encoding", "gzip, deflate, br, zstd");
            
            System.out.println("🌐 API 요청 URL: " + apiUrl);
            
            // HTTP 요청 실행
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    System.out.println("✅ API 응답 성공 (200 OK)");
                    
                    // JSON 배열 파싱하여 title 필드들 추출
                    return parseKeywordsFromResponse(responseBody);
                } else {
                    System.err.println("❌ API 요청 실패 - HTTP 상태 코드: " + statusCode);
                    return new java.util.ArrayList<>();
                }
            }
            
        } catch (IOException e) {
            System.err.println("❌ API 요청 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * JSON 배열 응답에서 키워드 리스트를 추출합니다.
     * @param responseBody JSON 배열 응답 문자열
     * @return 추출된 키워드 리스트, 실패 시 빈 리스트
     */
    private java.util.List<String> parseKeywordsFromResponse(String responseBody) {
        java.util.List<String> keywords = new java.util.ArrayList<>();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);
            
            // JSON 배열인지 확인
            if (rootNode.isArray()) {
                System.out.println("🔍 JSON 배열에서 키워드 추출 시작...");
                
                // 배열 순회하며 title 필드 추출
                for (JsonNode keywordNode : rootNode) {
                    JsonNode titleNode = keywordNode.get("title");
                    JsonNode rankNode = keywordNode.get("rank");
                    
                    if (titleNode != null && !titleNode.isNull()) {
                        String keyword = titleNode.asText().trim();
                        int rank = rankNode != null ? rankNode.asInt() : keywords.size() + 1;
                        
                        keywords.add(keyword);
                        System.out.println("✅ " + rank + "위: " + keyword);
                    }
                }
                
                System.out.println("🔍 총 " + keywords.size() + "개 키워드 추출 완료");
            } else {
                System.err.println("❌ JSON 응답이 배열 형식이 아닙니다");
            }
            
        } catch (Exception e) {
            System.err.println("❌ JSON 파싱 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        
        return keywords;
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