package io.gingersnapproject;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class MySQLProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.datasource.db-kind", "MYSQL");
    }
}
