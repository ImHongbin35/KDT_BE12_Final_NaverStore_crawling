package com.kdt.crawler.model;

/**
 * 키워드와 순위를 함께 저장하는 데이터 클래스
 */
public class KeywordRankPair {
    public final String keyword;
    public final int rank;
    
    public KeywordRankPair(String keyword, int rank) {
        this.keyword = keyword;
        this.rank = rank;
    }
    
    @Override
    public String toString() {
        return rank + "위: " + keyword;
    }
}