package ru.craftysoft.schemaregistry.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.ThreadLocalRandom;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UuidUtils {

    public static String generateDefaultUuid() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong());
    }

}
