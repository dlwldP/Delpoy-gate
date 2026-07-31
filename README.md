# Deploy-gate

> "누가 어떤 인프라 스택을 배포할 권한이 있는가"를 claims 기반으로 판단하는 배포 승인 게이트 서비스

## 개요

`deploy-gate`는 CI/CD 파이프라인에서 `cdk deploy` 직전에 호출되어, 요청자가 해당 인프라 스택을 배포할 권한이 있는지 판단하는 독립 인가 서비스입니다.

GitHub 저장소 권한(collaborator, write 등)은 "이 저장소에 접근 가능한가"만 구분할 뿐, "이 특정 인프라 스택을 배포해도 되는가"는 구분하지 못합니다. `deploy-gate`는 이 사이의 빈틈을 claims 기반 인가 모델로 메웁니다. AI/판단 로직 없이, claim과 정책 테이블을 매칭하는 순수 규칙 기반 서비스입니다.

## 배경

- **InfraHub와의 연결점**: InfraHub에서 다룬 claims 기반 인가 모델(웹 애플리케이션 리소스별 권한 관리)을, 대상만 "웹 리소스"에서 "인프라 스택"으로 바꿔 CI/CD 파이프라인에 그대로 적용
- IaC(AWS CDK)와 CI/CD(GitHub Actions)를 학습하며 발견한 문제: 배포 자동화 파이프라인은 "누구나 실행 권한만 있으면 모든 스택을 배포할 수 있다"는 구조적 허점을 갖기 쉬움
- AI 에이전트나 봇 없이, **결정론적 규칙(claim ↔ policy 매칭)만으로 완결되는 단일 도메인 문제**로 설계

## 해결하는 문제

- 신입/주니어 개발자는 테스트용 스택(`SmallAppStack`)은 배포 가능해도, 운영 스택(`ProdAlbStack`)은 배포하면 안 되는 상황
- GitHub 저장소 write 권한과 인프라 배포 권한이 동일시되는 문제
- 배포 승인/거부 이력이 남지 않아 사후 추적이 안 되는 문제

## 동작 흐름

```
GitHub Actions: cdk deploy 직전
        │
        ▼
POST /approval/check
  { "user": "jiye", "stack": "SmallAppStack", "action": "DEPLOY" }
        │
        ▼
[deploy-gate] — Spring Boot
  1. 요청자의 claim 조회 (예: "stack:SmallAppStack:deploy")
  2. stack_policy 테이블과 매칭 → 허용/거부/승인대기 판단
        │
   ┌────┼────────────┐
   ▼    ▼             ▼
 200 OK 403 Forbidden 202 Pending (승인 대기)
   │      │                │
   ▼      ▼                ▼
cdk deploy 워크플로우      승인권자 승인 시
 계속 진행  실패 처리       재요청 → 200 OK
```

## 데이터 모델

```
deployer
  - id, name
  - claims: ["stack:SmallAppStack:deploy", "stack:SmallAppStack:destroy"]

stack_policy
  - stack_name        # 예: "ProdAlbStack"
  - required_claim     # 예: "stack:ProdAlbStack:deploy"
  - approval_level     # NONE / SINGLE_APPROVER / DUAL_APPROVER

approval_log           # 감사 로그
  - deployer_id, stack_name, action, result, decided_by, decided_at
```

정책(`approval_level`)에 따라 승인 절차가 달라지도록, OOP 다형성으로 설계:

```java
public abstract class ApprovalPolicy {
    public abstract ApprovalResult evaluate(Deployer deployer, String stack);
}

public class NoApprovalPolicy extends ApprovalPolicy { /* claim만 있으면 즉시 승인 */ }
public class SingleApproverPolicy extends ApprovalPolicy { /* 승인권자 1인 필요 */ }
public class DualApproverPolicy extends ApprovalPolicy { /* 승인권자 2인 필요 (운영 스택) */ }
```

## API

| 엔드포인트 | 설명 |
|---|---|
| `POST /approval/check` | 즉시 판단 필요 시 호출 (CI 파이프라인에서 사용) |
| `POST /approval/request` | 승인 레벨이 높은 스택은 즉시 거부 대신 승인 대기 상태 생성 |
| `POST /approval/{id}/approve` | 승인권자가 대기 중인 요청 승인 |
| `POST /approval/{id}/reject` | 승인권자가 대기 중인 요청 거부 |
| `GET /approval/history` | 감사 로그 조회 (누가 언제 무엇을 승인/거부했는지) |

## 기술 스택

| 영역 | 기술 |
|---|---|
| 서비스 | Spring Boot |
| 인가 모델 | Claims 기반 (InfraHub 패턴 재사용) |
| 인프라 대상 | AWS CDK (Java) 로 정의된 스택 |
| 파이프라인 연동 | GitHub Actions |
| 헬스체크 | Spring Boot Actuator |
| DB | (검토 중 — 소규모 정책 테이블이라 PostgreSQL 또는 SQLite로 시작 가능) |

## 프로젝트 구조 (예정)

```
deploy-gate/
├── src/main/java/deploygate/
│   ├── domain/           # Deployer, StackPolicy, ApprovalLog
│   ├── policy/           # ApprovalPolicy 및 하위 구현체
│   ├── api/              # /approval/* 컨트롤러
│   └── config/           # Claims 인가 설정
├── .github/workflows/    # deploy-gate 자체 CI + 연동 예시 워크플로우
└── docs/                 # 정책 설계 문서
```

## 상태 및 MVP 범위

🚧 초기 설계 단계

### P1 — MVP 핵심
- `deployer` / `stack_policy` 데이터 모델 및 claim 매칭 로직
- `POST /approval/check` — 승인 없이 claim 매칭만으로 허용/거부 판단 (`NoApprovalPolicy`만 지원)
- GitHub Actions 연동 예시 워크플로우 (`cdk deploy` 전 호출)

### P2 — 2차
- `SingleApproverPolicy`, `DualApproverPolicy` 등 승인 레벨 확장
- `POST /approval/request` + `approve`/`reject` 승인 대기 흐름
- `approval_log` 감사 로그 및 `GET /approval/history` 조회 API

### P3 — 확장
- Actuator 기반 헬스체크 및 운영 모니터링 연동
- 정책 테이블을 코드 변경 없이 관리할 수 있는 간단한 관리 화면(옵션)
- 다른 CDK 스택/멀티 프로젝트로 확장 가능한 범용 라이브러리화 검토

## 왜 이 프로젝트인가

- InfraHub의 claims 기반 인가 모델을 "웹 리소스 인가"에서 "인프라 배포 인가"로 확장 — 하나의 인가 도메인 전문성을 두 프로젝트로 증명
- AI/봇 없이 순수 규칙 기반 설계로, 명확한 단일 도메인 문제에 집중
- OOP 상속/다형성(`ApprovalPolicy` 계층)을 실제 인가 정책 설계에 적용해보는 실습
