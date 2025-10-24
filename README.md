# login-app

## 개요

Google OAuth2로 로그인하고, 로그인 성공 시 JWT 액세스 토큰(access token)과 리프레시 토큰(refresh token, raw)을 쿠키로 전달하는 간단한 예제 Spring Boot
애플리케이션입니다. 리프레시 토큰은 서버에 SHA-256 해시로 저장하여 원본 토큰을 안전하게 관리합니다. 개발용으로 H2 인메모리 데이터베이스와 Thymeleaf 기반의 UI(부트스트랩 포함)를 제공합니다.

주요 기능

- Google OAuth2 로그인 연동
- JWT 액세스 토큰 발급 및 쿠키 전송
- 리프레시 토큰 저장(해시) 및 관리
- H2 DB(개발용), Thymeleaf 템플릿, Bootstrap UI

### 개발 환경
<div style="text-align:center">
  <img alt="Java 17 badge" src="https://img.shields.io/badge/Java%20-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img alt="Gradle 8.14.3 badge" src="https://img.shields.io/badge/Gradle-8.14.3-02303A?style=for-the-badge&logo=gradle&logoColor=white" />
  <img alt="Spring Boot 3.5.6 badge" src="https://img.shields.io/badge/Spring%20Boot-3.5.6-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
</div>
<div style="text-align:center">
  <img alt="Spring Security OAuth2 Client 6.5.5 badge" src="https://img.shields.io/badge/Spring%20Security%20OAuth2%20Client-6.5.5-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white" />
  <img alt="H2 2.1.214 badge" src="https://img.shields.io/badge/H2-2.1.214-0F172A?style=for-the-badge&logo=h2-database&logoColor=white" />
  <img alt="Spring Data JPA badge" src="https://img.shields.io/badge/Spring%20Data%20JPA-3.5.6-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
</div>
<div style="text-align:center">
  <img alt="Thymeleaf 3.3.6 badge" src="https://img.shields.io/badge/Thymeleaf-3.5.6-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white" />
  <img alt="Bootstrap 5.3.3 badge" src="https://img.shields.io/badge/Bootstrap-5.3.3-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white" />
  <img alt="GitHub badge" src="https://img.shields.io/badge/GitHub-–-100000?style=for-the-badge&logo=github&logoColor=white" />
</div>

## 화면 및 다이어그램

### 로그인

<img src="https://github.com/user-attachments/assets/c1261ae9-7728-495a-9d7f-153d8e3fc899" alt="login1" width="24%" />
<img src="https://github.com/user-attachments/assets/4f08070d-f466-4fee-99e5-9f49e060c574" alt="login2" width="24%" />
<img src="https://github.com/user-attachments/assets/035ec54e-8251-4bbc-a971-fc39caddf2eb" alt="login3" width="24%" />
<img src="https://github.com/user-attachments/assets/f3c686df-d771-4cd2-af51-ef11281d52ef" alt="logout" width="24%" />

### 로그아웃

<img src="https://github.com/user-attachments/assets/f3c686df-d771-4cd2-af51-ef11281d52ef" alt="logout" width="24%" />
<img src="https://github.com/user-attachments/assets/c1261ae9-7728-495a-9d7f-153d8e3fc899" alt="login1" width="24%" />

## ERD

```mermaid
---
config:
  layout: dagre
  look: classic
  theme: default
---
erDiagram
	direction LR
	USERS {
		BIGINT id PK "식별자 (자동생성)"  
		VARCHAR nickname  "별칭/닉네임"  
		VARCHAR picture_url  "프로필 이미지 URL"  
		VARCHAR role  "권한 (UserRole enum: ROLE_USER/ROLE_ADMIN)"  
		DATETIME created_at  "생성 시각 (BaseTimeEntity)"  
		DATETIME updated_at  "수정 시각 (BaseTimeEntity)"  
	}
	USER_DETAIL {
		BIGINT id PK "식별자 (자동생성)"  
		VARCHAR name  "실명"  
		VARCHAR email  "이메일"  
		BIGINT user_id FK "연결된 사용자 id (User)"  
		DATETIME created_at  "생성 시각 (BaseTimeEntity)"  
		DATETIME updated_at  "수정 시각 (BaseTimeEntity)"  
	}
	USER_SOCIAL {
		BIGINT id PK "식별자 (자동생성)"  
		VARCHAR provider  "소셜 제공자 (AuthProvider enum, 예: GOOGLE)"  
		VARCHAR provider_id  "소셜 제공자 고유 id"  
		BIGINT user_id FK "연결된 사용자 id (User)"  
		DATETIME created_at  "레코드 생성 시각 (UserSocial에 직접 선언)"  
	}
	USER_ACCOUNT {
		BIGINT id PK "식별자 (자동생성)"  
		VARCHAR username  "로그인 계정명"  
		VARCHAR password  "암호 (해시 저장 예상)"  
		BIGINT user_id FK "연결된 사용자 id (User)"  
		DATETIME created_at  "생성 시각 (BaseTimeEntity)"  
		DATETIME updated_at  "수정 시각 (BaseTimeEntity)"  
	}
	REFRESH_TOKENS {
		BIGINT id PK "식별자 (자동생성)"  
		VARCHAR token_hash  "리프레시 토큰 해시"  
		BIGINT user_id FK "연결된 사용자 id (User)"  
		DATETIME issued_at  "발급 시각"  
		DATETIME expires_at  "만료 시각"  
		BOOLEAN revoked  "폐기 여부"  
	}

	USERS||--||USER_DETAIL:"userDetail (1:1)"
	USERS||--||USER_ACCOUNT:"userAccount (1:1)"
	USERS||--o{USER_SOCIAL:"authProviders (1:N)"
	USERS||--o{REFRESH_TOKENS:"refreshTokens (1:N)"

```

## 시퀀스 다이어그램

### 로그인

```mermaid
sequenceDiagram
    participant Browser as 사용자
    participant Home as 홈컨트롤러
    participant App as 앱
    participant Provider as 인증제공자
    participant Service as OAuth서비스
    participant DB as DB
    participant Success as 성공핸들러
    participant JwtFilter as JWT필터

    Browser->>App: 로그인 시작
    App->>Provider: 인증 요청
    Provider->>Browser: 인증 화면
    Browser->>App: 콜백 수신
    App->>Service: 사용자 정보 요청
    Service->>DB: 사용자 조회 또는 생성
    Service-->>App: 사용자 반환
    App->>Success: 로그인 성공 처리
    Success->>App: 토큰 생성
    Success->>Browser: 쿠키 설정 및 리다이렉트
    Browser->>App: 후속 요청(쿠키 포함)
    App->>JwtFilter: 토큰 검증
    JwtFilter->>App: 인증 설정
    App->>Home: 홈 렌더링
    Home-->>Browser: 사용자 정보 표시
```

### 로그아웃

```mermaid
sequenceDiagram
    participant Browser as 사용자
    participant Home as 홈컨트롤러
    participant App as 앱
    participant Logout as 로그아웃핸들러
    participant DB as DB

    Browser->>App: 로그아웃 요청
    App->>Logout: 로그아웃 처리 시작
    Logout->>DB: 리프레시 토큰 삭제
    Logout->>Browser: 쿠키 제거
    Logout-->>Browser: 홈으로 리다이렉트
    App->>Home: 홈 페이지 렌더링
    Home-->>Browser: 비로그인 화면
```

## 요구사항 (로컬 개발)

- Java 17 이상 (프로젝트 설정에 따라 다름)
- Gradle Wrapper (프로젝트에 포함됨)
- 인터넷 연결 (Google OAuth2 인증을 위해)

## 설정

애플리케이션 설정은 `src/main/resources/application.properties` 또는 운영 환경의 환경 변수로 제공합니다.
다음 값들을 반드시 설정하세요:

- spring.security.oauth2.client.registration.google.client-id
- spring.security.oauth2.client.registration.google.client-secret
- app.jwt.secret (JWT 서명용 비밀)

예시(환경변수 사용 권장)

- 윈도우 cmd 예시:

  setx SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID "your-client-id"
  setx SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET "your-client-secret"
  setx APP_JWT_SECRET "your-jwt-secret"

(참고: 실제 운영에서는 환경변수나 시크릿 매니저를 사용하세요.)

## 빌드 및 실행 (Windows)

프로젝트 루트에서 다음 명령을 실행합니다:

gradlew.bat clean build
gradlew.bat bootRun

정상 실행 시 브라우저에서 http://localhost:8080/ 접속
H2 콘솔: http://localhost:8080/h2-console (개발 중에만 사용)

테스트 실행:

gradlew.bat test

## 보안 주의사항

- `app.jwt.secret`과 OAuth 클라이언트 시크릿은 절대 버전관리 시스템에 커밋하지 마세요.
- 운영 환경에서는 HTTPS를 사용하고 `app.cookie.secure=true`를 설정하세요.
- `spring.jpa.hibernate.ddl-auto=create-drop` 또는 `update` 설정은 운영에 적합하지 않을 수 있습니다.

## 디렉터리 (주요)

- src/main/java/.../controller : 컨트롤러
- src/main/java/.../service : 서비스 로직
- src/main/java/.../repository : JPA 리포지토리
- src/main/resources/templates : Thymeleaf 템플릿
- src/main/resources/application.properties : 환경설정

## 가정 및 참고

- 이 README는 로컬 개발 환경을 기준으로 작성되었습니다.
- OAuth 클라이언트 등록(구글 콘솔)과 리디렉션 URI 설정은 사용자가 직접 수행해야 합니다.
