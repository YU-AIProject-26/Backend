## 📝 커밋 메시지 작성 규칙

- 작성 방법 : `[카테고리]` 작업 내용 요약

| 카테고리 | 설명 |
|--|--|
| 추가 | 새로운 기능 추가 |
| 수정 | 기존 기능 수정, 버그 수정 |
| 삭제 | 불필요한 내용 삭제, 기능 삭제 |
| 문서 | 코드 수정 없이 문서 관련 작업 시 |
| 테스트 | 테스트 코드 작업 시 |
| 환경 | 빌드, 설정 파일, DB 연결 등 환경 관련 작업 시 |

### 예시
- `[추가] 회의 생성 API 구현`
- `[수정] 회의 요약 모델 결과 개선`
- `[환경] AI 서버 연동 설정 추가`
- `[문서] README 작성`

---

## ⚙️ 개발 환경 정보 (Environment)

| 항목 | 버전 | 비고 |
|------|------|------|
| **Java** | 17 | 프로젝트 소스 컴파일 대상(Java 17 언어 수준) |
| **JDK** | 22.0.2 (Temurin) | 실제 로컬 실행 환경 |
| **Spring Boot** | 3.5.6 | API 서버 |
| **Gradle** | 8.14.3 | Wrapper 사용 |
| **MySQL** | 8.4.6 | 데이터베이스 |
| **Python** | 3.x | AI 서버 실행 환경 |
| **FastAPI** | 사용 | AI 모델 추론 서버 |
| **Transformers** | 사용 | 자연어 처리 모델 |
| **OpenAI API** | 사용 | 음성 인식 및 요약 |
| **Pyannote** | 사용 | 화자 분리 |

---

## 💻 실행 방법

### 1. 레포지토리 복사

```bash
git clone https://github.com/YU-AIProject-26/Backend.git
```

### 2. Spring Boot 서버 실행

```bash
# 1. Spring Boot 서버 디렉토리로 이동
cd spring-server

# 2. 프로젝트 의존성 다운로드 및 전체 빌드 수행
./gradlew clean build

# 3. Spring Boot 서버 실행
./gradlew bootRun
```

### 3. AI 서버 실행 (FastAPI)

```bash
# 1. AI 서버 디렉토리로 이동
cd ai-server

# 2. 가상환경 생성
python -m venv venv

# 3. 가상환경 활성화
# macOS / Linux 환경
source venv/bin/activate

# Windows 환경
venv\Scripts\activate

# 4. 필요한 패키지 설치
pip install -r requirements.txt

# 5. FastAPI 서버 실행
uvicorn main:app --reload
```

---

## 📂 디렉토리 구조

```bash
backend/
├── spring-server/      # Spring Boot 기반 API 서버 디렉토리
├── ai-server/          # Python 기반 AI 모델 처리 및 분석 디렉토리
├── docs/               # 프로젝트 관련 문서 저장 디렉토리
├── .gitignore          # Git 추적에서 제외할 파일 목록
├── .gitattributes      # 줄바꿈 및 파일 속성 관리 설정
├── .editorconfig       # 팀 공통 코드 스타일 및 편집기 설정
└── README.md           # 프로젝트 설명 및 실행 방법 문서
```