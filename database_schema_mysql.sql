-- ================================================
-- MySQL DB 스키마 - 싸다구몰 크롤링 프로젝트
-- 작성일: 2025-09-12
-- 버전: v2.1/v2.2 (3-tier 관계형 테이블 구조)
-- ================================================

-- 1. 기존 테이블 삭제 (순서 중요: 자식 → 부모)
DROP TABLE IF EXISTS excluded_keyword;
DROP TABLE IF EXISTS used_keyword;
DROP TABLE IF EXISTS product;

-- 2.1. product 테이블 (부모 테이블)
CREATE TABLE product (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(255),
    product_name VARCHAR(500) NOT NULL,
    product_name_cn VARCHAR(500),
    price VARCHAR(50),
    description TEXT,
    rating VARCHAR(20),
    product_url VARCHAR(2048),
    image_url VARCHAR(2048),
    local_image_path VARCHAR(1024),
    ocr_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_product_url (product_url(255)),
    INDEX idx_product_keyword (keyword),
    INDEX idx_product_created_at (created_at)
);

-- 2.2. used_keyword 테이블 (product를 참조하는 자식 테이블)
CREATE TABLE used_keyword (
    keyword_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT, -- product 테이블을 참조할 FK 컬럼
    keyword VARCHAR(255) NOT NULL,
    source VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_used_keyword_keyword (keyword),
    INDEX idx_used_keyword_used_date (created_at),
    
    -- 외래 키(FK) 제약조건 설정
    -- product 테이블의 product_id를 참조합니다.
    FOREIGN KEY (product_id) REFERENCES product(product_id)
    -- 만약 부모(product) 데이터가 삭제되면, 관련된 자식(used_keyword) 데이터도 함께 삭제됩니다.
    ON DELETE CASCADE
);

-- 2.3. excluded_keyword 테이블 (used_keyword를 참조하는 자식 테이블)
CREATE TABLE excluded_keyword (
    excluded_keyword_id INT AUTO_INCREMENT PRIMARY KEY,
    keyword_id INT, -- used_keyword 테이블을 참조할 FK 컬럼
    excluded_keyword VARCHAR(255) NOT NULL,
    source VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_excluded_keyword (excluded_keyword),
    INDEX idx_created_at (created_at),

    -- 외래 키(FK) 제약조건 설정
    -- used_keyword 테이블의 keyword_id를 참조합니다.
    FOREIGN KEY (keyword_id) REFERENCES used_keyword(keyword_id)
    ON DELETE CASCADE
);

-- 3. 테이블 구조 확인
SHOW TABLES;
DESCRIBE product;
DESCRIBE used_keyword;
DESCRIBE excluded_keyword;