package deploygate.policy;

import deploygate.entity.ApprovalLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ApprovalPolicyResolver {

    private final Map<ApprovalLevel, ApprovalPolicy> policiesByLevel;

    public ApprovalPolicyResolver(List<ApprovalPolicy> policies) {
        this.policiesByLevel = policies.stream()
                .collect(Collectors.toMap(ApprovalPolicy::getSupportedLevel, Function.identity()));
    }

    public Optional<ApprovalPolicy> resolve(ApprovalLevel level) {
        return Optional.ofNullable(policiesByLevel.get(level));
    }
}
