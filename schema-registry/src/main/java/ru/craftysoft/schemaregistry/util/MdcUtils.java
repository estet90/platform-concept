package ru.craftysoft.schemaregistry.util;

import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.*;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class MdcUtils {

    public static void withMdcRun(@Nullable Map<String, String> mdc, Runnable runnable) {
        withMdc(mdc, runnable).run();
    }

    public static void withMdcRun(Runnable runnable) {
        withMdc(runnable).run();
    }

    public static Runnable withMdc(Runnable runnable) {
        var mdc = MDC.getCopyOfContextMap();
        return withMdc(mdc, runnable);
    }

    public static Runnable withMdc(@Nullable Map<String, String> mdc, Runnable runnable) {
        if (mdc == null) {
            return runnable;
        }
        return () -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                runnable.run();
            } finally {
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    public static <U> U withMdcGet(Supplier<U> supplier) {
        return withMdc(supplier).get();
    }

    public static <U> U withMdcGet(Map<String, String> mdc, Supplier<U> supplier) {
        return withMdc(mdc, supplier).get();
    }

    public static <U> Supplier<U> withMdc(Supplier<U> supplier) {
        var mdc = MDC.getCopyOfContextMap();
        return withMdc(mdc, supplier);
    }

    public static <U> Supplier<U> withMdc(Map<String, String> mdc, Supplier<U> supplier) {
        if (mdc == null) {
            return supplier;
        }
        return () -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                return supplier.get();
            } finally {
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    public static <U> Consumer<U> withMdc(Consumer<U> consumer) {
        var mdc = MDC.getCopyOfContextMap();
        if (mdc == null) {
            return consumer;
        }
        return u -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                consumer.accept(u);
            } finally {
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    public static <IN, OUT> Function<IN, OUT> withMdc(Function<IN, OUT> function) {
        var mdc = MDC.getCopyOfContextMap();
        if (mdc == null) {
            return function;
        }
        return in -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                return function.apply(in);
            } finally {
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    public static <IN1, IN2, OUT> BiFunction<IN1, IN2, OUT> withMdc(BiFunction<IN1, IN2, OUT> function) {
        var mdc = MDC.getCopyOfContextMap();
        if (mdc == null) {
            return function;
        }
        return (in1, in2) -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                return function.apply(in1, in2);
            } finally {
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    public static <IN1, IN2> BiConsumer<IN1, IN2> withMdc(BiConsumer<IN1, IN2> consumer) {
        var mdc = MDC.getCopyOfContextMap();
        return withMdc(mdc, consumer);
    }

    public static <IN1, IN2> BiConsumer<IN1, IN2> withMdc(@Nullable Map<String, String> mdc, BiConsumer<IN1, IN2> consumer) {
        if (mdc == null) {
            return consumer;
        }
        return (in1, in2) -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                consumer.accept(in1, in2);
            } finally {
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

}
