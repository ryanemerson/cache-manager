package io.gingersnapproject.configuration;
import io.smallrye.config.WithDefault;

// TODO expose Values? Check how Will handled this in his PR
public interface EagerRule  extends Rule {

      Connector connector();

      @WithDefault("false")
      boolean queryEnabled();

      @WithDefault("true")
      boolean expandEntity();

}
