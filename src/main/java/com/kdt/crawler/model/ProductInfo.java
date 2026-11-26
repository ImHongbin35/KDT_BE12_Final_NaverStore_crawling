package com.kdt.crawler.model;

public class ProductInfo {
    private String keyword;
    private String productName;
    private String price;
    private String productNameCn;
    private String description;
    private String rating;
    private String productUrl;
    private String crawlType;
    private String createdAt;

    public ProductInfo() {}

    // Getters and Setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getProductNameCn() { return productNameCn; }
    public void setProductNameCn(String productNameCn) { this.productNameCn = productNameCn; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String productUrl) { this.productUrl = productUrl; }

    public String getCrawlType() { return crawlType; }
    public void setCrawlType(String crawlType) { this.crawlType = crawlType; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    // 기존 네이밍 규칙 호환성을 위한 메서드 (crawledAt -> createdAt 매핑)
    public String getCrawledAt() { return createdAt; }
    public void setCrawledAt(String crawledAt) { this.createdAt = crawledAt; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 상품 정보 ===\n");
        sb.append("검색 키워드: ").append(keyword).append("\n");
        sb.append("상품명: ").append(productName).append("\n");
        sb.append("중국어 상품명: ").append(productNameCn).append("\n");
        sb.append("현재가격: ").append(price).append("\n");
        sb.append("상품설명: ").append(description).append("\n");
        sb.append("평점: ").append(rating).append("\n");
        sb.append("상품URL: ").append(productUrl).append("\n");
        sb.append("크롤링 타입: ").append(crawlType).append("\n");
        sb.append("수집일시: ").append(createdAt).append("\n");
        sb.append("==================\n");
        return sb.toString();
    }
}
