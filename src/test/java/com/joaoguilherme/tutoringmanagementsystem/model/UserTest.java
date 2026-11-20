package com.joaoguilherme.tutoringmanagementsystem.model;

import com.joaoguilherme.tutoringmanagementsystem.model.enums.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

public class UserTest {

    @Test
    void shouldDefaultToUserRoleWhenRoleIsNotSet() {
        User user = new User();

        Assertions.assertEquals(UserRole.USER, user.getRole());
    }

    @Test
    void shouldExposeRoleAsAuthorityWhenUserIsAdmin() {
        User user = new User();
        user.setRole(UserRole.ADMIN);

        List<String> authorities = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        Assertions.assertEquals(List.of("ROLE_ADMIN"), authorities);
    }
}
