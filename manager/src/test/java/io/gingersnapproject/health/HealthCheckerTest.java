package io.gingersnapproject.health;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public abstract class HealthCheckerTest {
   @Test
   public void testHealthEndpointDefined() {
      given()
            .when().get("/q/health/ready")
            .then()
            .statusCode(200);
   }
}
