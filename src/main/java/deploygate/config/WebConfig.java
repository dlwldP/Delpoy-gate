package deploygate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the admin frontend (a separately-served React app, see /frontend) to call
 * this API from a different origin during local development. The deployed backend
 * has no session/cookie-based auth, so this only widens which origins may read the
 * same unauthenticated data a direct curl already could.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${deploygate.cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/approval/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST");
        registry.addMapping("/admin/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET");
    }
}
