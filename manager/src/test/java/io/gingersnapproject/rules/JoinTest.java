package io.gingersnapproject.rules;

import io.gingersnapproject.database.DatabaseResourcesLifecyleManager;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.dataconversion.MediaType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
@QuarkusTestResource(value = DatabaseResourcesLifecyleManager.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JoinTest {
   private static final String RULE_NAME = "flight";
   private static final String RULE_PATH = "/rules";
   private static final String GET_PATH = RULE_PATH + "/{rule}/{key}";

   private RemoteCacheManager cm;
   private RemoteCache<String, String> cache;

   @BeforeAll
   public void beforeAll() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.addServer()
            .host("127.0.0.1")
            .port(11222);

      cm = new RemoteCacheManager(builder.build());
      cache = cm.getCache(RULE_NAME)
            .withDataFormat(DataFormat.builder().keyType(MediaType.TEXT_PLAIN).valueType(MediaType.TEXT_PLAIN).build());
   }

   @AfterAll
   public void afterAll() {
      if (cm != null) {
         cm.stop();
      }

      cm = null;
      cache = null;
   }

   @Test
   public void testJoin() throws Exception {
      cache.put("3", "{\"name\":\"BA0666\", \"scheduled_time\":\"12:00:00\", \"airline_id\": \"1\", \"gate_id\":\"3\"}");
      given().when().get(GET_PATH, "flight", "3").then().body(containsString("British Airways")).body(containsString("B1"));

      Thread.sleep(1000); // we need to wait for the ES near real time behavior

      given().queryParam("query", "select * from flight where name = 'BA0666' order by scheduled_time")
            .when()
              .get(RULE_PATH)
            .then()
              .body("hitCount", equalTo(1))
              .body("hitCountExact", equalTo(true))
              .body("hits[0].name", equalTo("BA0666"))
              .body("hits[0].airline_id.name", equalTo("British Airways"))
              .body("hits[0].gate_id.name", equalTo("B1"))
              .body("hitsExacts", equalTo(true));
   }

   @Test
   public void testMultipleForeignKeys() {
      cache.put("3", "{\"name\":\"BA0666\", \"scheduled_time\":\"12:00:00\", \"airline_id\": \"1\", \"gate_id\":\"3\", \"origin_id\":\"1\", \"destination_id\":\"2\"}");
      given()
              .when()
              .get(GET_PATH, "flight", "3")
              .then()
              .body("airline_id.name", equalTo("British Airways"))
              .body("origin_id.name", equalTo("Newcastle"))
              .body("destination_id.name", equalTo("Knock"));
   }

   @Test
   public void testValueWithNoForeignKey() {
      cache.put("3", "{\"name\":\"BA0666\", \"scheduled_time\":\"12:00:00\"}");
      given().when().get(GET_PATH, "flight", "3").then().body(not(containsString("British Airways")));
   }
}
