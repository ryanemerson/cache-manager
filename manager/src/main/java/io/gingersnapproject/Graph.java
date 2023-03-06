package io.gingersnapproject;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.gingersnapproject.configuration.EagerRule;
import io.gingersnapproject.configuration.RuleManager;
import io.gingersnapproject.database.DatabaseHandler;
import io.gingersnapproject.database.model.Column;
import io.quarkus.runtime.StartupEvent;
import org.infinispan.commons.dataconversion.internal.Json;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static graphql.Scalars.*;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

@ApplicationScoped
public class Graph {

    @Inject
    DatabaseHandler databaseHandler;

    @Inject
    RuleManager ruleManager;

    @Inject
    Caches caches;

    void start(@Observes StartupEvent ignore) {
    }

    private GraphQL generate() {
        // Need to register all types with !FK first
        // Add these types to a map with the rule name as the key and the rule definition as the value
        // Loop over rules with FK
        // If FK(s) exist, but no Rule exists for one of the Types, continue
        // If FK exists and Rule exists for type, define
        // Use while loop to implement

        // Process all rules without expansion or ForeignKeys
        Map<String, GraphQLObjectType> typeMap = ruleManager.eagerRules()
                .entrySet()
                .stream()
                .filter(e -> {
                    var table = databaseHandler.table(e.getValue().connector().table());
                    return !e.getValue().expandEntity() || table.foreignKeys().isEmpty();
                })
                .map(e -> {
                    var table = databaseHandler.table(e.getValue().connector().table());
                    var builder = GraphQLObjectType.newObject().name(e.getKey());
                    table.columns().forEach(column -> builder.field(fieldDef(column)));
                    return builder.build();
                })
                .collect(Collectors.toMap(GraphQLObjectType::getName, Function.identity()));

        Map<String, EagerRule> fkRules =  ruleManager.eagerRules()
                .entrySet()
                .stream()
                .filter(e -> !typeMap.containsKey(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        while (!fkRules.isEmpty()) {
            var it = fkRules.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                var ruleName = e.getKey();
                var rule = e.getValue();
                var table = databaseHandler.table(rule.connector().table());
                boolean fkTypeDefined = true;
                for (var fk : table.foreignKeys()) {
                    var tableRule = databaseHandler.tableToRuleName(fk.refTable());
                    if (!typeMap.containsKey(tableRule)) {
                        fkTypeDefined = false;
                        break;
                    }
                }

                // All FK rules defined, so we can define a Type for this rule
                if (fkTypeDefined) {
                    var builder = GraphQLObjectType.newObject().name(ruleName);
                    var fkColumns = table.foreignKeys()
                            .stream()
                            .flatMap(fk -> fk.columns().stream())
                            .collect(Collectors.toSet());

                    // Define non-FK fields
                    table.columns()
                            .stream()
                            .filter(c -> !fkColumns.contains(c))
                            .forEach(column ->  builder.field(fieldDef(column)));

                    // Define FK fields
                    table.foreignKeys()
                            .forEach(fk -> {
                                var refRule = databaseHandler.tableToRuleName(fk.refTable());
                                var refType = typeMap.get(refRule);
                                var fkCol = fk.columns().get(0).name();
                                builder.field(
                                        newFieldDefinition()
                                                .name(fkCol)
                                                .type(refType)
                                );
                            });
                    typeMap.put(ruleName, builder.build());
                    it.remove();
                }
            }
        }

        var schemaBuilder = new GraphQLSchema.Builder();
        var queryTypeBuilder = GraphQLObjectType.newObject().name("QueryType");
        // Define QueryType function for rule and register types
        typeMap.values().forEach(t -> {
            schemaBuilder.additionalType(t);

            var name = t.getName();
            var rule = ruleManager.eagerRules().get(name);
            var table = databaseHandler.table(rule.connector().table());

            // TODO store as Map in Table?
            var columnMap = table.columns().stream().collect(Collectors.toMap(Column::name, Function.identity()));
            var keyColumns = rule.connector()
                    .keyColumns()
                    .stream()
                    .map(columnMap::get)
                    .toList();

            var fieldBuilder = newFieldDefinition()
                    .name(name)
                    .type(t);

            keyColumns.forEach(c ->
                    fieldBuilder.argument(
                            GraphQLArgument.newArgument()
                                    .name(c.name())
                                    .type(nonNull(GraphQLString))
                                    .build()
                    )
            );

            fieldBuilder.dataFetcher(environment -> {
                // TODO handle JSON keys
                var keyBuilder = new StringBuilder();
                for (int i = 0; i < keyColumns.size(); i++) {
                    var argName = keyColumns.get(i).name();
                    var arg = environment.getArgument(argName);
                    keyBuilder.append(arg);
                    if (i != keyColumns.size() - 1)
                        keyBuilder.append(rule.plainSeparator());
                }

                var key = keyBuilder.toString();
                var entry = caches.get(name, key)
                        .await()
                        .atMost(Duration.ofSeconds(1));

                // TODO correctly handle so that exception is returned via "errors"
                if (entry == null)
                    throw new Exception(String.format("Entry '%s' could not be found", key));

                // TODO avoid back and forth between JSON -> MAP
                return Json.read(entry).asMap();
            });
            queryTypeBuilder.field(fieldBuilder);
        });

        schemaBuilder.query(queryTypeBuilder);
        var schema = schemaBuilder.build();
        return GraphQL.newGraphQL(schema).build();
    }

    private GraphQLFieldDefinition fieldDef(Column c) {
        return newFieldDefinition()
                .name(c.name())
                .type(graphQLType(c))
                .build();
    }

    private GraphQLOutputType graphQLType(Column c) {
        return switch (c.type()) {
            case "BOOL" -> GraphQLBoolean;
            case "INT" -> GraphQLInt;
            default -> GraphQLString;
        };
    }

    // `curl -X POST --data '{hello(name: "Ryan", email: "test@test.com"){name email}}' "http://localhost:8080/graphql"`
    private GraphQL programmatic() {
        var schemaBuilder = new GraphQLSchema.Builder();
        var userType = GraphQLObjectType.newObject()
                .name("User")
                .field(
                        newFieldDefinition()
                                .name("name")
                                .type(GraphQLString)
                                .build()
                )
                .field(
                        newFieldDefinition()
                                .name("email")
                                .type(GraphQLString)
                                .build()
                )
                .build();

        var queryType = GraphQLObjectType.newObject()
                .name("QueryType")
                .field(newFieldDefinition()
                        .name("hello")
                        .argument(
                                GraphQLArgument.newArgument()
                                        .name("name")
                                        .type(nonNull(GraphQLString))
                                        .build()
                        )
                        .argument(
                                GraphQLArgument.newArgument()
                                        .name("email")
                                        .type(GraphQLString)
                                        .build()
                        )
                        .type(userType)
                        .dataFetcher(environment -> {
                            var map = new HashMap<>(2);
                            map.put("name", environment.getArgument("name"));
                            map.put("email", environment.getArgument("email"));
                            return map;
                        })
                        .build()
                );
        schemaBuilder.additionalType(userType);
        schemaBuilder.query(queryType);

        var schema = schemaBuilder.build();
        return GraphQL.newGraphQL(schema).build();
    }

    // `curl -X POST --data '{hello(name: "Ryan", email: "test@test.com"){name email}}' "http://localhost:8080/graphql"`
    private GraphQL declarative() {
        String schema = """
                type Query {
                  hello(name: String!, email: String): User
                }
                                
                type User {
                  name: String
                  email: String
                }
                    """;

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        RuntimeWiring runtimeWiring = newRuntimeWiring()
                .type("Query",
                        builder -> builder
                                .dataFetcher("hello", environment -> {
                                    environment.getArguments().forEach((k, v) -> System.out.printf("K=%s,V=%s", k, v));
                                    var map = new HashMap<>(2);
                                    map.put("name", environment.getArgument("name"));
                                    map.put("email", environment.getArgument("email"));
                                    return map;
                                }))
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        return GraphQL.newGraphQL(graphQLSchema).build();
    }

    public String exec(String query) {
        // Ordering, Pagination etc all done by passing args to the function https://graphql.org/learn/pagination/
        //
        // TODO generate following query methods:
        // 1. rule(key: String!) rule
        // 2. rules() [RuleConfig]
        // rule(co1, col2, col3...) SQL query on Index to return all that match non-null args?

        // Return the GraphQL schemas via the endpoint `curl -X POST --data '{__schema{queryType{fields{name}}}}' "http://localhost:8080/rules"`
        // https://medium.com/@mrthankyou/how-to-get-a-graphql-schema-28915025de0e

        GraphQL graphQL = generate();
        ExecutionResult executionResult = graphQL.execute(query);
        for (var e : executionResult.getErrors())
            System.err.println(e);

        var data = executionResult.getData();

        // Create JSON
        Json json = Json.object();
        json.set("data", Json.make(data));
        // TODO include errors
        json.set("errors", Json.nil());
        return json.toPrettyString();
    }
}
