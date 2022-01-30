package ru.craftysoft.platform.gateway.builder.dynamic;

import com.google.protobuf.Descriptors;
import graphql.schema.DataFetchingEnvironment;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;

@ApplicationScoped
public class MethodDescriptorResolver {

    public Descriptors.MethodDescriptor resolve(DataFetchingEnvironment environment, Descriptors.FileDescriptor fileDescriptor, String serviceName) {
        return fileDescriptor.getServices().stream()
                .filter(service -> serviceName.equals(service.getFullName()))
                .findFirst()
                .map(Descriptors.ServiceDescriptor::getMethods)
                .stream()
                .flatMap(Collection::stream)
                .filter(m -> environment.getFieldDefinition().getName().equals(m.getName()))
                .findFirst()
                .orElseThrow();
    }

}
