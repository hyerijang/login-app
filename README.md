# login-app

## 개요
Google OAuth2로 로그인하고, 로그인 성공 시 JWT 액세스 토큰과 리프레시 토큰(raw)을 쿠키로 전달합니다. 리프레시 토큰은 서버에 SHA-256 해시로 저장합니다. 개발용 H2 데이터베이스와 Thymeleaf + Bootstrap 기반 UI를 제공합니다.

## 화면 시안

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

## 설정(빠른 체크)
`src/main/resources/application.properties`에서 다음 값을 확인/수정하세요:
- `spring.security.oauth2.client.registration.google.client-id`
- `spring.security.oauth2.client.registration.google.client-secret`
- `app.jwt.secret` (JWT 서명용)
- `app.jwt.expiration-ms`, `app.refresh-token.expiration-ms`
- `app.cookie.secure` (개발: false, 운영: true)
- JPA 설정(`spring.jpa.hibernate.ddl-auto`)은 운영 환경에서 적절히 변경

## 실행 (Windows)
프로젝트 루트에서:

```cmd
gradlew.bat clean build
gradlew.bat bootRun
```
웹브라우저에서 `http://localhost:8080/` 접속
H2 콘솔: `http://localhost:8080/h2-console`

## 주의사항
- 실제 운영 시 `app.jwt.secret`과 OAuth 클라이언트 시크릿을 환경변수 또는 시크릿 매니저로 관리하세요.
- `spring.jpa.hibernate.ddl-auto=create-drop`는 개발 전용입니다.
- HTTPS(또는 `app.cookie.secure=true`) 환경에서 쿠키 보안을 반드시 적용하세요.