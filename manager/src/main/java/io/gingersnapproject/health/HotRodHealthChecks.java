package io.gingersnapproject.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;

import io.gingersnapproject.HotRodServer;
import io.quarkus.arc.Arc;

import javax.enterprise.context.ApplicationScoped;

public class HotRodHealthChecks {

   private static final String SERVER_CHECK_NAME = "HotRod Server";

   @Liveness
   @ApplicationScoped
   public static class LivenessCheck implements HealthCheck {
      @Override
      public HealthCheckResponse call() {
         HotRodServer server = Arc.container().instance(HotRodServer.class).get();
         return HealthCheckResponse.named(SERVER_CHECK_NAME)
               .status(server.isLive())
               .build();
      }
   }

   @Readiness
   @ApplicationScoped
   public static class ReadinessCheck implements HealthCheck {
      @Override
      public HealthCheckResponse call() {
         HotRodServer server = Arc.container().instance(HotRodServer.class).get();
         return HealthCheckResponse.named(SERVER_CHECK_NAME)
               .status(server.isReady())
               .build();
      }
   }

   @Startup
   @ApplicationScoped
   public static class StartupCheck implements HealthCheck {
      @Override
      public HealthCheckResponse call() {
         HotRodServer server = Arc.container().instance(HotRodServer.class).get();
         return HealthCheckResponse.named(SERVER_CHECK_NAME)
               .status(server.hasStarted())
               .build();
      }
   }
}
