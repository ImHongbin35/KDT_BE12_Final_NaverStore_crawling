# 🚀 싸다구몰 자동 크롤링 프로젝트

![Java](https://img.shields.io/badge/Java-11+-007396?style=flat-square&logo=java)
![Maven](https://img.shields.io/badge/Maven-3.6+-C71A36?style=flat-square&logo=apache-maven)
![Playwright](https://img.shields.io/badge/Playwright-1.40.0-45ba4b?style=flat-square&logo=playwright)

## 📋 프로젝트 개요

**네이버 쇼핑 베스트 API**에서 실시간 인기 키워드(1~20위)를 수집하고, 그중 **중복 사용되지 않았으며 실제 상품과 연관성이 검증된 키워드**를 골라 싸다구몰에서 상품을 찾아 **상세 정보를 크롤링해 DB에 저장**하는 완전 자동화 프로젝트입니다.

> **⚠️ 주의**: 이 프로젝트는 교육 목적으로 제작되었습니다. 상업적 사용 시 해당 사이트의 이용약관을 준수해주세요.

이 저장소는 커밋 히스토리로 전체 개발 과정을 담고 있습니다: 구글 트렌드 기반 초기 버전 → 네이버 쇼핑 베스트로 전환 → 유사도 검증/번역 기능 추가 → API 기반 수집으로 고도화 → Oracle DB 전환(진행 중). 각 단계의 변경 내용은 [커밋 로그](https://github.com/ImHongbin35/KDT_BE12_Final_NaverStore_crawling/commits/main)에서 확인할 수 있습니다.

## 🎯 주요 기능

### ✅ 완전 자동화
네이버 쇼핑 베스트 키워드 수집 → 키워드 검증·선택 → 싸다구몰 검색 → 상품 클릭 → 상세 정보 수집 → DB 저장까지 사용자 개입 없이 전체 프로세스가 자동 실행됩니다.

### 🔍 네이버 쇼핑 베스트 API 기반 키워드 수집
- 페이지 스크래핑이 아닌 **HTTP + JSON API 호출**로 1~20위 베스트 키워드 목록을 조회
- 1위부터 순서대로 아래 3단계 검증을 거쳐 **첫 번째로 통과한 키워드**를 선택
  1. **기본 유효성 검증** (길이, 특수문자/HTML 태그, 네비게이션성 키워드 제외)
  2. **30일 중복 검사**: 최근 30일 내 이미 사용한 키워드는 건너뜀 (트렌드성 반영)
  3. **실제 상품 연관성 검증**: `SimilarityService`로 키워드와 검색된 상품명의 유사도를 검증, 실패 시 `PapagoTranslator`로 한/영 대체 키워드를 만들어 재시도

### 🛒 실제 상품 링크만 정확히 클릭
- `search_view.php` 완전 제외, `platform=` & `num_iid=` 파라미터 검증으로 실제 상품 상세 페이지만 타겟팅
- Playwright → JavaScript → 컨테이너 기반 순차 시도의 다단계 클릭 전략

### 📊 상품 상세 스펙 완전 추출 및 DB 저장
- `.pro-info-boxs` 구조 파싱으로 상표/모델/전력/크기/무게/원산지 등 전체 제품 사양 추출
- 크롤링 작업 유형(`CrawlJobType`: 자동/수동)과 함께 DB에 저장

## 🏗️ 프로젝트 구조

```
.
├── src/main/java/com/kdt/crawler/
│   ├── SsadaguCrawler.java              # 🎯 메인 자동화 크롤러
│   ├── PlaywrightInstaller.java         # 🔧 브라우저 설치 도구
│   ├── model/
│   │   ├── ProductInfo.java             # 📊 상품 정보 데이터 모델
│   │   ├── UsedKeyword.java             # 🔄 키워드 중복 체크용 모델
│   │   └── CrawlJobType.java            # 🏷️ 크롤링 작업 유형(자동/수동)
│   ├── service/
│   │   ├── NaverShoppingBestService.java  # 🔍 네이버 쇼핑 베스트 API 연동
│   │   ├── KeywordService.java           # 🔄 키워드 유효성·중복·연관성 검증
│   │   ├── DatabaseService.java          # 💾 상품 정보 DB 저장 (Oracle)
│   │   └── SsadaguCrawlerService.java    # 🛒 싸다구몰 상품 클릭·정보 추출
│   └── util/
│       ├── SimilarityService.java       # 🧠 키워드-상품명 유사도 검증
│       ├── PapagoTranslator.java        # 🌐 네이버 Papago 한/영 번역
│       ├── LanguageConverter.java       # 🔤 대체 언어 매핑
│       └── FileUtils.java               # 💾 로그 저장 유틸리티
├── python_similarity_api/               # 🐍 유사도 계산용 별도 Python 마이크로서비스
├── database_schema_oracle.sql           # 🗄️ Oracle DB 스키마
├── data/                                # 📁 로그 저장 폴더
├── pom.xml                              # ⚙️ Maven 설정
├── run.bat                              # 🚀 Windows 실행 스크립트
└── .env.example                         # 🔑 환경변수 템플릿
```

## 🚀 실행 방법

### 1단계: 환경 설정 (최초 1회만)
```bash
cp .env.example .env
# .env에 DB 접속 정보, PAPAGO_CLIENT_ID/SECRET 등을 채워주세요
# PlaywrightInstaller.java 실행하여 브라우저 설치
```

### 2단계: 메인 크롤러 실행
```bash
# SsadaguCrawler.java 실행 (인자 없으면 AUTOMATIC으로 동작)
```

### 3단계: 결과 확인
- 크롤링된 상품 정보와 키워드는 DB에 저장됩니다
- 실행 로그는 `data/` 폴더에서 확인 가능합니다

## 🎯 자동화 프로세스

```
🚀 시작
  ▼
📊 1단계: 네이버 쇼핑 베스트 API에서 1~20위 키워드 수집
  ▼
🔍 2단계: 1위부터 순차 검증 (기본 유효성 → 30일 중복 → 상품 연관성/번역 재시도)
  ▼
✅ 3단계: 검증 통과한 첫 키워드 확정
  ▼
🛒 4단계: 싸다구몰에서 해당 키워드로 검색, 실제 상품 클릭
  ▼
📋 5단계: 상품 상세 정보 추출 및 검증
  ▼
💾 6단계: 키워드·상품 정보를 DB에 저장
  ▼
🎉 완료!
```

## ⚠️ 알려진 이슈 / 진행 중인 작업

- **DB 마이그레이션 진행 중**: 최신 커밋에서 `DatabaseService`는 Oracle(`ORACLE_DB_*` 환경변수)로 전환됐지만, `KeywordService`는 아직 기존 MySQL 접속 정보(`DB_*`)를 그대로 사용하고 있어 전체 기능이 하나의 DB로 통일되지 않은 상태입니다. `pom.xml`에도 Oracle JDBC 드라이버 의존성이 아직 추가되지 않았습니다.
- 이 시점 이후 작업은 팀 프로젝트([Final-7team-BE](https://github.com/ImHongbin35/Final-7team-BE))로 이관되어 Spring Boot + MyBatis 기반으로 포팅되었습니다.

## 📞 프로젝트 정보

**Repository**: https://github.com/ImHongbin35/KDT_BE12_Final_NaverStore_crawling
**개발 기간**: 2025년 8월 ~ 9월
**기술 스택**: Java 11, Maven, Playwright, Jsoup, Jackson, Apache HttpClient, Python(유사도 API)
