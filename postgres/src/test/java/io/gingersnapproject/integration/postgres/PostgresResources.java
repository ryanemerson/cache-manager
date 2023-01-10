package io.gingersnapproject.integration.postgres;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.util.HashMap;
import java.util.Map;

public class PostgresResources implements QuarkusTestResourceLifecycleManager {

    public static final String RULE_NAME = "us-east";
    private static final String IMAGE = "postgres:latest";

//    private static final String HOST_TMP = Path.of(System.getProperty("java.io.tmpdir"), "postgres_cache_manager").toString();
//    private static final String CONTAINER_DATA_DIR = "/var/lib/mysql";

    private PostgreSQLContainer<?> db;

    @Override
    public Map<String, String> start() {
        db = new PostgreSQLContainer<>(IMAGE)
                .withDatabaseName("debeziumdb")
                .withUsername("gingersnap_user")
                .withPassword("password")
                .withExposedPorts(PostgreSQLContainer.POSTGRESQL_PORT)
//                .withFileSystemBind(HOST_TMP, CONTAINER_DATA_DIR, BindMode.READ_WRITE)
                .withCopyFileToContainer(MountableFile.forClasspathResource("setup.sql"), "/docker-entrypoint-initdb.d/setup.sql");
        try {
            db.start();
        } catch (Throwable t) {
            System.out.println(db.getLogs());
        }

        Map<String, String> properties = new HashMap<>( Map.of(
                "quarkus.datasource.username", db.getUsername(),
                "quarkus.datasource.password", db.getPassword(),
                "quarkus.datasource.reactive.url", String.format("postgresql://%s:%d/debeziumdb", db.getHost(), db.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)),
                String.format("gingersnap.rule.%s.key-type", RULE_NAME), "PLAIN",
                String.format("gingersnap.rule.%s.plain-separator", RULE_NAME), ":",
                String.format("gingersnap.rule.%s.select-statement", RULE_NAME), "select fullname, email from customer where id = ?",
                String.format("gingersnap.rule.%s.connector.schema", RULE_NAME), "debezium",
                String.format("gingersnap.rule.%s.connector.table", RULE_NAME), "customer"));

        for (int i = 1; i < 5; ++i) {
            properties.putAll(Map.of(
                    "gingersnap.rule.developers-" + i + ".key-type", "PLAIN",
                    "gingersnap.rule.developers-" + i + ".plain-separator", ":",
                    "gingersnap.rule.developers-" + i + ".select-statement", "select fullname, email from customer where id = ?",
                    "gingersnap.rule.developers-" + i + ".query-enabled", "true",
                    "gingersnap.rule.developers-" + i + ".connector.schema", "debezium",
                    "gingersnap.rule.developers-" + i + ".connector.table", "developers-" + i));
        }

        return properties;
    }

    @Override
    public void stop() {
        if (db != null)
            db.stop();
    }
}
