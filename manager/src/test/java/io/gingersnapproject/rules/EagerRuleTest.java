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
public class EagerRuleTest {
    @Test
    public void lazyLoadOnMiss() {
        // No values have been added via HotRod, so the DB will be queried
        given().when()
                .get("/rules/airport/1")
                .then()
                .body("name", equalTo("Newcastle"));

        given().when()
                .get("/rules/airport/3")
                .then()
                .statusCode(404);
    }

    @Test
    public void testJoinOnLazyLoad() {
        given().when()
                .get("/rules/flight/1")
                .then()
                .body("airline_id.name", equalTo("British Airways"))
                .body("origin_id.name", equalTo("Newcastle"))
                .body("destination_id.name", equalTo("Knock"));
    }
}
