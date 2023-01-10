package io.gingersnapproject.health;

import io.gingersnapproject.mysql.MySQLResources;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(MySQLResources.class)
public class HealthCheckerTest {
   @Test
   public void testHealthEndpointDefined() {
      given()
            .when().get("/q/health/ready")
            .then()
            .statusCode(200);
   }
}
