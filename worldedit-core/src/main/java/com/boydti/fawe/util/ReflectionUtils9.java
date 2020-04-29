package com.boydti.fawe.util;

import sun.misc.Unsafe;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectionUtils9 {
    public static <T extends Enum<?>> T addEnum(Class<T> enumType, String enumName) {

        // 0. Sanity checks
        if (!Enum.class.isAssignableFrom(enumType)) {
            throw new RuntimeException("class " + enumType + " is not an instance of Enum");
        }
        // 1. Lookup "$VALUES" holder in enum class and get previous enum instances
        Field valuesField = null;
        Field[] fields = enumType.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().contains("$VALUES")) {
                valuesField = field;
                break;
            }
        }
        AccessibleObject.setAccessible(new Field[]{valuesField}, true);

        try {

            // 2. Copy it
            T[] previousValues = (T[]) valuesField.get(enumType);
            List<T> values = new ArrayList<>(Arrays.asList(previousValues));

            // 3. build new enum
            T newValue = (T) makeEnum(enumType, // The target enum class
                    enumName, // THE NEW ENUM INSTANCE TO BE DYNAMICALLY ADDED
                    values.size()); // can be used to pass values to the enum constructor

            // 4. add new value
            values.add(newValue);

            // 5. Set new values field
            try {
                ReflectionUtils.setFailsafeFieldValue(valuesField, null,
                        values.toArray((T[]) Array.newInstance(enumType, 0)));
            } catch (Throwable e) {
                Field ordinalField = Enum.class.getDeclaredField("ordinal");
                ReflectionUtils.setFailsafeFieldValue(ordinalField, newValue, 0);
            }

            // 6. Clean enum cache
            ReflectionUtils.cleanEnumCache(enumType);
            return newValue;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static Object makeEnum(Class<?> enumClass, String value, int ordinal) throws Exception {
        Constructor<?> constructor = Unsafe.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Unsafe unsafe = (Unsafe) constructor.newInstance();
        Object instance = unsafe.allocateInstance(enumClass);

        Field ordinalField = Enum.class.getDeclaredField("ordinal");
        ReflectionUtils.setFailsafeFieldValue(ordinalField, instance, 0);

        Field nameField = Enum.class.getDeclaredField("name");
        ReflectionUtils.setFailsafeFieldValue(nameField, instance, value);

        return instance;
    }
}
