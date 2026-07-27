package com.minnminn.user_registration_system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves SQLite DB whether IntelliJ runs from project root or module folder.
 */
@Configuration
public class SqliteDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(SqliteDataSourceConfig.class);

    @Bean
    public DataSource dataSource() {
        Path db = resolveDbPath();
        String url = "jdbc:sqlite:" + db.toAbsolutePath().normalize();
        log.info("Using SQLite database: {}", url);
        return DataSourceBuilder.create()
                .driverClassName("org.sqlite.JDBC")
                .url(url)
                .build();
    }

    private Path resolveDbPath() {
        Path[] candidates = new Path[] {
                Paths.get("user-registration-system", "user_registration_system.db"),
                Paths.get("user_registration_system.db"),
                Paths.get("user-registration-system", "data", "fides.db"),
                Paths.get("data", "fides.db")
        };
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        // Default: create under module folder when possible
        Path nested = Paths.get("user-registration-system", "user_registration_system.db");
        if (Files.isDirectory(Paths.get("user-registration-system"))) {
            return nested;
        }
        return Paths.get("user_registration_system.db");
    }
}
