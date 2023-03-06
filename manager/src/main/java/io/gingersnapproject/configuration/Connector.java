package io.gingersnapproject.configuration;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

import java.util.List;

@ConfigGroup
public interface Connector {

   String schema();

   String table();

   List<String> keyColumns();

   @WithDefault("*")
   List<String> valueColumns();
}
