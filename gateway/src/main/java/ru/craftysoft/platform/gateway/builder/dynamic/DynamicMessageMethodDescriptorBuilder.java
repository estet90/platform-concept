package ru.craftysoft.platform.gateway.builder.dynamic;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ExtensionRegistryLite;
import io.grpc.MethodDescriptor;
import lombok.SneakyThrows;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;

import static io.grpc.MethodDescriptor.MethodType.UNARY;

@ApplicationScoped
public class DynamicMessageMethodDescriptorBuilder {

    public MethodDescriptor<DynamicMessage, DynamicMessage> build(String serviceName,
                                                                  String methodName,
                                                                  Descriptors.Descriptor inputType,
                                                                  Descriptors.Descriptor outputType) {
        var fullMethodName = MethodDescriptor.generateFullMethodName(serviceName, methodName);
        return MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                .setType(UNARY)
                .setFullMethodName(fullMethodName)
                .setRequestMarshaller(new DynamicMessageMarshaller(inputType))
                .setResponseMarshaller(new DynamicMessageMarshaller(outputType))
                .build();
    }

    private static class DynamicMessageMarshaller implements MethodDescriptor.Marshaller<DynamicMessage> {
        private final Descriptors.Descriptor messageDescriptor;

        public DynamicMessageMarshaller(Descriptors.Descriptor messageDescriptor) {
            this.messageDescriptor = messageDescriptor;
        }

        @Override
        @SneakyThrows
        public DynamicMessage parse(InputStream inputStream) {
            return DynamicMessage.newBuilder(messageDescriptor)
                    .mergeFrom(inputStream, ExtensionRegistryLite.getEmptyRegistry())
                    .build();
        }

        @Override
        public InputStream stream(DynamicMessage abstractMessage) {
            return abstractMessage.toByteString().newInput();
        }
    }

}
