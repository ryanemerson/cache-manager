package io.gingersnapproject.health;

import io.gingersnapproject.k8s.configuration.KubernetesConfiguration;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;

import javax.inject.Inject;
import javax.inject.Singleton;

public class ServiceBindingHealthChecks {

    private static final String SERVER_CHECK_NAME = "ServiceBinding";

    @Readiness
    @Startup
    @Singleton
    public static class RootDefined implements HealthCheck {

        // Field can't be static, otherwise it is set at native build time
        private final boolean SERVICE_BINDING_ROOT_DEFINED = System.getenv("SERVICE_BINDING_ROOT") != null;

        @Inject
        KubernetesConfiguration config;

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.builder()
                    .name(SERVER_CHECK_NAME)
                    .status(!config.serviceBindingRequired() || SERVICE_BINDING_ROOT_DEFINED)
                    .build();
        }
    }
}
