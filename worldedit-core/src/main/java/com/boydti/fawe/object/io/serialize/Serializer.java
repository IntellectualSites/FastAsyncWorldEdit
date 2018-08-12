package com.boydti.fawe.object.io.serialize;

import com.boydti.fawe.util.ReflectionUtils;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * Call serialize(stream) to serialize any field with @Serialize<br/>
 * Call deserialize(stream) to deserialize any field with @Serialize<br/>
 */
public interface Serializer extends Serializable {
    default void serialize(java.io.ObjectOutputStream stream) throws IOException {
        try {
            for (Field field : getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getDeclaredAnnotation(Serialize.class) != null) {
                    Class<?> type = field.getType();
                    boolean primitive = type.isPrimitive();
                    Object value = field.get(this);
                    if (primitive) {
                        stream.writeObject(value);
                    } else if (value == null){
                        stream.writeByte(0);
                    } else {
                        stream.writeByte(1);
                        stream.writeObject(value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    default void deserialize(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        for (Field field : getClass().getDeclaredFields()) {
            if (field.getDeclaredAnnotation(Serialize.class) != null) {
                Class<?> type = field.getType();
                boolean primitive = type.isPrimitive();

                if (primitive) {
                    ReflectionUtils.setField(field, this, stream.readObject());
                } else if (stream.readByte() == 1) {
                    ReflectionUtils.setField(field, this, stream.readObject());
                }
            }
        }
    }
}
