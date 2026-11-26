# Python Sentence-Transformers API Server

## 📋 개요
이 Python API 서버는 Java 크롤러 프로젝트와 함께 동작하여 키워드와 상품명 간의 의미적 유사도를 계산합니다.

Komoran 형태소 분석의 한계를 극복하기 위해 AI 기반 의미 분석을 제공합니다.

## 🚀 실행 방법

### 1단계: Python 환경 설정
```bash
# Python 가상환경 생성 (권장)
python -m venv venv

# 가상환경 활성화
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate
```

### 2단계: 의존성 설치
```bash
pip install -r requirements.txt
```

### 3단계: API 서버 실행
```bash
python app.py
```

**성공 메시지:**
```
Loading Sentence-Transformer model...
Model loaded successfully.
Starting Sentence-Transformers API server on port 5000...
 * Running on http://127.0.0.1:5000
```

## 🔗 API 사용법

### 상태 확인
```bash
curl http://localhost:5000/health
```

### 유사도 계산
```bash
curl -X POST http://localhost:5000/calculate-similarity \
  -H "Content-Type: application/json" \
  -d '{"text1": "애플워치", "text2": "Apple Watch 시리즈 8"}'
```

**응답 예시:**
```json
{"similarity": 0.8745}
```

## ⚠️ 주의사항

1. **메모리 사용량**: 초기 모델 로딩 시 약 1-2GB RAM 사용
2. **로딩 시간**: 첫 실행 시 모델 다운로드로 인해 시간 소요 가능
3. **포트**: 5000번 포트가 사용중이면 app.py에서 포트 번호 변경 필요

## 🔧 Java 연동

Java 프로젝트의 `SimilarityService.java`가 이 API 서버와 자동 통신합니다.

**실행 순서:**
1. 이 Python API 서버 실행 (python app.py)
2. Java 크롤러 프로젝트 실행

API 서버가 실행되지 않으면 Java에서 백업 로직(부분 문자열 매칭)을 사용합니다.