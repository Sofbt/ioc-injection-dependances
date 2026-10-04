package net.sofbt.minidi.context;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import net.sofbt.minidi.xml.BeanDefinition;
import net.sofbt.minidi.xml.Beans;
import net.sofbt.minidi.xml.InjectionDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Conteneur configuré par un fichier XML, lu avec JAXB (OXM : mapping Objet/XML).
 */
public class XmlApplicationContext extends AbstractApplicationContext {
    private final Map<String, BeanDefinition> definitions = new LinkedHashMap<>();
    private final Set<String> inCreation = new HashSet<>();

    public XmlApplicationContext(String configFile) {
        Beans beans = load(configFile);
        for (BeanDefinition def : beans.getBeans()) {
            if (definitions.containsKey(def.getId())) throw new BeansException("Id de bean dupliqué : " + def.getId());
            definitions.put(def.getId(), def);
        }
        for (String id : definitions.keySet()) getOrCreate(id);
    }

    private static Beans load(String configFile) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFile)) {
            if (in == null) throw new BeansException("Fichier de configuration introuvable : " + configFile);
            JAXBContext jaxb = JAXBContext.newInstance(Beans.class);
            return (Beans) jaxb.createUnmarshaller().unmarshal(in);
        } catch (JAXBException | IOException e) {
            throw new BeansException("Lecture impossible de " + configFile, e);
        }
    }

    private Object getOrCreate(String id) {
        Object bean = singletons.get(id);
        if (bean != null) return bean;
        BeanDefinition def = definitions.get(id);
        if (def == null) throw new BeansException("Aucun bean avec l'id '" + id + "'");
        if (!inCreation.add(id)) throw new BeansException("Dépendance circulaire sur le constructeur de '" + id + "'");
        try {
            Class<?> c = Class.forName(def.getClassName());

            // a- Injection via le constructeur
            bean = instantiate(c, def.getConstructorArgs());
            singletons.put(id, bean);

            // b- Injection via le setter
            for (InjectionDefinition p : def.getProperties()) {
                Method setter = findSetter(c, p.getName());
                setter.invoke(bean, resolve(p, setter.getParameterTypes()[0]));
            }

            // c- Injection via l'attribut (accès direct au Field)
            for (InjectionDefinition f : def.getFields()) {
                Field field = findField(c, f.getName());
                field.setAccessible(true);
                field.set(bean, resolve(f, field.getType()));
            }
            return bean;
        } catch (BeansException e) {
            throw e;
        } catch (Exception e) {
            throw new BeansException("Erreur lors de la création du bean '" + id + "'", e);
        } finally {
            inCreation.remove(id);
        }
    }

    private Object instantiate(Class<?> c, List<InjectionDefinition> args) throws Exception {
        if (args.isEmpty()) {
            Constructor<?> ctor = c.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        }
        // Les références sont résolues une seule fois, puis on cherche un constructeur compatible
        Object[] refs = new Object[args.size()];
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).getRef() != null) refs[i] = getOrCreate(args.get(i).getRef());
        }
        for (Constructor<?> ctor : c.getDeclaredConstructors()) {
            Class<?>[] types = ctor.getParameterTypes();
            if (types.length != args.size()) continue;
            Object[] values = new Object[types.length];
            boolean ok = true;
            for (int i = 0; i < types.length && ok; i++) {
                if (refs[i] != null) {
                    ok = types[i].isInstance(refs[i]);
                    values[i] = refs[i];
                } else {
                    try {
                        values[i] = convert(args.get(i).getValue(), types[i]);
                    } catch (RuntimeException e) {
                        ok = false;
                    }
                }
            }
            if (ok) {
                ctor.setAccessible(true);
                return ctor.newInstance(values);
            }
        }
        throw new BeansException("Aucun constructeur compatible dans " + c.getName());
    }

    private Object resolve(InjectionDefinition d, Class<?> type) {
        if (d.getRef() != null) return getOrCreate(d.getRef());
        if (d.getValue() != null) return convert(d.getValue(), type);
        throw new BeansException("'" + d.getName() + "' doit avoir un attribut ref ou value");
    }

    private static Method findSetter(Class<?> c, String property) {
        String name = setterName(property);
        for (Method m : c.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 1) return m;
        }
        throw new BeansException("Setter " + name + " introuvable dans " + c.getName());
    }

    private static Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                return k.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new BeansException("Attribut " + name + " introuvable dans " + c.getName());
    }
}
