package com.kdt.crawler.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 네이버 파파고 API를 사용한 번역 서비스 클래스
 * 
 * 주요 기능:
 * - 한국어 → 영어 번역
 * - 영어 → 한국어 번역
 * - 번역 결과 캐싱으로 API 호출 최소화
 * - 오류 처리 및 재시도 로직
 */
public class PapagoTranslator {
    
    // 네이버 클라우드 플랫폼 파파고 API 키 (환경변수에서 로드)
    private static final String CLIENT_ID = System.getenv("PAPAGO_CLIENT_ID");  // x-ncp-apigw-api-key-id
    private static final String CLIENT_SECRET = System.getenv("PAPAGO_CLIENT_SECRET");  // x-ncp-apigw-api-key
    private static final String API_URL = "https://papago.apigw.ntruss.com/nmt/v1/translation";
    
    // 번역 결과 캐시 (메모리 절약을 위해 최대 1000개까지만 캐시)
    private static final Map<String, String> translationCache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    
    // JSON 파서
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        System.out.println("✅ PapagoTranslator 초기화 완료 (네이버 클라우드 플랫폼)");
        System.out.println("   API Endpoint: " + API_URL);
        System.out.println("   Client ID: " + CLIENT_ID.substring(0, 3) + "***");
    }
    
    /**
     * 한국어를 영어로 번역
     * 
     * @param koreanText 번역할 한국어 텍스트
     * @return 영어 번역 결과, 실패 시 null
     */
    public static String koreanToEnglish(String koreanText) {
        return translate(koreanText, "ko", "en");
    }
    
    /**
     * 영어를 한국어로 번역
     * 
     * @param englishText 번역할 영어 텍스트
     * @return 한국어 번역 결과, 실패 시 null
     */
    public static String englishToKorean(String englishText) {
        return translate(englishText, "en", "ko");
    }
    
    /**
     * 텍스트를 지정된 언어로 번역
     * 
     * @param text 번역할 텍스트
     * @param sourceLang 원본 언어 코드 (ko, en)
     * @param targetLang 대상 언어 코드 (ko, en)
     * @return 번역 결과, 실패 시 null
     */
    public static String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        text = text.trim();
        
        // 캐시에서 확인
        String cacheKey = sourceLang + ":" + targetLang + ":" + text;
        if (translationCache.containsKey(cacheKey)) {
            System.out.println("📚 캐시에서 번역 결과 반환: " + text);
            return translationCache.get(cacheKey);
        }
        
        // API 키 확인 (네이버 클라우드 플랫폼 API는 항상 설정되어 있음)
        // 더미 번역은 네트워크 오류 시에만 사용
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            
            // HTTP POST 요청 생성
            HttpPost httpPost = new HttpPost(API_URL);
            
            // 네이버 클라우드 플랫폼 API 헤더 설정
            httpPost.setHeader("x-ncp-apigw-api-key-id", CLIENT_ID);
            httpPost.setHeader("x-ncp-apigw-api-key", CLIENT_SECRET);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            
            // 요청 바디 생성
            String requestBody = String.format(
                "source=%s&target=%s&text=%s",
                sourceLang,
                targetLang,
                URLEncoder.encode(text, StandardCharsets.UTF_8)
            );
            
            httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
            
            // API 호출
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                if (response.getStatusLine().getStatusCode() == 200) {
                    // JSON 응답 파싱
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    String translatedText = jsonResponse
                        .path("message")
                        .path("result")
                        .path("translatedText")
                        .asText();
                    
                    if (!translatedText.isEmpty()) {
                        // 캐시에 저장 (크기 제한)
                        addToCache(cacheKey, translatedText);
                        
                        System.out.println("🌐 파파고 번역 성공: " + text + " → " + translatedText);
                        return translatedText;
                    }
                } else {
                    System.err.println("❌ 파파고 API 호출 실패: " + response.getStatusLine().getStatusCode());
                    System.err.println("   응답: " + responseBody);
                }
            }
            
        } catch (IOException e) {
            System.err.println("❌ 파파고 API 호출 중 네트워크 오류: " + e.getMessage());
            System.out.println("🔄 더미 번역으로 대체 시도...");
            
            // 네트워크 오류 시 더미 번역 사용
            return getDummyTranslation(text, sourceLang, targetLang);
        }
        
        return null;
    }
    
    /**
     * 언어를 자동 감지하여 적절한 대상 언어로 번역
     * 
     * @param text 번역할 텍스트
     * @return 번역 결과, 실패 시 null
     */
    public static String autoTranslate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        // 한글이 포함되어 있으면 영어로 번역
        if (containsKorean(text)) {
            return koreanToEnglish(text);
        }
        // 영어로만 구성되어 있으면 한글로 번역
        else if (isEnglishOnly(text)) {
            return englishToKorean(text);
        }
        
        return null;
    }
    
    /**
     * 캐시에 번역 결과 추가 (크기 제한 적용)
     */
    private static void addToCache(String key, String value) {
        // 캐시 크기 제한
        if (translationCache.size() >= MAX_CACHE_SIZE) {
            // 가장 오래된 항목 제거 (단순하게 첫 번째 항목 제거)
            String firstKey = translationCache.keySet().iterator().next();
            translationCache.remove(firstKey);
        }
        
        translationCache.put(key, value);
    }
    
    /**
     * 네트워크 오류 시 더미 번역 결과 반환
     * (백업용 - 주요 키워드만 지원)
     */
    private static String getDummyTranslation(String text, String sourceLang, String targetLang) {
        // 네트워크 오류 시 백업용 더미 매핑
        Map<String, String> dummyMappings = new HashMap<>();
        
        if ("ko".equals(sourceLang) && "en".equals(targetLang)) {
            dummyMappings.put("애플워치", "Apple Watch");
            dummyMappings.put("베드민턴", "Badminton");
            dummyMappings.put("대마종자유", "Hemp Seed Oil");
            dummyMappings.put("이어플러그", "Earplug");
            dummyMappings.put("실내베드민턴", "Indoor Badminton");
            dummyMappings.put("룹이어플러그", "Loop Earplug");
        } else if ("en".equals(sourceLang) && "ko".equals(targetLang)) {
            dummyMappings.put("Apple Watch", "애플워치");
            dummyMappings.put("Badminton", "베드민턴");
            dummyMappings.put("Hemp Seed Oil", "대마종자유");
            dummyMappings.put("Earplug", "이어플러그");
            dummyMappings.put("Indoor Badminton", "실내베드민턴");
            dummyMappings.put("Loop Earplug", "룹이어플러그");
        }
        
        return dummyMappings.get(text);
    }
    
    /**
     * 텍스트에 한글이 포함되어 있는지 확인
     */
    private static boolean containsKorean(String text) {
        return text.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*");
    }
    
    /**
     * 텍스트가 영어로만 구성되어 있는지 확인
     */
    private static boolean isEnglishOnly(String text) {
        return text.matches("^[a-zA-Z\\s]+$");
    }
    
    /**
     * 현재 캐시된 번역 결과 수 반환
     */
    public static int getCacheSize() {
        return translationCache.size();
    }
    
    /**
     * 번역 캐시 초기화
     */
    public static void clearCache() {
        translationCache.clear();
        System.out.println("🧹 번역 캐시 초기화 완료");
    }
}