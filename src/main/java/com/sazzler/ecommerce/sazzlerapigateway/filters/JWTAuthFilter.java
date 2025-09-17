package com.sazzler.ecommerce.sazzlerapigateway.filters;


import com.sazzler.ecommerce.util.JWTutil.JWTUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JWTAuthFilter extends OncePerRequestFilter {
    // This class will handle JWT authentication for incoming requests.
    // It will extend OncePerRequestFilter to ensure that the filter is applied once per request.

    private final JWTUtil jwtUtility;

    @Autowired
    public JWTAuthFilter(JWTUtil jwtUtility ) {
        this.jwtUtility = jwtUtility;
    }

    // Override the doFilterInternal method to implement JWT authentication logic.
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = req.getHeader("Authorization");

        // If there's no Authorization header or it doesn't start with Bearer, continue the chain
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(req, res);
            return;
        }

        String jwtToken = authorizationHeader.substring(7);

        try {
            // First validate the token
            if (!jwtUtility.validateToken(jwtToken)) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Extract user id and authorities from the token
            String id = jwtUtility.extractUserId(jwtToken);
            Set<String> authoritiesSet = jwtUtility.extractAuthorities(jwtToken);

            // Only set authentication if not already set
            if (id != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<GrantedAuthority> authorities = authoritiesSet.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                var authentication = new UsernamePasswordAuthenticationToken(id, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException e) {
            // If the JWT token is invalid or parsing failed, set the response status to 401 Unauthorized
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Continue the filter chain.
        filterChain.doFilter(req, res);
    }
}
