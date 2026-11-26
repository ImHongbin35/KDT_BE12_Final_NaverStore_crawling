package com.kdt.crawler.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.*;

public class GoogleTrendsService {

    public List<String> getTrendingKeywords(Page page, int limit) {
        List<String> keywords = new ArrayList<>();
        
        try {
            System.out.println("🌐 구글 트렌드 페이지에 접속 중...");
            
            // 더 안정적인 URL 사용 (실시간 검색어)
            page.navigate("https://trends.google.co.kr/trending?geo=KR&hl=ko");
            
            // 페이지 로딩 대기
            System.out.println("⏳ 페이지 로딩 대기 중...");
            page.waitForLoadState(LoadState.LOAD);
            page.waitForTimeout(5000); // 5초 대기
            
            System.out.println("✅ 구글 트렌드 페이지 로딩 완료: " + page.url());
            
            // 간단한 JavaScript로 키워드 추출 시도
            System.out.println("🔍 트렌드 키워드 추출 중...");
            
            // .mZ3RIc 클래스에서만 키워드 추출
            Object result = page.evaluate("() => {" +
                "const keywords = [];" +
                "const trendElements = document.querySelectorAll('.mZ3RIc');" +
                "console.log('mZ3RIc 요소 개수:', trendElements.length);" +
                "for (let i = 0; i < trendElements.length; i++) {" +
                "  const el = trendElements[i];" +
                "  const text = el.textContent ? el.textContent.trim() : '';" +
                "  console.log('발견된 텍스트:', text);" +
                "  if (text.length >= 2 && text.length <= 20 && keywords.indexOf(text) === -1) {" +
                "    keywords.push(text);" +
                "  }" +
                "}" +
                "return keywords;" +
            "}");
            
            if (result instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> extractedKeywords = (List<String>) result;
                
                System.out.println("🔍 .mZ3RIc에서 추출된 키워드들:");
                for (String keyword : extractedKeywords) {
                    System.out.println("  - " + keyword);
                    if (isValidTrendKeyword(keyword) && !keywords.contains(keyword)) {
                        keywords.add(keyword);
                        System.out.println("✅ 유효한 상품 키워드: " + keyword);
                        if (keywords.size() >= limit) break;
                    } else {
                        System.out.println("❌ 상품과 무관한 키워드: " + keyword);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 구글 트렌드 접속 실패: " + e.getMessage());
        }
        
        // 키워드를 찾지 못한 경우 또는 충분하지 않은 경우 기본 키워드 추가
        if (keywords.size() < 3) {
            System.out.println("❌ 충분한 트렌드 키워드를 찾지 못했습니다. 기본 키워드를 사용합니다.");
            List<String> defaultKeywords = Arrays.asList(
                "에어컨", "선풍기", "휴대폰", "아이폰", "삼성갤럭시", 
                "에어팟", "맥북", "아이패드", "갤럭시버즈", "노트북", 
                "스마트워치", "무선이어폰", "케이스", "충전기", "마우스",
                "냉장고", "세탁기", "TV", "모니터", "키보드"
            );
            
            // 기존 키워드와 중복되지 않는 기본 키워드만 추가
            for (String defaultKeyword : defaultKeywords) {
                if (!keywords.contains(defaultKeyword)) {
                    keywords.add(defaultKeyword);
                    if (keywords.size() >= limit) break;
                }
            }
            System.out.println("📝 기본 키워드 " + keywords.size() + "개 설정 완료");
        } else {
            System.out.println("🎉 총 " + keywords.size() + "개의 키워드를 수집했습니다!");
        }
        
        return keywords.subList(0, Math.min(keywords.size(), limit));
    }
    
    private boolean isValidTrendKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        keyword = keyword.trim();
        
        // 길이 체크 (2-15자)
        if (keyword.length() < 2 || keyword.length() > 15) {
            return false;
        }
        
        // 숫자만 있는 키워드 필터링
        if (keyword.matches("^[0-9]+$")) {
            return false;
        }
        
        // 특수문자나 함수 호출 패턴 필터링
        if (keyword.contains("(") || keyword.contains(")") || keyword.contains("{") || keyword.contains("}") ||
            keyword.contains("[") || keyword.contains("]") || keyword.contains(";") || keyword.contains("&") ||
            keyword.contains("=") || keyword.contains("?") || keyword.contains("#") || keyword.contains("%") ||
            keyword.contains("@") || keyword.contains("*") || keyword.contains("+") || keyword.contains("<") ||
            keyword.contains(">") || keyword.contains("|") || keyword.contains("\\") || keyword.contains("/")) {
            return false;
        }
        
        // JavaScript/CSS/HTML 관련 키워드 필터링
        String[] invalidKeywords = {
            "더보기", "검색", "트렌드", "뉴스", "이미지", "동영상", 
            "쇼핑", "지도", "더", "모두", "웹", "전체", "기타", "보기",
            "Google", "Trends", "trending", "hours", "geo", "Korea",
            "한국", "검색어", "실시간", "인기", "순위", "위", "아래",
            "시간", "분", "초", "일", "월", "년", "오늘", "어제",
            "내일", "이번", "다음", "지난", "최근", "새로운", "loading",
            "로딩", "페이지", "사이트", "홈", "메인", "function",
            "onCssLoad", "onLoad", "onClick", "onChange", "style",
            "class", "div", "span", "img", "href", "src", "alt",
            "title", "id", "name", "value", "type", "submit",
            "button", "input", "form", "html", "css", "js",
            "javascript", "jquery", "ajax", "json", "xml"
        };
        
        for (String invalid : invalidKeywords) {
            if (keyword.toLowerCase().contains(invalid.toLowerCase())) {
                return false;
            }
        }
        
        // 상품과 관련된 키워드만 허용 (포지티브 필터링)
        String[] productKeywords = {
            // 전자제품
            "에어컨", "선풍기", "휴대폰", "아이폰", "갤럭시", "삼성", "LG", "애플",
            "노트북", "컴퓨터", "모니터", "키보드", "마우스", "헤드셋", "이어폰",
            "TV", "냉장고", "세탁기", "청소기", "전자레인지", "오븐", "믹서기",
            
            // 뷰티/화장품
            "화장품", "향수", "크림", "로션", "마스크", "샴푸", "린스", "립스틱",
            
            // 패션/액세서리
            "의류", "신발", "가방", "지갑", "시계", "반지", "목걸이",
            
            // 문구/도서
            "책", "문구", "펜", "노트", "다이어리", "스티커", "테이프",
            
            // 음식/음료 (상품으로 판매되는)
            "과자", "음료", "커피", "차", "라면", "과일", "월병", "케이크", "빵",
            "초콜릿", "사탕", "젤리", "쿠키", "비스킷", "떡", "한과", "견과류",
            
            // 자동차/교통
            "자동차", "자전거", "헬멧", "타이어", "오일", "배터리",
            
            // 가구/인테리어
            "침대", "의자", "책상", "소파", "테이블", "조명", "커튼",
            
            // 스포츠/운동
            "운동", "헬스", "요가", "축구", "농구", "테니스", "골프",
            
            // 게임/전자기기
            "게임", "콘솔", "PC방", "VR", "AR", "드론", "로봇",
            "카메라", "렌즈", "삼각대", "메모리", "하드디스크", "USB"
        };
        
        // 키워드가 상품 관련 키워드와 유사한지 확인
        String lowerKeyword = keyword.toLowerCase();
        for (String productKeyword : productKeywords) {
            if (lowerKeyword.contains(productKeyword.toLowerCase()) || 
                productKeyword.toLowerCase().contains(lowerKeyword)) {
                return true;
            }
        }
        
        // 일반적인 상품으로 판매될 수 있는 단어들 (더 넓은 범위)
        String[] generalProductTerms = {
            "월병", "케이크", "빵", "과자", "음료", "차", "커피", "라면", "떡",
            "의류", "신발", "가방", "시계", "반지", "목걸이", "팔찌",
            "화장품", "크림", "로션", "샴푸", "향수", "마스크",
            "책", "펜", "노트", "문구", "스티커",
            "장난감", "인형", "피규어", "게임", "퍼즐",
            "도구", "공구", "드라이버", "망치", "칼",
            "그릇", "컵", "접시", "숟가락", "젓가락",
            "타월", "수건", "베개", "이불", "매트리스"
        };
        
        // 일반 상품 용어와 정확히 매칭되는지 확인
        for (String term : generalProductTerms) {
            if (keyword.equals(term) || keyword.contains(term) || term.contains(keyword)) {
                return true;
            }
        }
        
        // 한글로만 구성된 2-6자 단어 (상품명일 가능성)
        if (keyword.matches("^[가-힣]{2,6}$")) {
            return true;
        }
        
        return false;
    }
    
    // 30일 중복 방지 로직을 적용한 키워드 선택 (트렌드성 반영)
    public String selectKeywordWithDuplicateCheck(List<String> keywords, com.kdt.crawler.service.KeywordService keywordService) {
        if (keywords.isEmpty()) {
            System.out.println("⚠️ 키워드 리스트가 비어있습니다. 기본 키워드를 사용합니다.");
            return "에어컨"; // 기본값
        }
        
        System.out.println("\n=== 발견된 구글 트렌드 키워드 ===");
        for (int i = 0; i < Math.min(keywords.size(), 10); i++) {
            System.out.println((i + 1) + ". " + keywords.get(i));
        }
        
        System.out.println("\n🔍 최근 30일간 중복되지 않은 키워드를 순서대로 검색 중...");
        
        // 인기순으로 하나씩 30일 중복 체크
        for (String keyword : keywords) {
            boolean isUsedRecently = keywordService.isKeywordUsedRecently(keyword);
            
            if (!isUsedRecently) {
                System.out.println("✅ 선택된 키워드: " + keyword + " (최근 30일 내 중복 없음)");
                return keyword;
            } else {
                System.out.println("❌ 키워드 '" + keyword + "'는 최근 30일 내 사용됨 - 다음 키워드 확인");
            }
        }
        
        System.out.println("⚠️ 모든 키워드가 최근 30일 내에 사용되었습니다. 첫 번째 키워드를 사용합니다.");
        return keywords.get(0);
    }
    
    // 기존 기능 유지 (호환성을 위해)
    public String selectFirstKeyword(List<String> keywords) {
        if (keywords.isEmpty()) {
            return "에어컨"; // 기본값
        }
        
        System.out.println("\n=== 발견된 구글 트렌드 키워드 ===");
        for (int i = 0; i < Math.min(keywords.size(), 10); i++) {
            System.out.println((i + 1) + ". " + keywords.get(i));
        }
        
        String selectedKeyword = keywords.get(0);
        System.out.println("\n✅ 자동 선택된 키워드: " + selectedKeyword);
        
        return selectedKeyword;
    }
}
