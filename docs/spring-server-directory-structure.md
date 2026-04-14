# Spring Server 디렉토리 구조 가이드

본 문서는 `spring-server` 프로젝트의 디렉토리 구조와 각 폴더의 역할을 정리한 문서이다.  
팀원들이 기능 구현 시 일관된 구조를 유지할 수 있도록 하기 위해 작성한다.

---

## 1. 전체 디렉토리 구조

```bash
spring-server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── acta/
│   │   │           └── spring-server/
│   │   │               ├── domain/
│   │   │               ├── global/
│   │   │               ├── common/
│   │   │               └── SpringServerApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       └── static/
│   └── test/
└── build.gradle
```

---

## 2. 최상위 패키지 역할

### 2.1 domain
`domain`은 실제 서비스의 기능 단위를 기준으로 나누는 패키지이다.  
예를 들어 사용자, 회의, 투두, 일정, 회의 분석 등의 핵심 기능은 모두 `domain` 아래에서 관리한다.

예시:
```bash
domain/
├── user/
├── meeting/
├── todo/
├── schedule/
├── analysis/
└── external/
```

즉, **프로젝트의 핵심 비즈니스 기능은 domain 아래에 위치한다.**

---

### 2.2 global
`global`은 프로젝트 전체에서 공통으로 적용되는 설정이나 전역 처리 코드를 모아두는 패키지이다.  
특정 기능 하나에만 속하지 않고, 여러 도메인에서 공통으로 사용되는 전역 요소를 관리한다.

예시:
```bash
global/
├── config/
├── exception/
├── response/
├── security/
└── util/
```

즉, **전역 설정, 보안, 예외 처리, 공통 응답 형식 등은 global 아래에 위치한다.**

---

### 2.3 common
`common`은 전역 설정보다는 가볍지만 여러 도메인에서 재사용할 수 있는 공통 요소를 두는 패키지이다.  
다만 `global`과 역할이 겹치지 않도록 주의해야 한다.

예시:
```bash
common/
├── constant/
├── enums/
└── helper/
```

즉, **상수, enum, 단순 공통 helper 성격의 코드는 common 아래에 위치한다.**

> 참고  
> `common`과 `global`의 역할이 겹치지 않도록 주의해야 한다.  
> 설정, 예외 처리, 보안, 응답 형식처럼 전역 처리 성격이 강한 코드는 `global`에 두고,  
> 여러 도메인에서 공통으로 쓰는 단순 재사용 요소는 `common`에 두는 것을 원칙으로 한다.

---

## 3. domain 하위 디렉토리 구조

각 도메인은 동일한 구조를 유지하는 것을 원칙으로 한다.  
예를 들어 `User` 도메인을 생성할 경우 아래와 같은 구조를 사용한다.

```bash
domain/
└── user/
    ├── controller/
    ├── service/
    ├── dto/
    ├── entity/
    └── repository/
```

필요에 따라 아래 패키지를 추가할 수 있다.

```bash
domain/
└── user/
    ├── controller/
    ├── service/
    ├── dto/
    ├── entity/
    ├── repository/
    ├── mapper/
    └── enums/
```

---

## 4. 도메인 하위 패키지 역할

도메인 하위 패키지는 일반적으로 아래 순서로 설계하고 구현하는 것을 권장한다.

- `entity`
- `repository`
- `dto`
- `service`
- `controller`

즉, **데이터 구조를 먼저 정의하고, DB 접근 계층을 만든 뒤, 요청/응답 객체와 비즈니스 로직, 마지막으로 API 진입점을 구현하는 순서**로 개발하는 것을 기본 흐름으로 한다.

---

### 4.1 entity
데이터베이스 테이블과 매핑되는 JPA 엔티티를 두는 패키지이다.  
실제 DB에 저장되는 객체 구조를 정의한다.

역할:
- 테이블 구조 정의
- 컬럼 및 연관관계 매핑
- JPA 엔티티 관리

예시:
- `User`
- `Meeting`
- `Todo`
- `Schedule`

즉, **DB와 직접 연결되는 객체는 entity 패키지에 위치한다.**  
도메인 구현 시 가장 먼저 정의해야 하는 기본 데이터 구조이다.

---

### 4.2 repository
엔티티를 대상으로 데이터베이스 접근을 담당하는 계층이다.  
보통 JPA Repository 인터페이스를 정의한다.

역할:
- DB 조회
- DB 저장
- DB 수정
- DB 삭제

예시:
- `UserRepository`
- `MeetingRepository`
- `TodoRepository`

즉, **DB 접근 책임은 repository 계층이 담당한다.**  
엔티티 정의 이후, 실제 데이터를 저장하고 조회하기 위한 계층이다.

---

### 4.3 dto
계층 간 데이터 전달을 위한 객체를 두는 패키지이다.  
요청(Request), 응답(Response), 내부 전달용 DTO 등을 관리한다.

역할:
- 요청 데이터 구조 정의
- 응답 데이터 구조 정의
- 엔티티를 직접 노출하지 않기 위한 중간 객체 역할

예시:
- `UserSignupRequestDto`
- `LoginRequestDto`
- `MeetingResponseDto`

즉, **클라이언트와 주고받는 데이터 형식을 정의하는 계층이다.**  
Repository까지 구성된 이후, API 입출력 형식을 정리하기 위해 설계한다.

---

### 4.4 service
실제 비즈니스 로직을 수행하는 계층이다.  
컨트롤러에서 받은 요청을 처리하고, 필요한 경우 repository를 통해 DB에 접근한다.

역할:
- 핵심 비즈니스 로직 처리
- 데이터 검증 및 가공
- 여러 repository 호출 조합
- 트랜잭션 처리

예시:
- 회원가입 처리
- 로그인 검증
- 회의 생성 및 조회
- Todo 생성 및 상태 변경

즉, **서비스의 핵심 동작은 service 계층에서 수행한다.**  
엔티티, 레포지토리, DTO가 준비된 이후 실제 기능 로직을 구현하는 단계이다.

---

### 4.5 controller
클라이언트의 요청을 직접 받는 계층이다.  
URL 매핑, 요청 파라미터 처리, 요청 본문 수신, 응답 반환을 담당한다.

역할:
- API 엔드포인트 정의
- 요청 데이터 수신
- 서비스 계층 호출
- 결과를 응답 형태로 반환

예시:
- 회원가입 API
- 로그인 API
- 회의 생성 API
- Todo 조회 API

즉, **클라이언트와 가장 먼저 만나는 계층이다.**  
실제 개발 순서상으로는 서비스 로직 구현 이후, 이를 외부에 노출하는 마지막 진입점으로 작성하는 것이 일반적이다.

---

### 4.6 API 경로 설정 및 확인
컨트롤러 구현 이후에는 각 API의 요청 경로와 메서드 방식을 명확히 정리해야 한다.  
이 단계에서는 실제 프론트엔드와 연결될 URL 구조를 확정하고, 요청/응답이 정상적으로 동작하는지 확인한다.

역할:
- URL 경로 설계
- HTTP Method 구분
- 요청/응답 형식 점검
- Swagger를 통한 테스트 및 문서화
- 프론트엔드 연동 준비

예시:
- `POST /api/users/signup`
- `POST /api/users/login`
- `GET /api/meeting/{id}`

즉, **컨트롤러 구현 이후 최종적으로 API 경로를 정리하고 검증하는 단계이다.**

---

## 5. 선택적으로 사용할 수 있는 하위 패키지

### 5.1 mapper
DTO와 Entity 간 변환이 많을 경우 사용하는 패키지이다.

역할:
- Entity → DTO 변환
- DTO → Entity 변환

예시:
- `UserMapper`
- `MeetingMapper`

---

### 5.2 enums
해당 도메인에서만 사용하는 enum 타입을 정의하는 패키지이다.

역할:
- 상태값 정의
- 역할 정의
- 유형 정의

예시:
- `UserRole`
- `MeetingStatus`

---

## 6. 예시: User 도메인 구조

```bash
domain/
└── user/
    ├── controller/
    │   └── UserController.java
    ├── service/
    │   └── UserService.java
    ├── dto/
    │   ├── UserSignupRequestDto.java
    │   ├── LoginRequestDto.java
    │   └── UserResponseDto.java
    ├── entity/
    │   └── User.java
    └── repository/
        └── UserRepository.java
```

### User 도메인 예시 설명
- `UserController` : 회원 관련 API 요청을 처리한다.
- `UserService` : 회원가입, 로그인, 사용자 정보 조회 등의 비즈니스 로직을 처리한다.
- `UserSignupRequestDto` : 회원가입 요청 데이터를 전달한다.
- `UserResponseDto` : 사용자 정보를 응답 형식으로 반환한다.
- `User` : 사용자 테이블과 매핑되는 엔티티이다.
- `UserRepository` : 사용자 데이터에 대한 DB 접근을 처리한다.

---

## 7. 패키지 생성 규칙

### 7.1 도메인 생성 시 규칙
새로운 기능을 추가할 때는 먼저 해당 기능이 어떤 도메인에 속하는지 판단한다.  
하나의 기능 단위는 하나의 도메인 패키지 안에서 관리하는 것을 원칙으로 한다.

예시:
- 회원 관련 기능 → `domain/user`
- 회의 관련 기능 → `domain/meeting`
- Todo 관련 기능 → `domain/todo`
- 일정 관련 기능 → `domain/schedule`
- 회의 분석 관련 기능 → `domain/analysis`
- 외부 API 연동 관련 기능 → `domain/external`

---

### 7.2 도메인 내부 패키지 생성 규칙
새로운 도메인을 생성하면 기본적으로 아래 패키지를 만든다.

- `entity`
- `repository`
- `dto`
- `service`
- `controller`

필요에 따라 아래를 추가한다.

- `mapper`
- `enums`

---

### 7.3 공통 코드 배치 규칙
- 전역 설정, 예외 처리, 보안, 응답 형식 → `global`
- 상수, enum, helper → `common`
- 특정 기능에만 속하는 코드 → 해당 `domain`

---

### 7.4 개발 권장 순서
하나의 도메인을 구현할 때는 일반적으로 아래 순서로 개발하는 것을 권장한다.

1. `entity` 작성
2. `repository` 작성
3. `dto` 작성
4. `service` 작성
5. `controller` 작성
6. API 경로 및 요청/응답 테스트

즉, **도메인 구현은 DB 구조 정의부터 시작하여 최종 API 확인 단계까지 순차적으로 진행하는 것을 원칙으로 한다.**

---

## 8. 정리

### 기본 원칙
- 기능 중심 코드는 `domain`
- 전역 공통 처리는 `global`
- 단순 공통 요소는 `common`

### 도메인 생성 시 기본 구조
- `entity`
- `repository`
- `dto`
- `service`
- `controller`

### 개발 순서
- `entity` → `repository` → `dto` → `service` → `controller` → API 경로 확인

### 목적
이 구조를 통일하면 다음과 같은 장점이 있다.

- 팀원 간 코드 위치를 쉽게 예측할 수 있다.
- 기능별 책임이 명확해진다.
- 유지보수와 확장이 쉬워진다.
- 협업 시 충돌을 줄일 수 있다.