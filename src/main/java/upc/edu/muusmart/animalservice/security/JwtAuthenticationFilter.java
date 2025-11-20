package upc.edu.muusmart.animalservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        log.info("📥 [JwtFilter] Nueva solicitud: {} {}", request.getMethod(), request.getRequestURI());

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("⚠️ [JwtFilter] No se encontró header Authorization válido.");
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        log.info("🪙 [JwtFilter] Token extraído: {}", jwt);

        String username;
        try {
            username = jwtUtil.extractUsername(jwt);

            // Normalización segura
            username = username == null ? null : username.trim();
            username = username.replace("\u200B", "");
            // username = username.toLowerCase();  // Habilitar si quieres usernames case-insensitive

            log.info("👤 [JwtFilter] Username normalizado: '{}'", username);

        } catch (Exception e) {
            log.error("❌ [JwtFilter] Error al extraer username del token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Si ya hay autentificación previa, continuar
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            if (jwtUtil.validateToken(jwt)) {

                // ============================
                //  EXTRAER ROLES DEL TOKEN
                // ============================
                List<String> roles = new ArrayList<>();
                try {
                    Object rolesClaim = jwtUtil.extractClaim(jwt, claims -> claims.get("roles"));
                    log.info("🔎 [JwtFilter] Raw roles claim: {}", rolesClaim);

                    if (rolesClaim instanceof List<?> list) {
                        for (Object r : list) roles.add(r.toString());
                    } else if (rolesClaim instanceof String str) {
                        for (String r : str.split(",")) roles.add(r.trim());
                    }

                    log.info("📄 [JwtFilter] Roles procesados: {}", roles);
                } catch (Exception e) {
                    log.error("⚠️ [JwtFilter] Error leyendo roles del token: {}", e.getMessage());
                }

                // Autoridades
                var authorities = new ArrayList<org.springframework.security.core.GrantedAuthority>();
                for (String role : roles) {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(role));
                    log.info("➡️ [JwtFilter] Añadiendo autoridad: {}", role);
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("✅ [JwtFilter] SecurityContext configurado correctamente para '{}'", username);

            } else {
                log.warn("❌ [JwtFilter] Token inválido");
            }
        }

        filterChain.doFilter(request, response);
    }
}
