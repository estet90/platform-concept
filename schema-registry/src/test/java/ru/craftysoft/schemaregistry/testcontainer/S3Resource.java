package ru.craftysoft.schemaregistry.testcontainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class S3Resource implements QuarkusTestResourceLifecycleManager {

    private final LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.11.2"))
            .withServices(LocalStackContainer.Service.S3);

    @Override
    public Map<String, String> start() {
        localStack.start();
        var service = localStack.getEndpointOverride(LocalStackContainer.Service.S3);
        var url = "http://%s:%d".formatted(service.getHost(), service.getPort());
        return Map.of(
                "quarkus.s3.endpoint-override", url,
                "quarkus.s3.credentials.static-provider.access-key-id", localStack.getAccessKey(),
                "quarkus.s3.credentials.static-provider.secret-access-key", localStack.getSecretKey()
        );
    }

    @Override
    public void stop() {
        localStack.stop();
    }
}
