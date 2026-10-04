package net.sofbt.minidi.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registre commun des beans (singletons) et résolution par identifiant ou par type. */
public abstract class AbstractApplicationContext implements ApplicationContext {
    protected final Map<String, Object> singletons = new LinkedHashMap<>();

    @Override
    public Object getBean(String id) {
        Object bean = singletons.get(id);
        if (bean == null) throw new BeansException("Aucun bean avec l'id '" + id + "'");
        return bean;
    }

    @Override
    public <T> T getBean(Class<T> type) {
        List<Object> candidates = new ArrayList<>();
        for (Object bean : singletons.values()) {
            if (type.isInstance(bean)) candidates.add(bean);
        }
        if (candidates.isEmpty()) throw new BeansException("Aucun bean de type " + type.getName());
        if (candidates.size() > 1)
            throw new BeansException("Plusieurs beans de type " + type.getName() + " : utilisez un id");
        return type.cast(candidates.get(0));
    }

    /** Convertit une valeur littérale (String) vers le type cible. */
    protected static Object convert(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == float.class || type == Float.class) return Float.parseFloat(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        throw new BeansException("Conversion impossible de '" + value + "' vers " + type.getName());
    }

    protected static String setterName(String property) {
        return "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
    }
}
