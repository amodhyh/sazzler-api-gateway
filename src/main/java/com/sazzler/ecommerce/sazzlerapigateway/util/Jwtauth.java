package com.sazzler.ecommerce.sazzlerapigateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.sql.Date;
import java.util.*;

@Component
public class JWTUtility {
    //generate secret key from a external source

    //to generate in run time
    //final private Key secretKey = io.jsonwebtoken.security.Keys.secretKeyFor(SignatureAlgorithm.HS256);

    //expiration time
    // 1 hour in milliseconds
    @Getter
    private final Date expirationTime;
    //final private Date expirationTime = new Date(System.currentTimeMillis()+ 3600000);
    @Getter
    private final SecretKey secretKey;

    public JWTUtility(@Value("${security.jwt.secret-key}") String secretKeyString,@Value("${security.jwt.expiration-time}") String expirationTimeString) {
        // Decode the Base64-encoded secret key string
        // Keys.hmacShaKeyFor() correctly handles the byte array to create the SecretKey
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKeyString));
        long expirationMillis = Long.parseLong(expirationTimeString);
        this.expirationTime = new Date(expirationMillis);
    }

    //Generate JWT token
    public String generateToken(String username, Collection<? extends GrantedAuthority> authorities) {
        Set<String> authoritySet = new HashSet<>();
        authorities.forEach(auth -> authoritySet.add(auth.getAuthority()));

        // Convert the Set to a List for storage in the JWT claim(no duplications)
                /* JWT payloads are JSON objects, and the JSON specification doesn't have a native "set" data type.
                It only supports arrays (which are equivalent to Java's List).
                So, if you were to store a Java Set directly, it would be serialized as a JSON array anyway*/
        List<String> roles = new ArrayList<>(authoritySet);
        return io.jsonwebtoken.Jwts.builder()
                .setSubject(username)
                .claim("auth",roles)
                .setExpiration(new java.util.Date(System.currentTimeMillis() + expirationTime.getTime()))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .signWith(getSecretKey())
                .compact();
    }

    // Extract username from JWT token
    public String extractUserDetails(String token) {
        if(!validateToken(token)) {
            throw new JwtException("Invalid token");
        }
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    //Extract roles from JWT token
    public Collection<? extends GrantedAuthority>  extractAuthorities(String token) {
        Collection<? extends GrantedAuthority> userAuth=new HashSet<>();
        try {
            Map<String, Object> claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if(!claims.containsKey("auth")) {
                throw new JwtException("JWT token does not contain 'auth' claim");
            }
            Set<String> roles = (Set<String>) claims.get("auth");
            userAuth= roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role))
                    .toList();
        } catch (ExpiredJwtException e) {
            System.err.println("ExpiredJwtException");
        } catch (UnsupportedJwtException e) {
            System.err.println("UnsupportedJwtException");
        } catch (MalformedJwtException e) {
            System.err.println("MalformedJwtException");

        } catch (SignatureException e) {
            System.err.println("SignatureException");
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException");
        } catch (JwtException e) {
            System.err.println("JwtException");

        }
        return userAuth;
    }

    // Validate JWT token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())  // Use the secret key to validate the signature,throws SignatureException if the signature is invalid
                    .build()    //build the jwt parser object
                    .parseClaimsJws(token);  //Signature validation, header and payload parsing(decoding base64), and expiration check are all done here.
            // o.w it would pass as a jws token.
            return true; // Token is valid
        } catch (ExpiredJwtException e) {
            // Log the expiration error for debugging
            System.err.println("JWT Token is expired: " + e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            // Log other validation errors (e.g., malformed, bad signature)
            System.err.println("Invalid JWT Token: " + e.getMessage());
            return false;
        }
    }

}



