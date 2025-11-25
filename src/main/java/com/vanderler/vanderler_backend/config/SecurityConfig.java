package com.vanderler.vanderler_backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.vanderler.vanderler_backend.security.CustomUserDetailsService;
import com.vanderler.vanderler_backend.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 🔐 desabilita CSRF (API stateless com JWT)
            .csrf(csrf -> csrf.disable())

            // 🔓 ativa CORS global (usa o bean corsConfigurationSource)
            .cors(Customizer.withDefaults())

            // sessão stateless
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // regras de autorização
            .authorizeHttpRequests(auth -> auth
                // libera pré-flight CORS de qualquer rota
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 🔓 DEBUG: liberar /face/** pra gente ver se o controller está sendo chamado
                .requestMatchers("/face/**").permitAll()
                // se depois quiser exigir login:
                // .requestMatchers("/face/**").authenticated()

                // Auth público
                .requestMatchers("/auth/**").permitAll()

                // ===== PERFIL (precisa estar autenticado) =====
                .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()

                // ===== Biblioteca: só logado =====
                .requestMatchers("/api/library/**").authenticated()

                // ===== Catálogo de livros =====
                // Conteúdo do livro: só logado e com livro na biblioteca
                .requestMatchers(HttpMethod.GET, "/api/books/*/content").authenticated()
                // Lista / detalhes básicos: público
                .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()

                // Admin: criar/remover livros
                .requestMatchers(HttpMethod.POST, "/api/books/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasRole("ADMIN")

                // resto: precisa estar autenticado
                .anyRequest().authenticated()
            )

            // registra o AuthenticationProvider com o CustomUserDetailsService
            .authenticationProvider(authenticationProvider())

            // adiciona o filtro JWT antes do filtro padrão de login
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // === Encoder padrão (BCrypt) ===
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // === AuthenticationProvider usando o CustomUserDetailsService ===
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // === AuthenticationManager para injetar no UserService ===
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // 🔓 CORS global: libera o front Vite (http://localhost:5173)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // origem do front
        config.setAllowedOrigins(List.of("http://localhost:5173"));

        // métodos permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // headers permitidos
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // cache do preflight em segundos

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // aplica CORS pra todas as rotas
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
