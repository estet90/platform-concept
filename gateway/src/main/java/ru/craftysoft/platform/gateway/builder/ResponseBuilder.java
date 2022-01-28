package ru.craftysoft.platform.gateway.builder;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ResponseBuilder {

    public Map<String, Object> build(DynamicMessage dynamicMessage) {
        var result = new HashMap<String, Object>();
        var fieldDescriptors = dynamicMessage.getDescriptorForType().getFields();
        for (var fieldDescriptor : fieldDescriptors) {
            if (dynamicMessage.hasField(fieldDescriptor)) {
                var o = dynamicMessage.getField(fieldDescriptor);
                fillResponse(fieldDescriptor, o, result);
            }
        }
        return (Map<String, Object>) result;
    }

    private void fillResponse(Descriptors.FieldDescriptor fieldDescriptor, Object object, HashMap<String, Object> result) {
        try {
            if (fieldDescriptor.isRepeated()) {
                var values = (List<Object>) object;
                var list = new ArrayList<>();
                for (var value : values) {
                    if (value instanceof DynamicMessage dynamicMessage) {
                        var intermediateResult = new HashMap<String, Object>();
                        var fieldDescriptors = dynamicMessage.getDescriptorForType().getFields();
                        for (var fd : fieldDescriptors) {
                            if (dynamicMessage.hasField(fieldDescriptor)) {
                                var o = dynamicMessage.getField(fieldDescriptor);
                                fillResponse(fd, o, intermediateResult);
                            }
                        }
                        list.add(intermediateResult);
                    }
                }
                result.put(fieldDescriptor.getName(), list);
            } else if (fieldDescriptor.getType().equals(Descriptors.FieldDescriptor.Type.MESSAGE)) {
                var dynamicMessage = (DynamicMessage) object;
                var intermediateResult = new HashMap<String, Object>();
                var fieldDescriptors = dynamicMessage.getDescriptorForType().getFields();
                for (var fd : fieldDescriptors) {
                    if (dynamicMessage.hasField(fieldDescriptor)) {
                        var o = dynamicMessage.getField(fieldDescriptor);
                        fillResponse(fd, o, intermediateResult);
                    }
                }
                result.put(fieldDescriptor.getName(), intermediateResult);
            } else {
                if (object instanceof Descriptors.EnumValueDescriptor enm) {
                    result.put(fieldDescriptor.getName(), enm.getName());
                } else {
                    result.put(fieldDescriptor.getName(), object);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
