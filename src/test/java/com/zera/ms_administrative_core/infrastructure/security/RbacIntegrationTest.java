package com.zera.ms_administrative_core.infrastructure.security;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.zera.ms_administrative_core.core.usecase.organization.registerOrganization.RegisterOrganization;
import com.zera.ms_administrative_core.core.usecase.user.changeUserPassword.ChangePassword;
import com.zera.ms_administrative_core.core.usecase.user.findUser.FindAllUsers;
import com.zera.ms_administrative_core.core.usecase.user.suspendUser.SuspendUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean private SuspendUser suspendUser;
    @MockitoBean private ChangePassword changePassword;
    @MockitoBean private FindAllUsers findAllUsers;
    @MockitoBean private RegisterOrganization registerOrganization;

    private static MockHttpServletRequestBuilder asRole(MockHttpServletRequestBuilder request, String role) {
        return request.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role))
                .jwt(builder -> builder.subject(UUID.randomUUID().toString())));
    }

    // --- rota MANAGER-only: PATCH /users/{id}/suspend ---

    @Test
    void suspendUserRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/suspend", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void suspendUserForbiddenForEmployee() throws Exception {
        mockMvc.perform(asRole(patch("/api/v1/users/{id}/suspend", UUID.randomUUID()), "EMPLOYEE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendUserAllowedForManager() throws Exception {
        mockMvc.perform(asRole(patch("/api/v1/users/{id}/suspend", UUID.randomUUID()), "MANAGER"))
                .andExpect(status().isNoContent());
    }

    // --- rota admin de organizacao: POST /organization ---

    @Test
    void registerOrganizationForbiddenForEmployee() throws Exception {
        mockMvc.perform(asRole(post("/api/v1/organization"), "EMPLOYEE")
                        .contentType("application/json")
                        .content("{\"name\":\"X\",\"cnpj\":\"11222333000181\",\"email\":\"x@y.com\",\"plan\":\"FREE\"}"))
                .andExpect(status().isForbidden());
    }

    // --- leitura: GET /users e permitida para qualquer autenticado ---

    @Test
    void listUsersAllowedForEmployee() throws Exception {
        mockMvc.perform(asRole(get("/api/v1/users"), "EMPLOYEE"))
                .andExpect(status().isOk());
    }

    // --- self-service: PATCH /users/{id}/password ---

    @Test
    void changePasswordAllowedForTheOwner() throws Exception {
        UUID ownerId = UUID.randomUUID();
        mockMvc.perform(patch("/api/v1/users/{id}/password", ownerId)
                        .with(jwt().jwt(b -> b.subject(ownerId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                        .contentType("application/json")
                        .content("{\"rawPassword\":\"NovaSenha1\",\"confirmPassword\":\"NovaSenha1\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void changePasswordForbiddenForADifferentEmployee() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/password", UUID.randomUUID())
                        .with(jwt().jwt(b -> b.subject(UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                        .contentType("application/json")
                        .content("{\"rawPassword\":\"NovaSenha1\",\"confirmPassword\":\"NovaSenha1\"}"))
                .andExpect(status().isForbidden());
    }

    // --- token real: o claim `role` e convertido em ROLE_ pelo JwtAuthenticationConverter ---

    @Test
    void realTokenWithRoleClaimGrantsManagerAuthority() throws Exception {
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .issuer("ms-administrative-core")
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(300))
                .subject(UUID.randomUUID().toString())
                .claim("role", "MANAGER")
                .build())).getTokenValue();

        mockMvc.perform(patch("/api/v1/users/{id}/suspend", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
