package io.gingersnapproject.postgres;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = MySQLResources.class)
public class HotRodOperationsTest extends io.gingersnapproject.hotrod.HotRodOperationsTest {
}
