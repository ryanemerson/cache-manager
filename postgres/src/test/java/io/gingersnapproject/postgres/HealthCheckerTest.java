package io.gingersnapproject.postgres;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(PostgresResources.class)
public class HealthCheckerTest extends io.gingersnapproject.health.HealthCheckerTest {
}