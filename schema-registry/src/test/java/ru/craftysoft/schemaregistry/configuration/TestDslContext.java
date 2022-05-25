package ru.craftysoft.schemaregistry.configuration;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jetbrains.annotations.Nullable;
import org.jooq.ConnectionProvider;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DefaultDSLContext;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;

import static ru.craftysoft.schemaregistry.util.TestDbHelper.getConnection;

@ApplicationScoped
@Alternative
public class TestDslContext extends DefaultDSLContext {

    public TestDslContext() {
        super(SQLDialect.POSTGRES);
    }

    @Inject
    public TestDslContext(@ConfigProperty(name = "quarkus.datasource.jdbc.url") String url,
                          @ConfigProperty(name = "quarkus.datasource.username") String username,
                          @ConfigProperty(name = "quarkus.datasource.password") String password) {
        super(new ConnectionProvider() {
            @Override
            public @Nullable Connection acquire() throws DataAccessException {
                return getConnection(url, username, password);
            }

            @Override
            public void release(Connection connection) throws DataAccessException {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new DataAccessException(e.getMessage(), e);
                }
            }
        }, SQLDialect.POSTGRES, new Settings().withRenderFormatted(true));
    }

}

