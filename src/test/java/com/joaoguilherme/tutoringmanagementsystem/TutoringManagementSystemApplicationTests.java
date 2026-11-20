package com.joaoguilherme.tutoringmanagementsystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "security.jwt.secret=test-secret-with-enough-length-for-hmac")
class TutoringManagementSystemApplicationTests {

    @Test
    void contextLoads() {
    }
}
