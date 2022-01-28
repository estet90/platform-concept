package ru.craftysoft.platform.gateway.builder;

import com.google.common.base.CharMatcher;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.GenericDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import ru.craftysoft.proto.*;

import javax.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.protobuf.NullValue.NULL_VALUE;
import static java.util.Optional.ofNullable;

@ApplicationScoped
public final class DynamicMessageBuilder {

    public Message build(Descriptor descriptor, Message.Builder builder, Map<String, Object> input) {
        if (input == null) {
            return builder.build();
        }
        var fileDescriptorSet = extractDependencies(descriptor.getFile());

        var loop = new LinkedList<Descriptor>();
        var enumMapping = new HashMap<String, EnumDescriptor>();
        for (var fileDescriptor : fileDescriptorSet) {
            loop.addAll(fileDescriptor.getMessageTypes());
            enumMapping.putAll(getEnumMap(fileDescriptor.getEnumTypes()));
        }

        var descriptorMapping = new HashMap<String, Descriptor>();
        while (!loop.isEmpty()) {
            var innerDescriptor = loop.pop();
            if (!descriptorMapping.containsKey(innerDescriptor.getFullName())) {
                descriptorMapping.put(getInputReferenceName(innerDescriptor), innerDescriptor);
                loop.addAll(innerDescriptor.getNestedTypes());
                enumMapping.putAll(getEnumMap(innerDescriptor.getEnumTypes()));
            }
        }
        var remainingInput = new HashMap<>(input);
        for (var field : descriptor.getFields()) {
            var fieldName = field.getName();
            if (!remainingInput.containsKey(fieldName)) {
                continue;
            }
            if (field.isRepeated()) {
                var values = (List<Object>) remainingInput.remove(fieldName);
                for (var value : values) {
                    var valueForField = getValueForField(descriptorMapping, enumMapping, field, value, builder);
                    builder.addRepeatedField(field, valueForField);
                }
            } else {
                var valueForField = getValueForField(descriptorMapping, enumMapping, field, remainingInput.remove(fieldName), builder);
                builder.setField(field, valueForField);
            }
        }
        if (!remainingInput.isEmpty()) {
            throw new AssertionError("All fields in input should have been consumed. Remaining: " + remainingInput);
        }
        return builder.build();
    }

    private Object getValueForField(Map<String, Descriptor> descriptorMapping,
                                    Map<String, EnumDescriptor> enumMapping,
                                    FieldDescriptor field,
                                    Object value,
                                    Message.Builder builder) {
        switch (field.getType()) {
            case MESSAGE -> {
                var fieldTypeDescriptor = descriptorMapping.get(getInputReferenceName(field.getMessageType()));
                switch (fieldTypeDescriptor.getFullName()) {
                    case "google.protobuf.Timestamp" -> {
                        return ofNullable(value)
                                .map(OffsetDateTime.class::cast)
                                .map(dateTime -> Timestamp.newBuilder()
                                        .setSeconds(dateTime.toEpochSecond())
                                        .setNanos(dateTime.getNano()).build())
                                .orElseGet(Timestamp.newBuilder()::getDefaultInstanceForType);
                    }
                    case "ru.craftysoft.proto.NullableString" -> {
                        return ofNullable(value)
                                .map(String.class::cast)
                                .map(string -> NullableString.newBuilder().setValue(string).build())
                                .orElseGet(() -> NullableString.newBuilder().setNullValue(NULL_VALUE).build());
                    }
                    case "ru.craftysoft.proto.NullableDouble" -> {
                        return ofNullable(value)
                                .map(Double.class::cast)
                                .map(string -> NullableDouble.newBuilder().setValue(string).build())
                                .orElseGet(() -> NullableDouble.newBuilder().setNullValue(NULL_VALUE).build());
                    }
                    case "ru.craftysoft.proto.NullableFloat" -> {
                        return ofNullable(value)
                                .map(Float.class::cast)
                                .map(string -> NullableFloat.newBuilder().setValue(string).build())
                                .orElseGet(() -> NullableFloat.newBuilder().setNullValue(NULL_VALUE).build());
                    }
                    case "ru.craftysoft.proto.NullableInt64" -> {
                        return ofNullable(value)
                                .map(Long.class::cast)
                                .map(string -> NullableInt64.newBuilder().setValue(string).build())
                                .orElseGet(() -> NullableInt64.newBuilder().setNullValue(NULL_VALUE).build());
                    }
                    case "ru.craftysoft.proto.NullableUInt64" -> {
                        return ofNullable(value)
                                .map(Long.class::cast)
                                .map(string -> NullableUInt64.newBuilder().setValue(string).build())
                                .orElseGet(() -> NullableUInt64.newBuilder().setNullValue(NULL_VALUE).build());
                    }
                    case "ru.craftysoft.proto.NullableInt32" -> {
                        return ofNullable(value)
                                .map(Integer.class::cast)
                                .map(string -> NullableInt32.newBuilder().setValue(string).build())
                                .orElseGet(() -> NullableInt32.newBuilder().setNullValue(NULL_VALUE).build());
                    }
                    case "ru.craftysoft.proto.NullableUInt32" -> {
                        return ofNullable(value)
                                .map(Integer.class::cast)
                                .map(string -> NullableUInt32.newBuilder().setValue(string).build())
                                .orElseGet(() -> NullableUInt32.newBuilder().setNullValue(NULL_VALUE).build());
                    }
                    case "ru.craftysoft.proto.NullableBool" -> {
                        return ofNullable(value)
                                .map(Boolean.class::cast)
                                .map(string -> NullableBool.newBuilder().setValue(string).build())
                                .orElseGet(() -> NullableBool.newBuilder().setNullValue(NULL_VALUE).build());
                    }
                    case "ru.craftysoft.proto.NullableBytes" -> {
                        return ofNullable(value)
                                .map(byte[].class::cast)
                                .map(bytes -> NullableBytes.newBuilder().setValue(ByteString.copyFrom(bytes)).build())
                                .orElseGet(() -> NullableBytes.newBuilder().setNullValue(NULL_VALUE).build());
                    }
                }
                return build(fieldTypeDescriptor, builder.newBuilderForField(field), (Map<String, Object>) value);
            }
            case ENUM -> {
                var enumDescriptor = enumMapping.get(getReferenceName(field.getEnumType()));
                return enumDescriptor.findValueByName(value.toString());
            }
            case FLOAT -> {
                if (value instanceof Double) {
                    return ((Double) value).floatValue();
                }
                return value;
            }
            default -> {
                return value;
            }
        }
    }

    private Set<Descriptors.FileDescriptor> extractDependencies(Descriptors.FileDescriptor fileDescriptor) {
        var loop = new LinkedList<Descriptors.FileDescriptor>();
        loop.add(fileDescriptor);
        var fileDescriptorSet = new HashSet<Descriptors.FileDescriptor>();
        fileDescriptorSet.add(fileDescriptor);
        while (!loop.isEmpty()) {
            var fd = loop.pop();
            for (var dependency : fd.getDependencies()) {
                if (!fileDescriptorSet.contains(dependency)) {
                    fileDescriptorSet.add(dependency);
                    loop.push(dependency);
                }
            }
        }
        return fileDescriptorSet;
    }

    private String getInputReferenceName(GenericDescriptor descriptor) {
        return "Input_" + getReferenceName(descriptor);
    }

    private Map<String, EnumDescriptor> getEnumMap(Collection<EnumDescriptor> enumDescriptors) {
        return enumDescriptors.stream()
                .collect(Collectors.toMap(this::getReferenceName, e -> e));
    }

    private String getReferenceName(GenericDescriptor descriptor) {
        return CharMatcher.anyOf(".").replaceFrom(descriptor.getFullName(), "_");
    }
}
