package ru.craftysoft.platform.gateway.service.client.grpc;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.CallOptions;
import ru.craftysoft.platform.gateway.builder.DynamicMessageBuilder;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@ApplicationScoped
public class DynamicGrpcClientAdapter {

    private final Map<String, DynamicGrpcClient> dynamicGrpcClients;
    private final DynamicMessageBuilder dynamicMessageBuilder;

    public DynamicGrpcClientAdapter(Map<String, DynamicGrpcClient> dynamicGrpcClients,
                                    DynamicMessageBuilder dynamicMessageBuilder) {
        this.dynamicGrpcClients = dynamicGrpcClients;
        this.dynamicMessageBuilder = dynamicMessageBuilder;
    }

    public CompletableFuture<Message> processRequest(DataFetchingEnvironment environment,
                                                     DescriptorProtos.FileDescriptorSet descriptorSet,
                                                     String serviceName,
                                                     String serverName) {
        var fileDescriptorsWithDependencies = descriptorSet.getFileList().stream()
                .collect(Collectors.toMap(f -> f, f -> (Set<String>) new HashSet<>(f.getDependencyList())));
        var wrapper = new FileDescriptorHolder();
        resolveFullFileDescriptor(fileDescriptorsWithDependencies, new HashMap<>(), wrapper);
        var fileDescriptor = wrapper.fileDescriptor;
        var method = resolveMethod(environment, fileDescriptor, serviceName);
        var inputTypeDescriptor = resolveDescriptor(method.getInputType(), fileDescriptor);
        var outputTypeDescriptor = resolveDescriptor(method.getOutputType(), fileDescriptor);
        var builder = DynamicMessage.newBuilder(inputTypeDescriptor);
        var message = (DynamicMessage) dynamicMessageBuilder.build(inputTypeDescriptor, builder, environment.getArgument("request"));
        var dynamicGrpcClient = dynamicGrpcClients.get(serverName);
        return dynamicGrpcClient.callUnary(message, CallOptions.DEFAULT, serviceName, method.getName(), inputTypeDescriptor, outputTypeDescriptor);
    }

    private Descriptors.Descriptor resolveDescriptor(Descriptors.Descriptor targetDescriptor, Descriptors.FileDescriptor fileDescriptor) {
        var inputTypeNameParts = targetDescriptor.getName().split("\\.");
        var inputTypeName = inputTypeNameParts[inputTypeNameParts.length - 1];
        return fileDescriptor.findMessageTypeByName(inputTypeName);
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

    private Descriptors.MethodDescriptor resolveMethod(DataFetchingEnvironment environment, Descriptors.FileDescriptor fileDescriptor, String serviceName) {
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

    private static class FileDescriptorHolder {
        private Descriptors.FileDescriptor fileDescriptor;
    }
}
