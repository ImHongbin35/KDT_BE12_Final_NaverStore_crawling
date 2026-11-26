package com.kdt.crawler.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Sentence-Transformers 기반 의미 유사도 계산 서비스
 * 
 * 기존 Komoran 형태소 분석을 대체하여 더 정확한 의미 기반 비교 제공
 * 
 * 주요 기능:
 * - Python Sentence-Transformers API 서버와 HTTP 통신
 * - 키워드와 상품명 간 의미적 유사도 계산
 * - 임계값 기반 연관성 판단
 */
public class SimilarityService {
    
    // Python API 서버 설정
    private static final String PYTHON_API_BASE_URL = "http://localhost:5000";
    private static final String SIMILARITY_ENDPOINT = "/calculate-similarity";
    private static final String HEALTH_ENDPOINT = "/health";
    
    // 유사도 임계값 설정 (다층 임계값) - 10% 상향 조정
    private static final double HIGH_SIMILARITY_THRESHOLD = 0.8; // 80% 이상: 확실한 연관성
    private static final double MEDIUM_SIMILARITY_THRESHOLD = 0.6; // 60% 이상: 보통 연관성  
    private static final double LOW_SIMILARITY_THRESHOLD = 0.45; // 45% 이상: 낮은 연관성
    
    // 기본 임계값 (하위 호환성)
    private static final double SIMILARITY_THRESHOLD = MEDIUM_SIMILARITY_THRESHOLD;
    
    // JSON 파서
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        System.out.println("✅ SimilarityService 초기화 완료 (Sentence-Transformers)");
        System.out.println("   Python API Server: " + PYTHON_API_BASE_URL);
        System.out.println("   Similarity Threshold: " + (SIMILARITY_THRESHOLD * 100) + "%");
        
        // API 서버 상태 확인
        if (isPythonApiServerHealthy()) {
            System.out.println("✅ Python API Server 연결 확인됨");
        } else {
            System.out.println("⚠️ Python API Server 연결 실패");
            System.out.println("   python_similarity_api/app.py를 먼저 실행해주세요.");
        }
    }
    
    /**
     * 키워드와 상품명 간의 연관성을 판단 (최종 3단계 검증 로직)
     * 
     * @param keyword 검색 키워드
     * @param productTitle 상품명
     * @return 연관성이 있으면 true, 없으면 false
     */
    public static boolean isRelevant(String keyword, String productTitle) {
        if (keyword == null || productTitle == null || keyword.trim().isEmpty() || productTitle.trim().isEmpty()) {
            return false;
        }
        
        // 키워드 전처리
        String processedKeyword = preprocessKeyword(keyword.trim());
        String processedTitle = preprocessProductTitle(productTitle.trim());
        
        System.out.println("🔍 최종 3단계 유사도 검증 시작");
        System.out.println("   원본 키워드: " + keyword);
        System.out.println("   전처리 키워드: " + processedKeyword);
        System.out.println("   원본 상품명: " + productTitle);
        System.out.println("   전처리 상품명: " + processedTitle);
        
        // [우선 검사] 키워드가 2~3글자인 경우: contains() 완전 일치 검사 먼저
        if (keyword.length() >= 2 && keyword.length() <= 3) {
            System.out.println("[1단계: 우선 검사] 2-3글자 키워드 완전 일치 검사");
            if (productTitle.contains(keyword)) {
                System.out.println("   ✅ 완전 일치 성공 → 즉시 연관성 인정");
                return true;
            }
            System.out.println("   ❌ 완전 일치 실패 → 다음 단계로");
        }
        
        // [메인 검증] AI 의미 분석 (조건: 1글자, 4글자+, 또는 1단계 실패한 2-3글자)
        if (keyword.length() == 1 || keyword.length() >= 4 || (keyword.length() >= 2 && keyword.length() <= 3)) {
            System.out.println("[2단계: 메인 검증] AI 의미 분석");
            try {
                double similarity = calculateSimilarity(processedKeyword, processedTitle);
                
                if (similarity >= 0) {
                    boolean isRelevant = evaluateWithMultiThreshold(similarity, processedKeyword, processedTitle);
                    
                    String confidenceLevel = getConfidenceLevel(similarity);
                    System.out.printf("   📊 유사도: %.3f (%.1f%%) [%s] → %s\n", 
                        similarity, similarity * 100, confidenceLevel, 
                        isRelevant ? "✅ AI 분석 성공" : "❌ AI 분석 실패");
                    
                    if (isRelevant) {
                        return true;
                    }
                } else {
                    System.out.println("   ❌ AI 분석 실패 (API 오류)");
                }
            } catch (Exception e) {
                System.out.println("   ❌ AI 분석 실패 (예외): " + e.getMessage());
            }
        } else {
            System.out.println("[2단계: 메인 검증] AI 분석 조건 불일치 - 건너뛰기");
        }
        
        // [최후 검증] 구조 분해 분석
        System.out.println("[3단계: 최후 검증] 구조 분해 분석");
        boolean structuralMatch = performStructuralDecomposition(keyword, productTitle);
        if (structuralMatch) {
            System.out.println("   ✅ 구조 분해 분석 성공 → 연관성 인정");
            return true;
        }
        
        System.out.println("   ❌ 모든 검증 단계 실패 → 연관성 없음");
        return false;
    }
    
    /**
     * Python API 서버에 유사도 계산 요청
     * 
     * @param text1 첫 번째 텍스트 (키워드)
     * @param text2 두 번째 텍스트 (상품명)
     * @return 유사도 점수 (0.0 ~ 1.0), 실패 시 -1.0
     */
    private static double calculateSimilarity(String text1, String text2) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            
            // HTTP POST 요청 생성
            HttpPost httpPost = new HttpPost(PYTHON_API_BASE_URL + SIMILARITY_ENDPOINT);
            httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
            
            // 요청 바디 생성
            String requestBody = String.format(
                "{\"text1\": \"%s\", \"text2\": \"%s\"}", 
                text1.replace("\"", "\\\""), 
                text2.replace("\"", "\\\"")
            );
            
            httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
            
            // API 호출
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                if (response.getStatusLine().getStatusCode() == 200) {
                    // JSON 응답 파싱
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    double similarity = jsonResponse.path("similarity").asDouble(-1.0);
                    
                    if (similarity >= 0.0 && similarity <= 1.0) {
                        return similarity;
                    } else {
                        System.err.println("❌ 유효하지 않은 유사도 값: " + similarity);
                    }
                } else {
                    System.err.println("❌ Python API 호출 실패: " + response.getStatusLine().getStatusCode());
                    System.err.println("   응답: " + responseBody);
                }
            }
            
        } catch (IOException e) {
            System.err.println("❌ Python API 통신 오류: " + e.getMessage());
        }
        
        return -1.0; // 실패
    }
    
    /**
     * Python API 서버 상태 확인
     * 
     * @return 서버가 정상이면 true, 아니면 false
     */
    private static boolean isPythonApiServerHealthy() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            
            HttpGet httpGet = new HttpGet(PYTHON_API_BASE_URL + HEALTH_ENDPOINT);
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    return "healthy".equals(jsonResponse.path("status").asText());
                }
            }
            
        } catch (Exception e) {
            // 연결 실패는 정상 (서버가 아직 시작되지 않았을 수 있음)
        }
        
        return false;
    }
    
    /**
     * 키워드 전처리 (노이즈 제거 및 정규화)
     */
    private static String preprocessKeyword(String keyword) {
        return keyword.toLowerCase()
                     .replaceAll("[^가-힣a-zA-Z0-9\\s]", " ") // 특수문자 제거
                     .replaceAll("\\s+", " ") // 연속 공백 정리
                     .trim();
    }
    
    /**
     * 상품명 전처리 (노이즈 제거 및 정규화)
     */
    private static String preprocessProductTitle(String title) {
        return title.toLowerCase()
                   .replaceAll("[^가-힣a-zA-Z0-9\\s]", " ") // 특수문자 제거
                   .replaceAll("\\s+", " ") // 연속 공백 정리
                   .trim();
    }
    
    /**
     * 다층 임계값으로 연관성 평가
     */
    private static boolean evaluateWithMultiThreshold(double similarity, String keyword, String title) {
        // 70% 이상: 확실한 연관성
        if (similarity >= HIGH_SIMILARITY_THRESHOLD) {
            return true;
        }
        
        // 50~70%: 추가 검증 수행
        if (similarity >= MEDIUM_SIMILARITY_THRESHOLD) {
            // 추가 검증: 키워드 길이가 짧으면 더 관대하게
            if (keyword.length() <= 3) {
                return similarity >= 0.5; // 짧은 키워드는 50% 이상
            }
            return true;
        }
        
        // 35~50%: 부분 매칭 추가 확인
        if (similarity >= LOW_SIMILARITY_THRESHOLD) {
            // 부분 매칭으로 보완
            return title.contains(keyword) || containsPartialMatch(keyword, title);
        }
        
        // 35% 미만: 연관성 없음
        return false;
    }
    
    /**
     * 신뢰도 레벨 반환
     */
    private static String getConfidenceLevel(double similarity) {
        if (similarity >= HIGH_SIMILARITY_THRESHOLD) {
            return "높은 신뢰도";
        } else if (similarity >= MEDIUM_SIMILARITY_THRESHOLD) {
            return "보통 신뢰도";
        } else if (similarity >= LOW_SIMILARITY_THRESHOLD) {
            return "낮은 신뢰도";
        } else {
            return "신뢰도 부족";
        }
    }
    
    /**
     * 부분 매칭 확인 (단어 단위)
     */
    private static boolean containsPartialMatch(String keyword, String title) {
        String[] keywordWords = keyword.split("\\s+");
        for (String word : keywordWords) {
            if (word.length() >= 2 && title.contains(word)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 구조 분해 분석 - 키워드를 모든 부분 문자열로 분해하여 검사 (길이별 성공 기준 적용)
     * 
     * @param keyword 원본 키워드
     * @param productTitle 원본 상품명
     * @return 키워드 길이별 기준에 따른 성공/실패
     */
    private static boolean performStructuralDecomposition(String keyword, String productTitle) {
        System.out.println("   🔧 키워드 구조 분해 중: " + keyword + " (" + keyword.length() + "글자)");
        
        // 키워드 조합 풀 생성 (1글자, 2글자, 3글자... 모든 부봄 문자열)
        java.util.Set<String> keywordPool = new java.util.HashSet<>();
        
        for (int length = 1; length <= keyword.length(); length++) {
            for (int start = 0; start <= keyword.length() - length; start++) {
                String substring = keyword.substring(start, start + length);
                if (substring.length() >= 1) { // 1글자 이상만 포함
                    keywordPool.add(substring);
                }
            }
        }
        
        System.out.println("   📦 생성된 키워드 조합 풀 (" + keywordPool.size() + "개): " + keywordPool);
        
        // 상품명에서 매칭되는 조합 개수 계산 (단어 단위로 쪼개서 직접 비교)
        int matchCount = 0;
        java.util.List<String> matchedParts = new java.util.ArrayList<>();
        
        // 상품명을 단어 단위로 분리 (공백, 콤마 등 비-문자/숫자 기준)
        String[] titleWords = productTitle.split("[^가-힣a-zA-Z0-9]+");
        java.util.List<String> wordList = java.util.Arrays.asList(titleWords);
        
        boolean hasHighFrequencyPart = false; // 3회 이상 나타나는 조각이 있는지 확인
        
        for (String part : keywordPool) {
            // 각 조각이 상품명에서 몇 번 나타나는지 계산
            long partCount = wordList.stream().filter(word -> word.equals(part)).count();
            
            // 3회 이상 나타나는 조각이 있으면 즉시 성공 플래그 설정
            if (partCount >= 3) {
                hasHighFrequencyPart = true;
                matchedParts.add(part + "(" + partCount + "회-고빈도)");
            }
            // 2번 이상 나타나는 조각만 매칭으로 인정
            else if (partCount >= 2) {
                matchCount++;
                matchedParts.add(part + "(" + partCount + "회)");
            }
        }
        
        System.out.println("   🎯 매칭된 조합 (" + matchCount + "개): " + matchedParts);
        
        // 키워드 길이별 성공 기준 적용
        int requiredMatches;
        String lengthCategory;
        
        if (keyword.length() == 1) {
            requiredMatches = 2; // 1글자: 2개 이상
            lengthCategory = "1글자";
        } else if (keyword.length() >= 2 && keyword.length() <= 3) {
            requiredMatches = 2; // 2-3글자: 2개 이상
            lengthCategory = "2-3글자";
        } else { // 4글자 이상
            requiredMatches = 2; // 4글자+: 2개 이상
            lengthCategory = "4글자+";
        }
        
        // 성공 조건: 일반 매칭이 기준을 만족하거나, 3회 이상 고빈도 조각이 있는 경우
        boolean isSuccess = matchCount >= requiredMatches || hasHighFrequencyPart;
        
        if (hasHighFrequencyPart) {
            System.out.println("   📋 매칭 기준 [" + lengthCategory + "]: 고빈도 조각 발견으로 즉시 성공");
        } else {
            System.out.println("   📋 매칭 기준 [" + lengthCategory + "]: " + requiredMatches + "개 이상 → " + (isSuccess ? "성공" : "실패"));
        }
        
        return isSuccess;
    }
}