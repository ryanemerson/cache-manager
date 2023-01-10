package io.gingersnapproject;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class PostgresProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.datasource.db-kind", "postgresql");
    }
}
