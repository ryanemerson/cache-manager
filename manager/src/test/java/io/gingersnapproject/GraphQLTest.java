package io.gingersnapproject;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@QuarkusTest
public class GraphQLTest {

    @Inject
    Graph graphql;

    @Test
    public void testValueWithNoForeignKey() {
        graphql.exec("{hello}");
//        given().when().body("{hello}").post("/rules/graphql").then().body(not(containsString("British Airways")));
    }
}
