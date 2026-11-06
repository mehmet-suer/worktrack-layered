package com.worktrack.security.jwt;


import com.worktrack.security.policy.AuthenticationRequestPolicy;
import com.worktrack.service.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AuthenticationRequestPolicy authenticationRequestPolicy;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserService userService,
                                   AuthenticationEntryPoint authenticationEntryPoint,
                                   AuthenticationRequestPolicy authenticationRequestPolicy) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authenticationRequestPolicy = authenticationRequestPolicy;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);
        try {
            final String username = jwtService.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                userService.findByUsername(username)
                        .ifPresent(user -> {
                            var authToken = new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    user.getAuthorities()
                            );
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        });
            }

            filterChain.doFilter(request, response);
        } catch (AuthenticationException ex) {
            authenticationEntryPoint.commence(request, response, ex);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return authenticationRequestPolicy.ignoresAccessToken(request);
    }

}
