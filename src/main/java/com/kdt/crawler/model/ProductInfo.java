package com.kdt.crawler.model;

public class ProductInfo {
    private String keyword;
    private String productName;
    private String price;
    private String description;
    private String rating;
    private String productUrl;
    private String imageUrl;
    private String localImagePath;
    private String productNameCn;
    private String ocrText; // OCR로 추출된 이미지 텍스트
    private String createdAt;

    public ProductInfo() {}

    // Getters and Setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String productUrl) { this.productUrl = productUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLocalImagePath() { return localImagePath; }
    public void setLocalImagePath(String localImagePath) { this.localImagePath = localImagePath; }

    public String getProductNameCn() { return productNameCn; }
    public void setProductNameCn(String productNameCn) { this.productNameCn = productNameCn; }

    public String getOcrText() { return ocrText; }
    public void setOcrText(String ocrText) { this.ocrText = ocrText; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 상품 정보 ===\n");
        sb.append("검색 키워드: ").append(keyword).append("\n");
        sb.append("상품명: ").append(productName).append("\n");
        sb.append("상품명(중문): ").append(productNameCn).append("\n");
        sb.append("현재가격: ").append(price).append("\n");
        sb.append("상품설명: ").append(description).append("\n");
        sb.append("평점: ").append(rating).append("\n");
        sb.append("상품URL: ").append(productUrl).append("\n");
        sb.append("이미지URL: ").append(imageUrl).append("\n");
        sb.append("저장된 이미지 경로: ").append(localImagePath).append("\n");
        sb.append("OCR 추출 텍스트: ").append(ocrText != null ? ocrText.replace("\n", " | ") : "없음").append("\n");
        sb.append("생성일시: ").append(createdAt).append("\n");
        sb.append("==================\n");
        return sb.toString();
    }
}
