package deploygate.dao;

import deploygate.entity.StackPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StackPolicyRepository extends JpaRepository<StackPolicy, Long> {

    Optional<StackPolicy> findByStackName(String stackName);
}
