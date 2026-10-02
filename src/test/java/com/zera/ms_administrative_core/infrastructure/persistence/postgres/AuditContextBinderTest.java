package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.repository.AuditContextRepository;
import com.zera.ms_administrative_core.infrastructure.security.CurrentUserProvider;

@ExtendWith(MockitoExtension.class)
class AuditContextBinderTest {

    @Mock
    private AuditContextRepository auditContext;
    @Mock
    private CurrentUserProvider currentUser;

    @InjectMocks
    private AuditContextBinder binder;

    @Test
    @DisplayName("Deve publicar o usuario da requisicao")
    void shouldBindWhenUserPresent() {
        UUID usuario = UUID.randomUUID();
        when(currentUser.currentUserId()).thenReturn(Optional.of(usuario));

        binder.bindCurrentUser();

        verify(auditContext).bindUser(usuario);
    }

    /**
     * Job, agendador e token de servico nao tem pessoa por tras. Nao publicar nada e a informacao
     * correta: a auditoria grava usuario_app nulo, que significa "nao partiu de uma pessoa".
     */
    @Test
    @DisplayName("Nao deve publicar nada quando nao ha usuario na requisicao")
    void shouldNotBindWhenNoUser() {
        when(currentUser.currentUserId()).thenReturn(Optional.empty());

        binder.bindCurrentUser();

        verifyNoInteractions(auditContext);
    }
}
