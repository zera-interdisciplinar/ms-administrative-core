package com.zera.ms_administrative_core.infrastructure.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.zera.ms_administrative_core.core.usecase.auth.Login;
import com.zera.ms_administrative_core.core.usecase.user.findUser.FindAllUsers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private Login login;
    @MockitoBean private FindAllUsers findAllUsers;

    @Test
    void healthProbeIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void jwksEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json")).andExpect(status().isOk());
    }

    @Test
    void loginEndpointIsPublic() throws Exception {
        when(login.execute(any(), any()))
                .thenReturn(com.zera.ms_administrative_core.core.usecase.auth.TokenPair
                        .bearer("access", "refresh", 900));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"a@b.com\",\"password\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsValidJwt() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(jwt().jwt(builder -> builder.claim("role", "MANAGER"))))
                .andExpect(status().isOk());
    }
}
