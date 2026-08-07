package deploygate.security;

import java.util.Set;

/**
 * The authenticated principal behind a request. For {@link CallerType#DEPLOYER} the
 * name is a proven identity; for {@link CallerType#CI_SERVICE} there is no personal
 * identity at all and {@code name} is just a label.
 */
public record AuthenticatedCaller(CallerType type, String name, Set<String> claims) {

    public boolean isDeployer() {
        return type == CallerType.DEPLOYER;
    }
}
