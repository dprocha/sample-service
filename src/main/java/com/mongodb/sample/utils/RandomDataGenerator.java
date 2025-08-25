package com.mongodb.sample.utils;

import com.github.javafaker.Faker;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RandomDataGenerator {

    private static final Faker faker = new Faker();

    public static <T> T generateRandomObject(Class<T> clazz) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();

            for (Field field : clazz.getDeclaredFields()) {

                if (!field.getName().equalsIgnoreCase("id")) {

                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();

                    // Collections (Lists) first
                    if (Collection.class.isAssignableFrom(fieldType)) {
                        field.set(instance, generateRandomList(field));
                        continue;
                    }

                    Object value = randomScalar(fieldType);
                    if (value != null) {
                        field.set(instance, value);
                    } else if (fieldType.isEnum()) {
                        Object[] enumValues = fieldType.getEnumConstants();
                        if (enumValues != null && enumValues.length > 0) {
                            field.set(instance, enumValues[ThreadLocalRandom.current().nextInt(enumValues.length)]);
                        }
                    } else if (!fieldType.isPrimitive() && !fieldType.getName().startsWith("java.")) {
                        // Nested custom object
                        field.set(instance, generateRandomObject(fieldType));
                    }
                }
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Error generating random object for class: " + clazz.getName(), e);
        }
    }

    /**
     * Centralized scalar/random value creator for single values
     */
    private static Object randomScalar(Class<?> type) {
        // Strings
        if (type == String.class) {
            return faker.lorem().word() + "_" + UUID.randomUUID();
        }

        // Booleans
        if (type == boolean.class || type == Boolean.class) {
            return ThreadLocalRandom.current().nextBoolean();
        }

        // Integral numbers
        if (type == int.class || type == Integer.class) {
            return ThreadLocalRandom.current().nextInt(0, 1_000_000);
        }
        if (type == long.class || type == Long.class) {
            return ThreadLocalRandom.current().nextLong(0L, 1_000_000_000L);
        }
        if (type == short.class || type == Short.class) {
            return (short) ThreadLocalRandom.current().nextInt(0, Short.MAX_VALUE + 1);
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) ThreadLocalRandom.current().nextInt(0, 128);
        }
        if (type == char.class || type == Character.class) {
            return (char) ('a' + ThreadLocalRandom.current().nextInt(26));
        }

        // Floating-point
        if (type == double.class || type == Double.class) {
            return ThreadLocalRandom.current().nextDouble(0.0, 1_000_000.0);
        }
        if (type == float.class || type == Float.class) {
            return (float) ThreadLocalRandom.current().nextDouble(0.0, 1_000_000.0);
        }

        // BigDecimal
        if (type == BigDecimal.class) {
            // 2 decimal places (e.g., money-like)
            double val = ThreadLocalRandom.current().nextDouble(0.0, 1_000_000.0);
            return BigDecimal.valueOf(Math.round(val * 100.0) / 100.0);
        }

        // Dates
        if (type == LocalDate.class) {
            long start = LocalDate.of(1990, 1, 1).toEpochDay();
            long end = LocalDate.now().toEpochDay();
            return LocalDate.ofEpochDay(ThreadLocalRandom.current().nextLong(start, end + 1));
        }
        if (type == LocalDateTime.class) {
            randomLocalDateTime(LocalDateTime.of(1990, 1, 1, 0, 0), LocalDateTime.now());
        }

        // Not a supported scalar
        return null;
    }

    /**
     * Generates a random List<T> for supported element types, or List of nested objects if T is complex.
     */
    @SuppressWarnings("unchecked")
    private static List<Object> generateRandomList(Field field) {
        List<Object> list = new ArrayList<>();
        try {
            // Resolve element type: List<T>
            Type g = field.getGenericType();
            if (!(g instanceof ParameterizedType)) {
                // Raw collection — fill with random strings
                int size = ThreadLocalRandom.current().nextInt(1, 2);
                for (int i = 0; i < size; i++) list.add(UUID.randomUUID().toString());
                return list;
            }

            Type elem = ((ParameterizedType) g).getActualTypeArguments()[0];
            if (!(elem instanceof Class<?>)) {
                // Wildcards / type vars -> fallback to String
                int size = ThreadLocalRandom.current().nextInt(1, 2);
                for (int i = 0; i < size; i++) list.add(UUID.randomUUID().toString());
                return list;
            }

            Class<?> elemType = (Class<?>) elem;
            int size = ThreadLocalRandom.current().nextInt(1, 2);

            for (int i = 0; i < size; i++) {
                // Scalars first
                Object scalar = randomScalar(elemType);
                if (scalar != null) {
                    list.add(scalar);
                    continue;
                }

                // Enums
                if (elemType.isEnum()) {
                    Object[] values = elemType.getEnumConstants();
                    if (values != null && values.length > 0) {
                        list.add(values[ThreadLocalRandom.current().nextInt(values.length)]);
                        continue;
                    }
                }

                // For other java.* classes we don't specialize, add a String token
                if (elemType.getName().startsWith("java.")) {
                    list.add(UUID.randomUUID().toString());
                    continue;
                }

                // Complex POJO element -> recurse
                list.add(generateRandomObject(elemType));
            }
        } catch (Exception ignore) {
            // best-effort; return whatever was built
        }
        return list;
    }

    public static LocalDateTime randomLocalDateTime(LocalDateTime startInclusive,
                                                    LocalDateTime endExclusive) {
        if (!startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("startInclusive must be before endExclusive");
        }
        ZoneId zone = ZoneId.systemDefault();
        long start = startInclusive.atZone(zone).toInstant().toEpochMilli();
        long end = endExclusive.atZone(zone).toInstant().toEpochMilli();
        long rnd = ThreadLocalRandom.current().nextLong(start, end);
        return Instant.ofEpochMilli(rnd).atZone(zone).toLocalDateTime();
    }
}
