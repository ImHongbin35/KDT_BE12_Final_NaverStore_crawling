package com.kdt.crawler.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 한글 키워드를 영어로 또는 영어 키워드를 한글로 변환하는 유틸리티 클래스
 * 
 * 주요 기능:
 * - 파파고 API를 활용한 실시간 번역
 * - 번역 결과 캐싱으로 성능 최적화
 * - 하드코딩 없이 유연한 언어 변환
 * 
 * 변경사항:
 * - 기존: 하드코딩된 매핑 테이블 사용
 * - 현재: 파파고 API + 캐싱 시스템 사용
 */
public class LanguageConverter {
    
    static {
        System.out.println("✅ LanguageConverter 초기화 완료 (파파고 API 연동)");
        System.out.println("   기존 하드코딩 방식에서 실시간 번역 방식으로 전환");
    }
    
    /**
     * 한글 키워드를 영어로 변환 (파파고 API 사용)
     * 
     * @param koreanKeyword 한글 키워드
     * @return 영어 키워드, 실패 시 null
     */
    public static String koreanToEnglish(String koreanKeyword) {
        if (koreanKeyword == null || koreanKeyword.trim().isEmpty()) {
            return null;
        }
        
        System.out.println("🌐 한국어 → 영어 번역 시도: " + koreanKeyword);
        return PapagoTranslator.koreanToEnglish(koreanKeyword.trim());
    }
    
    /**
     * 영어 키워드를 한글로 변환 (파파고 API 사용)
     * 
     * @param englishKeyword 영어 키워드
     * @return 한글 키워드, 실패 시 null
     */
    public static String englishToKorean(String englishKeyword) {
        if (englishKeyword == null || englishKeyword.trim().isEmpty()) {
            return null;
        }
        
        System.out.println("🌐 영어 → 한국어 번역 시도: " + englishKeyword);
        return PapagoTranslator.englishToKorean(englishKeyword.trim());
    }
    
    /**
     * 키워드의 대체 언어 버전을 반환 (한글이면 영어로, 영어면 한글로)
     * 파파고 API의 자동 언어 감지 기능 활용
     * 
     * @param keyword 원본 키워드
     * @return 대체 언어 키워드, 실패 시 null
     */
    public static String getAlternativeLanguage(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        
        System.out.println("🔄 대체 언어 번역 시도: " + keyword);
        return PapagoTranslator.autoTranslate(keyword.trim());
    }
    
    /**
     * 키워드에 대한 번역 지원 여부 확인
     * (파파고 API는 대부분의 키워드를 지원하므로 항상 true 반환)
     * 
     * @param keyword 확인할 키워드
     * @return 번역 지원 여부 (거의 항상 true)
     */
    public static boolean hasMappingFor(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        // 파파고 API는 대부분의 한국어/영어 텍스트를 번역할 수 있음
        String text = keyword.trim();
        return containsKorean(text) || isEnglishOnly(text);
    }
    
    /**
     * 현재 캐시된 번역 수 반환
     * 
     * @return 캐시된 번역 결과 수
     */
    public static int getCacheSize() {
        return PapagoTranslator.getCacheSize();
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
     * 키워드 변환 결과를 포맷된 문자열로 반환
     * 
     * @param originalKeyword 원본 키워드
     * @param convertedKeyword 변환된 키워드
     * @return 포맷된 결과 문자열
     */
    public static String formatConversionResult(String originalKeyword, String convertedKeyword) {
        if (convertedKeyword == null) {
            return String.format("'%s' → 번역 실패 (파파고 API 오류)", originalKeyword);
        } else {
            return String.format("'%s' → '%s' (파파고)", originalKeyword, convertedKeyword);
        }
    }
}