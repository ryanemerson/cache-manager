package io.gingersnapproject.rest;

import io.gingersnapproject.database.DatabaseResourcesLifecyleManager;
import io.gingersnapproject.profile.GingersnapIntegrationTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusIntegrationTest
@GingersnapIntegrationTest
@QuarkusTestResource(DatabaseResourcesLifecyleManager.class)
public class RuleResourcesTest {

   @Test
   public void testRetrieveRuleKeys() {
      given()
            .when().get("/rules/airline")
            .then()
            .statusCode(200)
            .body("", hasSize(2));
   }

   @Test
   public void testRetrieveRuleKeysNotFound() {
      given()
              .when().get("/rules/none")
              .then()
              .statusCode(404);
   }

   @Test
   public void testEntryFound() {
      given()
              .when().get("/rules/airline/1")
              .then()
              .statusCode(200)
              .body("iata", equalTo("BA"))
              .body("name", equalTo("British Airways"));
   }

   @Test
   public void testEntryNotFound() {
      given()
              .when().get("/rules/airline/3")
              .then()
              .statusCode(404);
   }
}
