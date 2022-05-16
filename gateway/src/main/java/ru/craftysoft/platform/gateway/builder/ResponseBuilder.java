package ru.craftysoft.platform.gateway.builder;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Timestamp;
import com.google.type.Date;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ResponseBuilder {

    public Object build(DynamicMessage dynamicMessage) {
        return dynamicMessage.getAllFields().size() == 1 && dynamicMessage.getAllFields().entrySet().iterator().next().getKey().isRepeated()
                ? buildList(dynamicMessage)
                : buildMap(dynamicMessage);
    }

    public Map<String, Object> buildMap(DynamicMessage dynamicMessage) {
        var result = new HashMap<String, Object>();
        dynamicMessage.getAllFields().forEach(((fieldDescriptor, o) -> fillResponse(fieldDescriptor, o, result)));
        return result;
    }

    public List<Object> buildList(DynamicMessage dynamicMessage) {
        var result = new ArrayList<>();
        dynamicMessage.getAllFields().values().forEach((o -> fillResponse(o, result)));
        return result;
    }

    private void fillResponse(Object object, List<Object> result) {
        var values = (List<?>) object;
        for (var value : values) {
            if (value instanceof DynamicMessage dynamicMessage) {
                var dynamicMessageValue = resolveDynamicMessageValue(dynamicMessage);
                result.add(dynamicMessageValue);
            }
        }
    }

    private void fillResponse(Descriptors.FieldDescriptor fieldDescriptor, Object object, HashMap<String, Object> result) {
        var name = fieldDescriptor.getName();
        if (fieldDescriptor.isRepeated()) {
            var values = (List<?>) object;
            var list = new ArrayList<>();
            for (var value : values) {
                if (value instanceof DynamicMessage dynamicMessage) {
                    var dynamicMessageValue = resolveDynamicMessageValue(dynamicMessage);
                    list.add(dynamicMessageValue);
                }
            }
            result.put(name, list);
        } else if (object instanceof DynamicMessage dynamicMessage) {
            var dynamicMessageValue = resolveDynamicMessageValue(dynamicMessage);
            result.put(name, dynamicMessageValue);
        } else {
            if (object instanceof Descriptors.EnumValueDescriptor enm) {
                result.put(name, enm.getName());
            } else {
                result.put(name, object);
            }
        }
    }

    private Object resolveDynamicMessageValue(DynamicMessage dynamicMessage) {
        var fullName = dynamicMessage.getDescriptorForType().getFullName();
        switch (fullName) {
            case "ru.craftysoft.proto.NullableString",
                    "ru.craftysoft.proto.NullableDouble",
                    "ru.craftysoft.proto.NullableFloat",
                    "ru.craftysoft.proto.NullableInt64",
                    "ru.craftysoft.proto.NullableUInt64",
                    "ru.craftysoft.proto.NullableInt32",
                    "ru.craftysoft.proto.NullableUInt32",
                    "ru.craftysoft.proto.NullableBool",
                    "ru.craftysoft.proto.NullableBytes" -> {
                var value = dynamicMessage.getAllFields().values().iterator().next();
                if (value instanceof Descriptors.EnumValueDescriptor enumValueDescriptor && "NULL_VALUE".equals(enumValueDescriptor.getName())) {
                    return null;
                } else {
                    return value;
                }
            }
            case "google.type.Date" -> {
                var dateParts = dynamicMessage.getAllFields().entrySet().stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().getFullName(), Map.Entry::getValue));
                if (dateParts.isEmpty()) {
                    return null;
                }
                var date = Date.newBuilder()
                        .setYear((int) dateParts.get("google.type.Date.year"))
                        .setMonth((int) dateParts.get("google.type.Date.month"))
                        .setDay((int) dateParts.get("google.type.Date.day"))
                        .build();
                return Date.getDefaultInstance().equals(date)
                        ? null
                        : LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
            }
            case "google.protobuf.Timestamp" -> {
                var timestampParts = dynamicMessage.getAllFields().entrySet().stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().getFullName(), Map.Entry::getValue));
                if (timestampParts.isEmpty()) {
                    return null;
                }
                var timestamp = Timestamp.newBuilder()
                        .setSeconds((long) timestampParts.get("google.protobuf.Timestamp.seconds"))
                        .setNanos((int) timestampParts.get("google.protobuf.Timestamp.nanos"))
                        .build();
                return Timestamp.getDefaultInstance().equals(timestamp)
                        ? null
                        : Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()).atOffset(ZoneOffset.UTC);
            }
            default -> {
                var intermediateResult = new HashMap<String, Object>();
                dynamicMessage.getAllFields().forEach(((fd, o) -> fillResponse(fd, o, intermediateResult)));
                return intermediateResult;
            }
        }
    }

}
