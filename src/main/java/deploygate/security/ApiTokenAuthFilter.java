package deploygate.security;

import deploygate.dao.DeployerRepository;
import deploygate.entity.Deployer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Resolves {@code Authorization: Bearer <token>} into an {@link AuthenticatedCaller}.
 *
 * <p>A deployer's claims become granted authorities, so authorization rules can be
 * expressed against the very same claim strings the approval policies use.
 */
@Component
public class ApiTokenAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final DeployerRepository deployerRepository;
    private final String ciServiceToken;

    public ApiTokenAuthFilter(DeployerRepository deployerRepository,
                              @Value("${deploygate.ci.token:}") String ciServiceToken) {
        this.deployerRepository = deployerRepository;
        this.ciServiceToken = ciServiceToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = bearerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            resolve(token).ifPresent(caller -> {
                var authentication = new UsernamePasswordAuthenticationToken(caller, null, authorities(caller));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        chain.doFilter(request, response);
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private java.util.Optional<AuthenticatedCaller> resolve(String token) {
        // The CI service token is configured, not stored per-deployer, so it is checked first.
        if (!ciServiceToken.isEmpty() && ApiTokens.matches(token, ciServiceToken)) {
            return java.util.Optional.of(new AuthenticatedCaller(CallerType.CI_SERVICE, "ci-service", Set.of()));
        }
        return deployerRepository.findByApiTokenHash(ApiTokens.hash(token))
                .map(this::toCaller);
    }

    private AuthenticatedCaller toCaller(Deployer deployer) {
        return new AuthenticatedCaller(CallerType.DEPLOYER, deployer.getName(), Set.copyOf(deployer.getClaims()));
    }

    private List<GrantedAuthority> authorities(AuthenticatedCaller caller) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + caller.type().name()));
        caller.claims().forEach(claim -> authorities.add(new SimpleGrantedAuthority(claim)));
        return authorities;
    }
}
