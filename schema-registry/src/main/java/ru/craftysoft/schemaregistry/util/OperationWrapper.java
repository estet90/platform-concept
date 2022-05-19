package ru.craftysoft.schemaregistry.util;

import io.smallrye.mutiny.Uni;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class OperationWrapper {

    public static <T> Uni<T> wrap(Logger log,
                                  String point,
                                  Supplier<Uni<T>> operation) {
        return wrap(log, point, operation, null, null);
    }

    public static <T> Uni<T> wrap(Logger log,
                                  String point,
                                  Supplier<Uni<T>> operation,
                                  @Nullable Supplier<String> logInCustomizer,
                                  @Nullable Function<T, String> logOutCustomizer) {
        ofNullable(logInCustomizer)
                .ifPresentOrElse(
                        supplier -> log.info("{}.in data: {}", point, supplier.get()),
                        () -> log.info("{}.in", point)
                );
        return operation.get()
                .invoke(result -> ofNullable(logOutCustomizer)
                        .ifPresentOrElse(
                                function -> log.info("{}.out result: {}", point, function.apply(result)),
                                () -> log.info("{}.out", point)
                        ))
                .onFailure()
                .invoke(e -> log.error("{}.thrown {}", point, e.getMessage()));
    }

}
