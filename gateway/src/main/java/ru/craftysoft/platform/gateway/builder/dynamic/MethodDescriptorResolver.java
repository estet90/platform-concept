package ru.craftysoft.platform.gateway.builder.dynamic;

import com.google.protobuf.Descriptors;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;

@ApplicationScoped
public class MethodDescriptorResolver {

    public Descriptors.MethodDescriptor resolve(String methodName, Descriptors.FileDescriptor fileDescriptor, String serviceName) {
        return fileDescriptor.getServices().stream()
                .filter(service -> serviceName.equals(service.getFullName()))
                .findFirst()
                .map(Descriptors.ServiceDescriptor::getMethods)
                .stream()
                .flatMap(Collection::stream)
                .filter(m -> methodName.equals(m.getName()))
                .findFirst()
                .orElseThrow();
    }

}
