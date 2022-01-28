// Copyright 2017 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ru.craftysoft.platform.gateway.rejoiner;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.*;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import graphql.schema.*;
import ru.craftysoft.proto.*;

import java.time.OffsetDateTime;
import java.util.*;

import static com.google.protobuf.NullValue.NULL_VALUE;
import static graphql.Scalars.GraphQLString;
import static java.util.Optional.ofNullable;

/**
 * Converts GraphQL inputs into Protobuf message.
 *
 * <p>Keeps a mapping from type name to Proto descriptor for Message and Enum types.
 */
public final class GqlInputConverter {

    private final BiMap<String, Descriptor> descriptorMapping;
    private final BiMap<String, EnumDescriptor> enumMapping;

    private static final Converter<String, String> UNDERSCORE_TO_CAMEL = CaseFormat.LOWER_UNDERSCORE.converterTo(CaseFormat.LOWER_CAMEL);

    private GqlInputConverter(BiMap<String, Descriptor> descriptorMapping, BiMap<String, EnumDescriptor> enumMapping) {
        this.descriptorMapping = descriptorMapping;
        this.enumMapping = enumMapping;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public Message createProtoBuf(Descriptor descriptor, Message.Builder builder, Map<String, Object> input) {
        if (input == null) {
            return builder.build();
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
                    var valueForField = getValueForField(field, value, builder);
                    builder.addRepeatedField(field, valueForField);
                }
            } else {
                var valueForField = getValueForField(field, remainingInput.remove(fieldName), builder);
                builder.setField(field, valueForField);
            }
        }
        if (!remainingInput.isEmpty()) {
            throw new AssertionError("All fields in input should have been consumed. Remaining: " + remainingInput);
        }
        return builder.build();
    }

    private Object getValueForField(FieldDescriptor field, Object value, Message.Builder builder) {
        switch (field.getType()) {
            case MESSAGE -> {
                var fieldTypeDescriptor = descriptorMapping.get(getReferenceName(field.getMessageType()));
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
                return createProtoBuf(fieldTypeDescriptor, builder.newBuilderForField(field), (Map<String, Object>) value);
            }
            case ENUM -> {
                var enumDescriptor = enumMapping.get(ProtoToGql.getReferenceName(field.getEnumType()));
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

    GraphQLType getInputType(Descriptor descriptor, SchemaOptions schemaOptions) {
        var builder = GraphQLInputObjectType.newInputObject().name(getReferenceName(descriptor));
        if (descriptor.getFields().isEmpty()) {
            builder.field(STATIC_FIELD);
        }
        for (var field : descriptor.getFields()) {
            GraphQLType fieldType = getFieldType(field, schemaOptions);
            GraphQLInputObjectField.Builder inputBuilder = GraphQLInputObjectField.newInputObjectField().name(getFieldName(field));
            if (field.isRepeated()) {
                inputBuilder.type(new GraphQLList(fieldType));
            } else {
                inputBuilder.type((GraphQLInputType) fieldType);
            }

            inputBuilder.description(schemaOptions.commentsMap().get(field.getFullName()));

            builder.field(inputBuilder.build());
        }
        builder.description(schemaOptions.commentsMap().get(descriptor.getFullName()));
        return builder.build();
    }

    static GraphQLArgument createArgument(Descriptor descriptor, String name) {
        return GraphQLArgument.newArgument().name(name).type(getInputTypeReference(descriptor)).build();
    }

    static GraphQLArgument createArgument(EnumDescriptor descriptor, String name) {
        return GraphQLArgument.newArgument()
                .name(name)
                .type(ProtoToGql.getReference(descriptor))
                .build();
    }

    static String getReferenceName(GenericDescriptor descriptor) {
        return "Input_" + ProtoToGql.getReferenceName(descriptor);
    }

    /**
     * Field names with under_scores are converted to camelCase.
     */
    private static String getFieldName(FieldDescriptor field) {
        var fieldName = field.getName();
        return fieldName.contains("_") ? UNDERSCORE_TO_CAMEL.convert(fieldName) : fieldName;
    }

    private static GraphQLType getFieldType(FieldDescriptor field, SchemaOptions schemaOptions) {
        if (field.getType() == FieldDescriptor.Type.MESSAGE
                || field.getType() == FieldDescriptor.Type.GROUP) {
            return new GraphQLTypeReference(getReferenceName(field.getMessageType()));
        }
        if (field.getType() == FieldDescriptor.Type.ENUM) {
            return new GraphQLTypeReference(ProtoToGql.getReferenceName(field.getEnumType()));
        }
        GraphQLType type = ProtoToGql.convertType(field, schemaOptions);
        if (type instanceof GraphQLList) {
            return ((GraphQLList) type).getWrappedType();
        }
        return type;
    }

    private static GraphQLInputType getInputTypeReference(Descriptor descriptor) {
        return new GraphQLTypeReference(getReferenceName(descriptor));
    }

    private static final GraphQLInputObjectField STATIC_FIELD =
            GraphQLInputObjectField.newInputObjectField()
                    .type(GraphQLString)
                    .name("_")
                    .defaultValue("")
                    .description("NOT USED")
                    .build();

    // Based on ProtoRegistry.Builder, but builds a map of descriptors rather than types.

    /**
     * Builder for GqlInputConverter.
     */
    public static class Builder {
        private final ArrayList<FileDescriptor> fileDescriptors = new ArrayList<>();
        private final ArrayList<EnumDescriptor> enumDescriptors = new ArrayList<>();

        public Builder add(FileDescriptor fileDescriptor) {
            fileDescriptors.add(fileDescriptor);
            return this;
        }

        public GqlInputConverter build() {
            HashBiMap<String, Descriptor> mapping = HashBiMap.create();
            HashBiMap<String, EnumDescriptor> enumMapping = HashBiMap.create(getEnumMap(enumDescriptors));
            LinkedList<Descriptor> loop = new LinkedList<>();

            Set<FileDescriptor> fileDescriptorSet = ProtoRegistry.extractDependencies(fileDescriptors);

            for (FileDescriptor fileDescriptor : fileDescriptorSet) {
                loop.addAll(fileDescriptor.getMessageTypes());
                enumMapping.putAll(getEnumMap(fileDescriptor.getEnumTypes()));
            }

            while (!loop.isEmpty()) {
                Descriptor descriptor = loop.pop();
                if (!mapping.containsKey(descriptor.getFullName())) {
                    mapping.put(getReferenceName(descriptor), descriptor);
                    loop.addAll(descriptor.getNestedTypes());
                    enumMapping.putAll(getEnumMap(descriptor.getEnumTypes()));
                }
            }

            return new GqlInputConverter(ImmutableBiMap.copyOf(mapping), ImmutableBiMap.copyOf(enumMapping));
        }

        private static BiMap<String, EnumDescriptor> getEnumMap(Iterable<EnumDescriptor> descriptors) {
            HashBiMap<String, EnumDescriptor> mapping = HashBiMap.create();
            for (EnumDescriptor enumDescriptor : descriptors) {
                mapping.put(ProtoToGql.getReferenceName(enumDescriptor), enumDescriptor);
            }
            return mapping;
        }
    }
}
