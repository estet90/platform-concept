package ru.craftysoft.schemaregistry.configuration;

import io.quarkus.test.junit.QuarkusTestProfile;
import ru.craftysoft.schemaregistry.testcontainer.PostgreSqlResource;
import ru.craftysoft.schemaregistry.testcontainer.S3Resource;

import java.util.List;
import java.util.Set;

public class ApplicationTestProfile implements QuarkusTestProfile {

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(TestDslContext.class);
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(
                new TestResourceEntry(S3Resource.class),
                new TestResourceEntry(PostgreSqlResource.class)
        );
    }
}
