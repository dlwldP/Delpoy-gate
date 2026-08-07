package deploygate.dao;

import deploygate.entity.ApprovalLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalLogRepository extends JpaRepository<ApprovalLog, Long> {

    List<ApprovalLog> findAllByOrderByDecidedAtDesc(Pageable pageable);

    List<ApprovalLog> findAllByStackNameOrderByDecidedAtDesc(String stackName, Pageable pageable);

    List<ApprovalLog> findAllByDeployerNameOrderByDecidedAtDesc(String deployerName, Pageable pageable);

    List<ApprovalLog> findAllByStackNameAndDeployerNameOrderByDecidedAtDesc(
            String stackName, String deployerName, Pageable pageable);
}
