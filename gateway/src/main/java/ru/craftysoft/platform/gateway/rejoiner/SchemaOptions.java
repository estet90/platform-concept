package ru.craftysoft.platform.gateway.rejoiner;

import java.util.Map;

public class SchemaOptions {

    private final boolean useProtoScalarTypes;
    private final Map<String, String> commentsMap;

    public SchemaOptions(boolean useProtoScalarTypes, Map<String, String> commentsMap) {
        this.useProtoScalarTypes = useProtoScalarTypes;
        this.commentsMap = commentsMap;
    }

    public boolean useProtoScalarTypes() {
        return useProtoScalarTypes;
    }

    public Map<String, String> commentsMap() {
        return commentsMap;
    }
}
