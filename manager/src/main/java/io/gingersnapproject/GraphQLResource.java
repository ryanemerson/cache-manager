package io.gingersnapproject;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

// TODO https://graphql.org/learn/serving-over-http/
@Path("/graphql")
public class GraphQLResource {

    @Inject
    Graph graph;

    @GET
    @Path("schema.graphql")
    public String get() {
        return graph.exec("{__schema{queryType{fields{name}}}}");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@QueryParam("query") String query) {
        // TODO UNI
        return graph.exec(query);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String post(String payload) {
        // TODO UNI
        // TODO https://graphql.org/learn/serving-over-http/
        return graph.exec(payload);
    }
}
