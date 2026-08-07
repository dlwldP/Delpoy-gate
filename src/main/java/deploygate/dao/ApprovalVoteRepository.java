package deploygate.dao;

import deploygate.entity.ApprovalVote;
import deploygate.entity.VoteDecision;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalVoteRepository extends JpaRepository<ApprovalVote, Long> {

    boolean existsByRequestIdAndApproverId(Long requestId, Long approverId);

    long countByRequestIdAndDecision(Long requestId, VoteDecision decision);
}
