package io.gingersnapproject;

import io.gingersnapproject.configuration.RuleManager;
import io.gingersnapproject.mutiny.UniItem;
import io.gingersnapproject.search.QueryHandler;
import io.gingersnapproject.search.QueryResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/rules")
public class RuleResource {

   private static final NotFoundException NOT_FOUND_EXCEPTION = new NotFoundException();

   @Inject
   Caches maps;

   @Inject
   QueryHandler queryHandler;

   @Inject
   RuleManager ruleManager;

   @GET
   @Operation(summary = "Retrieve a cache entry associated with the provided rule and key")
   @Path("/{rule}/{key}")
   @Produces(MediaType.APPLICATION_JSON)
   public Uni<String> get(String rule, String key) {
      checkRuleExists(rule);
      Uni<String> uni = maps.get(rule, key);
      if (uni instanceof UniItem<?> uniItem)  {
         if (uniItem.getItem() == null)
            throw NOT_FOUND_EXCEPTION;
         return uni;
      }
      return uni.onItem()
              .invoke(v -> {
                 if (v == null)
                    throw NOT_FOUND_EXCEPTION;
              });
   }

   @GET
   @Operation(summary = "Retrieve all cached keys associated with the provided rule")
   @Path("/{rule}")
   @Produces(MediaType.APPLICATION_JSON)
   public Multi<String> getAllKeys(String rule) {
      checkRuleExists(rule);
      return Multi.createFrom().items(maps.getKeys(rule))
            // TODO: should be able to do this in a better way - technically we also need to escape the String as well
            .map(a -> "\"" + a + "\"");
   }

   @GET
   @Operation(summary = "Queries from any of the rules that are indexed")
   @Produces(MediaType.APPLICATION_JSON)
   public Uni<QueryResult> query(@QueryParam("query") String query) {
      return queryHandler.query(query);
   }

   private void checkRuleExists(String rule) {
      if (ruleManager.getEagerOrLazyRuleByName(rule) == null)
         throw NOT_FOUND_EXCEPTION;
   }
}
