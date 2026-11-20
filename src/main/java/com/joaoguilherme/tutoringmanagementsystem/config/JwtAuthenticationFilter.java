package com.joaoguilherme.tutoringmanagementsystem.config;

import com.joaoguilherme.tutoringmanagementsystem.exception.InvalidTokenException;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.repository.UserRepository;
import com.joaoguilherme.tutoringmanagementsystem.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            findActiveUser(header.substring(BEARER_PREFIX.length())).ifPresent(user ->
                    SecurityContextHolder.getContext().setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities())));
        }
        filterChain.doFilter(request, response);
    }

    private Optional<User> findActiveUser(String token) {
        try {
            UUID userId = tokenService.validate(token);
            return userRepository.findById(userId).filter(User::isEnabled);
        } catch (InvalidTokenException ex) {
            return Optional.empty();
        }
    }
}
