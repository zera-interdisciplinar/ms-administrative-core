package com.zera.ms_administrative_core.core.usecase.auth.passwordReset;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zera.ms_administrative_core.core.domain.entity.PasswordResetToken;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.PasswordResetEmail;
import com.zera.ms_administrative_core.core.repository.PasswordResetMailGateway;
import com.zera.ms_administrative_core.core.repository.PasswordResetTokenRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;

@Service
public class RequestPasswordResetImpl implements RequestPasswordReset {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetTokenGenerator tokenGenerator;
    private final PasswordResetLinkFactory linkFactory;
    private final PasswordResetMailGateway mailGateway;

    public RequestPasswordResetImpl(UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetTokenGenerator tokenGenerator,
            PasswordResetLinkFactory linkFactory,
            PasswordResetMailGateway mailGateway) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.linkFactory = linkFactory;
        this.mailGateway = mailGateway;
    }

    // @Transactional por simetria com os outros fluxos de auth que escrevem tabela auditavel;
    // password_reset_token em si nao e auditada, mas a escrita faz parte do mesmo caso de uso.
    @Override
    @Transactional
    public void execute(String email) {
        User user = findActiveUserOrNull(email);
        if (user == null) {
            // Nunca revela ao chamador se o e-mail existe: o controller sempre responde 202.
            return;
        }

        String rawToken = tokenGenerator.newRawToken();
        PasswordResetToken token = PasswordResetToken.issue(
                user.getUserId(), tokenGenerator.hash(rawToken),
                LocalDateTime.now().plus(tokenGenerator.timeToLive()));
        passwordResetTokenRepository.save(token);

        mailGateway.send(new PasswordResetEmail(
                user.getEmail().value(), user.getName(), linkFactory.buildLink(rawToken)));
    }

    private User findActiveUserOrNull(String rawEmail) {
        Email parsedEmail;
        try {
            parsedEmail = new Email(rawEmail);
        } catch (RuntimeException malformed) {
            return null;
        }

        return userRepository.findByEmail(parsedEmail)
                .filter(user -> user.getStatus().isActive())
                .orElse(null);
    }
}
