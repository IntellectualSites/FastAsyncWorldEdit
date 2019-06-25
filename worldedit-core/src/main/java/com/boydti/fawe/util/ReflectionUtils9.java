package com.boydti.fawe.util;

import sun.misc.Unsafe;

import java.lang.reflect.*;
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
            List values = new ArrayList<>(Arrays.asList(previousValues));

            // 3. build new enum
            T newValue = (T) makeEnum(enumType, // The target enum class
                    enumName, // THE NEW ENUM INSTANCE TO BE DYNAMICALLY ADDED
                    values.size()); // can be used to pass values to the enum constuctor

            // 4. add new value
            values.add(newValue);

            // 5. Set new values field
            try {
                setFailsafeFieldValue(valuesField, null,
                        values.toArray((T[]) Array.newInstance(enumType, 0)));
            } catch (Throwable e) {
                Field ordinalField = Enum.class.getDeclaredField("ordinal");
                setFailsafeFieldValue(ordinalField, newValue, 0);
            }

            // 6. Clean enum cache
            cleanEnumCache(enumType);
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
        setFailsafeFieldValue(ordinalField, instance, 0);

        Field nameField = Enum.class.getDeclaredField("name");
        setFailsafeFieldValue(nameField, instance, value);

        return instance;
    }

    public static void setFailsafeFieldValue(Field field, Object target, Object value)
            throws NoSuchFieldException, IllegalAccessException {

        // let's make the field accessible
        field.setAccessible(true);

        // next we change the modifier in the Field instance to
        // not be final anymore, thus tricking reflection into
        // letting us modify the static final field
        if (Modifier.isFinal(field.getModifiers())) {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            int modifiers = modifiersField.getInt(field);

            // blank out the final bit in the modifiers int
            modifiers &= ~Modifier.FINAL;
            modifiersField.setInt(field, modifiers);
        }

        try {
            if (target == null) field.set(null, value);
            else field.set(target, value);
        } catch (NoSuchMethodError error) {
            field.set(target, value);
        }
    }

    private static void blankField(Class<?> enumClass, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        for (Field field : Class.class.getDeclaredFields()) {
            if (field.getName().contains(fieldName)) {
                AccessibleObject.setAccessible(new Field[]{field}, true);
                setFailsafeFieldValue(field, enumClass, null);
                break;
            }
        }
    }

    private static void cleanEnumCache(Class<?> enumClass)
            throws NoSuchFieldException, IllegalAccessException {
        blankField(enumClass, "enumConstantDirectory"); // Sun (Oracle?!?) JDK 1.5/6
        blankField(enumClass, "enumConstants"); // IBM JDK
    }
}
