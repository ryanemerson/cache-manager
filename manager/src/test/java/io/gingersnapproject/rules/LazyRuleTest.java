package io.gingersnapproject.rules;

import io.gingersnapproject.database.DatabaseResourcesLifecyleManager;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@QuarkusTestResource(value = DatabaseResourcesLifecyleManager.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LazyRuleTest {
    @Test
    public void lazyLoad() {
        // No values have been added via HotRod, so the DB will be queried
        given().when()
                .get("/rules/lazy-flight/1")
                .then()
                .body("name", equalTo("BA1234"));

        given().when()
                .get("/rules/lazy-flight/3")
                .then()
                .statusCode(404);
    }
}
