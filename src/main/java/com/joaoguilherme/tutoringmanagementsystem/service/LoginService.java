package com.joaoguilherme.tutoringmanagementsystem.service;

import com.joaoguilherme.tutoringmanagementsystem.dto.LoginResponse;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public LoginResponse login(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        User user = (User) authentication.getPrincipal();
        TokenService.IssuedToken issuedToken = tokenService.generate(user);
        return new LoginResponse(issuedToken.token(), "Bearer", issuedToken.expiresAt());
    }
}
