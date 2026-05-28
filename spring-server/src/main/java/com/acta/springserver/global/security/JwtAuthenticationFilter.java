package com.acta.springserver.global.security;

import com.acta.springserver.domain.auth.repository.BlacklistedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String JWT_EXCEPTION_ATTRIBUTE = "jwtException";

    private final JwtTokenProvider jwtTokenProvider;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = jwtTokenProvider.resolveToken(request);

        try {
            if (token != null) {
                if (blacklistedTokenRepository.existsByToken(token)) {
                    SecurityContextHolder.clearContext();
                } else {
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            request.setAttribute(JWT_EXCEPTION_ATTRIBUTE, e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}