package ru.craftysoft.platform.gateway.builder.dynamic;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Empty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DescriptorResolver {

    public Descriptors.Descriptor resolve(Descriptors.Descriptor targetDescriptor, Descriptors.FileDescriptor fileDescriptor) {
        var inputTypeNameParts = targetDescriptor.getName().split("\\.");
        var inputTypeName = inputTypeNameParts[inputTypeNameParts.length - 1];
        if ("Empty".equals(inputTypeName)) {
            return Empty.getDescriptor();
        }
        return fileDescriptor.findMessageTypeByName(inputTypeName);
    }

}
