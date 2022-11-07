package org.redcraft.redcraftchat.helpers;

import java.lang.reflect.Field;

public class PrivateFieldExtractor {

    private PrivateFieldExtractor() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static Object extractPrivateApiField(Object object, String fieldName) {
        Object extractedObject;
        Field field = null;

        try {
            field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            extractedObject = field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        } finally {
            if (field != null) {
                field.setAccessible(false);
            }
        }

        return extractedObject;
    }
}
