package com.sazzler.ecommerce.sazzlerapigateway.filters;


import com.sazzler.ecommerce.util.JWTutil.JWTUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
        String id = null;
        String jwtToken = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwtToken = authorizationHeader.substring(7);
            try {
                // Validate the JWT token and extract user details
                id = jwtUtility.extractUserDetails(jwtToken);
            } catch (JwtException e) {
                // If the JWT token is invalid, set the response status to 401 Unauthorized
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        if (id != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Load user details from the database
                if (jwtUtility.validateToken(jwtToken)) {
                    // Set the authentication in the security context
                    var authentication = new UsernamePasswordAuthenticationToken(userDetails, null);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    /*
                    Security context temporary store  users identity and permissions for the
                    duration of a single http request
                    SecurityContext is an object held by the SecurityContextHolder
                     has 3 objects
                     Principle-UserDetails object
                     Credentials-null for the jwt
                     Authorities-User's granted authorities
                     */
                } else {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            } catch (UserNotFoundException e) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        // Critical: Continue the filter chain.
        filterChain.doFilter(req, res);
    }
}
