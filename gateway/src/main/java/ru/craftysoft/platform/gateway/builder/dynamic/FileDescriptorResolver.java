package ru.craftysoft.platform.gateway.builder.dynamic;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class FileDescriptorResolver {

    public Descriptors.FileDescriptor resolve(ServerReflectionResponse serverReflectionResponse) {
        var fileDescriptorsWithDependencies = resolveFileDescriptorsWithDependencies(serverReflectionResponse);
        var wrapper = new FileDescriptorHolder();
        resolveFullFileDescriptor(fileDescriptorsWithDependencies, new HashMap<>(), wrapper);
        return wrapper.fileDescriptor;
    }

    private Map<DescriptorProtos.FileDescriptorProto, Set<String>> resolveFileDescriptorsWithDependencies(ServerReflectionResponse serverReflectionResponse) {
        return serverReflectionResponse
                .getFileDescriptorResponse()
                .getFileDescriptorProtoList().stream()
                .map(fileDescriptorBytes -> {
                    try {
                        return DescriptorProtos.FileDescriptorProto.parseFrom(fileDescriptorBytes);
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toMap(f -> f, f -> new HashSet<>(f.getDependencyList())));
    }

    private void resolveFullFileDescriptor(Map<DescriptorProtos.FileDescriptorProto, Set<String>> fileDescriptorsWithDependencies,
                                           Map<String, Descriptors.FileDescriptor> completedFileDescriptors,
                                           FileDescriptorHolder result) {
        if (completedFileDescriptors.isEmpty()) {
            fileDescriptorsWithDependencies.entrySet().removeIf(entry -> {
                if (entry.getValue().isEmpty()) {
                    var descriptorProto = entry.getKey();
                    try {
                        var addedFileDescriptor = Descriptors.FileDescriptor.buildFrom(descriptorProto, new Descriptors.FileDescriptor[]{});
                        completedFileDescriptors.put(descriptorProto.getName(), addedFileDescriptor);
                        result.fileDescriptor = addedFileDescriptor;
                        return true;
                    } catch (Descriptors.DescriptorValidationException e) {
                        throw new RuntimeException(e);
                    }
                }
                return false;
            });
        }
        var completedFileDescriptorsNames = completedFileDescriptors.keySet();
        fileDescriptorsWithDependencies.entrySet().removeIf(entry -> {
            if (completedFileDescriptorsNames.containsAll(entry.getValue())) {
                var descriptorProto = entry.getKey();
                var currentDependencies = entry.getValue().stream()
                        .map(completedFileDescriptors::get)
                        .toArray(Descriptors.FileDescriptor[]::new);
                try {
                    var addedFileDescriptor = Descriptors.FileDescriptor.buildFrom(descriptorProto, currentDependencies);
                    completedFileDescriptors.put(descriptorProto.getName(), addedFileDescriptor);
                    result.fileDescriptor = addedFileDescriptor;
                    return true;
                } catch (Descriptors.DescriptorValidationException e) {
                    throw new RuntimeException(e);
                }
            }
            return false;
        });
        if (!fileDescriptorsWithDependencies.isEmpty()) {
            resolveFullFileDescriptor(
                    fileDescriptorsWithDependencies,
                    completedFileDescriptors,
                    result
            );
        }
    }

    private static class FileDescriptorHolder {
        private Descriptors.FileDescriptor fileDescriptor;
    }

}
