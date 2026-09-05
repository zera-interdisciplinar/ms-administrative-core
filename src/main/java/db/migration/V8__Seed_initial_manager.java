package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.zera.ms_administrative_core.infrastructure.persistence.postgres.bootstrap.BootstrapAdminSettings;

/**
 * Semeia o primeiro usuario MANAGER (e a arvore organization -> unit que ele exige) a partir das
 * variaveis de ambiente {@code BOOTSTRAP_ADMIN_*}. Sem essas variaveis a migration e um no-op, de
 * modo que ambientes ja provisionados nao sao afetados.
 *
 * <p>E idempotente pelo email: se ja existir uma conta com o email informado, nada e feito.
 */
public class V8__Seed_initial_manager extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V8__Seed_initial_manager.class);

    @Override
    public void migrate(Context context) throws Exception {
        Optional<BootstrapAdminSettings> maybeSettings =
                BootstrapAdminSettings.fromEnv(System::getenv);
        if (maybeSettings.isEmpty()) {
            log.info("V8 seed: BOOTSTRAP_ADMIN_EMAIL/PASSWORD/ORG_CNPJ ausentes — seed ignorado.");
            return;
        }
        BootstrapAdminSettings settings = maybeSettings.get();
        Connection connection = context.getConnection();

        if (accountExists(connection, settings.adminEmail())) {
            log.info("V8 seed: conta {} ja existe — nada a fazer.", settings.adminEmail());
            return;
        }

        UUID organizationId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        String passwordHash = new BCryptPasswordEncoder().encode(settings.rawPassword());

        execute(connection,
                "INSERT INTO organization (id, name, cnpj, status, email, plan, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                organizationId, settings.organizationName(), settings.organizationCnpj(),
                "ACTIVE", settings.organizationEmail(), settings.organizationPlan(), now, now);

        execute(connection,
                "INSERT INTO unit (id, name, organization_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                unitId, settings.unitName(), organizationId, now, now);

        execute(connection,
                "INSERT INTO user_account (id, name, role, password, email, status, unit_id, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                userId, settings.adminName(), "MANAGER", passwordHash, settings.adminEmail(),
                "ACTIVE", unitId, now, now);

        log.info("V8 seed: MANAGER inicial criado (email={}, organizationId={}, unitId={}).",
                settings.adminEmail(), organizationId, unitId);
    }

    private static boolean accountExists(Connection connection, String email) throws Exception {
        try (PreparedStatement ps =
                connection.prepareStatement("SELECT 1 FROM user_account WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void execute(Connection connection, String sql, Object... params) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        }
    }
}
