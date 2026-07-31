package deploygate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "approval_request")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deployerId;

    @Column(nullable = false)
    private String stackName;

    @Column(nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalRequestStatus status;

    @Column(nullable = false)
    private int requiredApprovals;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant decidedAt;

    public void approve(Instant now) {
        this.status = ApprovalRequestStatus.APPROVED;
        this.decidedAt = now;
    }

    public void reject(Instant now) {
        this.status = ApprovalRequestStatus.REJECTED;
        this.decidedAt = now;
    }
}
