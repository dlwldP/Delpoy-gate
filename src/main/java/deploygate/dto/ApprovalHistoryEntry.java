package deploygate.dto;

import java.time.Instant;

public record ApprovalHistoryEntry(
        Long id,
        String deployer,
        String stack,
        String action,
        String result,
        String decidedBy,
        Instant decidedAt
) {
}
