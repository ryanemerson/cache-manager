package io.gingersnap.configuration;

import java.util.List;
import java.util.Optional;

import io.quarkus.kubernetes.service.binding.runtime.DatasourceServiceBindingConfigSourceFactory;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;

public class ServiceBindingConverter implements io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter {

   @Override
   public Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings) {
      return ServiceBinding.singleMatchingByType("mysql", serviceBindings)
            .map(new DatasourceServiceBindingConfigSourceFactory.Reactive());
   }
}
