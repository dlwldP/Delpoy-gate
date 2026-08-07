package deploygate.dto;

import java.util.Set;

public record DeployerSummary(
        Long id,
        String name,
        Set<String> claims
) {
}
