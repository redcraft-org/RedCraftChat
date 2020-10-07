package org.redcraft.redcraftchat.helpers;

import java.lang.reflect.Field;

public class PrivateFieldExtractor {
    public static Object extractPrivateApiField(Object object, String fieldName) {
        Object extractedObject;
        Field field = null;

        try {
            field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            extractedObject = field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            if (field != null) {
                field.setAccessible(false);
            }
        }

        return extractedObject;
    }
}
