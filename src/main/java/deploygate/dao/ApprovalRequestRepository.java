package deploygate.dao;

import deploygate.entity.ApprovalRequest;
import deploygate.entity.ApprovalRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    Optional<ApprovalRequest> findByDeployerIdAndStackNameAndActionAndStatus(
            Long deployerId, String stackName, String action, ApprovalRequestStatus status);

    Optional<ApprovalRequest> findTopByDeployerIdAndStackNameAndActionOrderByCreatedAtDesc(
            Long deployerId, String stackName, String action);
}
