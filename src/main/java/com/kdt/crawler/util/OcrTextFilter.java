package com.kdt.crawler.util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * OCR로 추출된 텍스트를 정제하는 필터링 유틸리티 클래스
 * 
 * 주요 기능:
 * - 한글이 포함된 라인은 무조건 유지
 * - 의미 있는 영어 단어/문장 유지
 * - 무의미한 OCR 노이즈 제거
 */
public class OcrTextFilter {
    
    // 한글 패턴 (자음, 모음, 완성된 한글 모두 포함)
    private static final Pattern KOREAN_PATTERN = Pattern.compile("[ㄱ-ㅎㅏ-ㅣ가-힣]");
    
    // 의미 있는 영어 단어 화이트리스트
    private static final Set<String> ENGLISH_WHITELIST = new HashSet<>(Arrays.asList(
        // 쇼핑/마케팅 관련 키워드
        "SALE", "BEST", "NEW", "HOT", "DEAL", "OFF", "PRICE", "FREE", "GIFT",
        "SPECIAL", "LIMITED", "PREMIUM", "QUALITY", "BRAND", "ORIGINAL",
        
        // 사이즈/옵션 관련
        "S", "M", "L", "XL", "XXL", "SIZE", "COLOR", "STYLE",
        
        // 일반적인 상품 관련 단어
        "PRODUCT", "ITEM", "GOODS", "SHOP", "STORE", "MALL", "ONLINE",
        "SHIPPING", "DELIVERY", "ORDER", "BUY", "PURCHASE",
        
        // 브랜드명이나 일반적인 단어들
        "NIKE", "ADIDAS", "APPLE", "SAMSUNG", "LG", "SONY",
        "FASHION", "BEAUTY", "HOME", "KITCHEN", "ELECTRONICS",
        
        // 숫자와 함께 자주 나오는 단어들
        "DAYS", "HOURS", "MINUTES", "PIECES", "SET", "PACK"
    ));
    
    // 무의미한 문자 패턴 (OCR 오류로 자주 나오는 패턴들)
    private static final Pattern NOISE_PATTERN = Pattern.compile(
        "^[^a-zA-Z가-힣]*$|" +  // 알파벳이나 한글이 전혀 없는 라인
        "^[a-z]{1,2}$|" +       // 1-2글자 소문자만
        ".*[\\[\\]{}()<>]+.*|"  // 특수 괄호가 포함된 라인
    );
    
    /**
     * OCR 결과 텍스트를 정제하여 의미 있는 텍스트만 반환
     * 
     * @param ocrText OCR로 추출된 원본 텍스트
     * @return 정제된 텍스트 (개행으로 구분)
     */
    public static String filterOcrText(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return "";
        }
        
        List<String> validLines = new ArrayList<>();
        String[] lines = ocrText.split("\\r?\\n");
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 빈 라인 건너뛰기
            if (trimmedLine.isEmpty()) {
                continue;
            }
            
            // 정제 규칙 적용
            if (isValidLine(trimmedLine)) {
                validLines.add(trimmedLine);
            }
        }
        
        return String.join("\n", validLines);
    }
    
    /**
     * 라인의 유효성을 검사하는 메서드
     * 
     * @param line 검사할 라인
     * @return 유효한 라인인지 여부
     */
    private static boolean isValidLine(String line) {
        // 규칙 1: 한글이 포함된 라인은 무조건 유지
        if (containsKorean(line)) {
            return true;
        }
        
        // 규칙 2: 의미 있는 영어 단어/문장 검사
        if (isValidEnglishContent(line)) {
            return true;
        }
        
        // 규칙 3: 무의미한 노이즈 제거
        return false;
    }
    
    /**
     * 한글이 포함되어 있는지 확인
     * 
     * @param text 검사할 텍스트
     * @return 한글 포함 여부
     */
    private static boolean containsKorean(String text) {
        return KOREAN_PATTERN.matcher(text).find();
    }
    
    /**
     * 의미 있는 영어 콘텐츠인지 검사
     * 
     * @param line 검사할 라인
     * @return 의미 있는 영어 콘텐츠 여부
     */
    private static boolean isValidEnglishContent(String line) {
        // 노이즈 패턴에 매칭되면 무효
        if (NOISE_PATTERN.matcher(line).matches()) {
            return false;
        }
        
        // 화이트리스트에 있는 단어가 포함되어 있는지 확인
        String upperLine = line.toUpperCase();
        for (String whitelistWord : ENGLISH_WHITELIST) {
            if (upperLine.contains(whitelistWord)) {
                return true;
            }
        }
        
        // 2개 이상의 영어 단어로 구성된 문장인지 확인 (공백 기준)
        String[] words = line.split("\\s+");
        if (words.length >= 2) {
            // 모든 단어가 영어 알파벳으로만 구성되어 있는지 확인
            int englishWordCount = 0;
            for (String word : words) {
                if (word.matches("[a-zA-Z]+")) {
                    englishWordCount++;
                }
            }
            
            // 최소 2개의 영어 단어가 있으면 문장으로 판단
            if (englishWordCount >= 2) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * OCR 텍스트를 한 줄로 합치는 유틸리티 메서드
     * 
     * @param ocrText 정제된 OCR 텍스트
     * @return 한 줄로 합쳐진 텍스트
     */
    public static String joinToSingleLine(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return "";
        }
        
        return ocrText.replace("\n", " ").replaceAll("\\s+", " ").trim();
    }
    
    /**
     * OCR 텍스트의 통계 정보를 반환하는 유틸리티 메서드
     * 
     * @param originalText 원본 OCR 텍스트
     * @param filteredText 정제된 OCR 텍스트
     * @return 통계 정보 문자열
     */
    public static String getFilteringStats(String originalText, String filteredText) {
        if (originalText == null) originalText = "";
        if (filteredText == null) filteredText = "";
        
        int originalLines = originalText.split("\\r?\\n").length;
        int filteredLines = filteredText.split("\\r?\\n").length;
        
        // 빈 텍스트인 경우 보정
        if (originalText.trim().isEmpty()) originalLines = 0;
        if (filteredText.trim().isEmpty()) filteredLines = 0;
        
        return String.format("OCR 텍스트 정제 결과: %d줄 → %d줄 (%.1f%% 유지)", 
                           originalLines, 
                           filteredLines, 
                           originalLines > 0 ? (filteredLines * 100.0 / originalLines) : 0.0);
    }
}