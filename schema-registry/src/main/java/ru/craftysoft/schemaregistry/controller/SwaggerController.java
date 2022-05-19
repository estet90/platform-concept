package ru.craftysoft.schemaregistry.controller;

import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

@Path("/swagger")
@ApplicationScoped
public class SwaggerController {

    private final byte[] swagger;

    private static final String FILE_NAME = "schema-registry.yaml";

    public SwaggerController() throws IOException {
        this.swagger = requireNonNull(getClass().getResourceAsStream("/openapi/" + FILE_NAME)).readAllBytes();
    }

    @GET
    @Path(FILE_NAME)
    @Produces("text/yaml;charset=UTF-8")
    public Uni<byte[]> opportunityEntitySwagger() {
        return Uni.createFrom().item(swagger);
    }
}
