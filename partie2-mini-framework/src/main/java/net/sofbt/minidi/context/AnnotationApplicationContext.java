package net.sofbt.minidi.context;

import net.sofbt.minidi.annotations.Autowired;
import net.sofbt.minidi.annotations.Component;
import net.sofbt.minidi.annotations.Qualifier;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Conteneur configuré par annotations : scanne les packages à la recherche de classes @Component
 * puis injecte les dépendances marquées @Autowired (constructeur, setter ou attribut).
 */
public class AnnotationApplicationContext extends AbstractApplicationContext {
    private final Map<String, Class<?>> definitions = new LinkedHashMap<>();
    private final Set<String> inCreation = new HashSet<>();

    public AnnotationApplicationContext(String... basePackages) {
        for (String pkg : basePackages) {
            for (Class<?> c : scan(pkg)) {
                Component component = c.getAnnotation(Component.class);
                if (component == null || c.isInterface() || Modifier.isAbstract(c.getModifiers())) continue;
                String id = component.value().isEmpty() ? defaultId(c) : component.value();
                if (definitions.containsKey(id)) throw new BeansException("Id de bean dupliqué : " + id);
                definitions.put(id, c);
            }
        }
        for (String id : definitions.keySet()) getOrCreate(id);
    }

    private Object getOrCreate(String id) {
        Object bean = singletons.get(id);
        if (bean != null) return bean;
        Class<?> c = definitions.get(id);
        if (c == null) throw new BeansException("Aucun bean avec l'id '" + id + "'");
        if (!inCreation.add(id)) throw new BeansException("Dépendance circulaire sur le constructeur de '" + id + "'");
        try {
            // a- Injection via le constructeur
            Constructor<?> constructor = chooseConstructor(c);
            constructor.setAccessible(true);
            Object[] args = resolveParameters(constructor.getParameters());
            bean = constructor.newInstance(args);
            singletons.put(id, bean);

            // c- Injection via l'attribut (accès direct au Field)
            for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                for (Field field : k.getDeclaredFields()) {
                    if (!field.isAnnotationPresent(Autowired.class)) continue;
                    Qualifier q = field.getAnnotation(Qualifier.class);
                    Object dep = q != null ? getOrCreate(q.value()) : resolveByType(field.getType());
                    field.setAccessible(true);
                    field.set(bean, dep);
                }
            }

            // b- Injection via le setter
            for (Method method : c.getMethods()) {
                if (!method.isAnnotationPresent(Autowired.class)) continue;
                Object[] deps;
                Qualifier q = method.getAnnotation(Qualifier.class);
                if (q != null && method.getParameterCount() == 1) deps = new Object[]{getOrCreate(q.value())};
                else deps = resolveParameters(method.getParameters());
                method.invoke(bean, deps);
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

    private Constructor<?> chooseConstructor(Class<?> c) throws NoSuchMethodException {
        Constructor<?>[] constructors = c.getDeclaredConstructors();
        for (Constructor<?> ctor : constructors) {
            if (ctor.isAnnotationPresent(Autowired.class)) return ctor;
        }
        if (constructors.length == 1) return constructors[0];
        return c.getDeclaredConstructor();
    }

    private Object[] resolveParameters(Parameter[] params) {
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Qualifier q = params[i].getAnnotation(Qualifier.class);
            args[i] = q != null ? getOrCreate(q.value()) : resolveByType(params[i].getType());
        }
        return args;
    }

    private Object resolveByType(Class<?> type) {
        List<String> candidates = new ArrayList<>();
        for (Map.Entry<String, Class<?>> e : definitions.entrySet()) {
            if (type.isAssignableFrom(e.getValue())) candidates.add(e.getKey());
        }
        if (candidates.isEmpty()) throw new BeansException("Aucun bean de type " + type.getName());
        if (candidates.size() > 1)
            throw new BeansException("Plusieurs beans de type " + type.getName() + " " + candidates + " : utilisez @Qualifier");
        return getOrCreate(candidates.get(0));
    }

    private static String defaultId(Class<?> c) {
        String n = c.getSimpleName();
        return Character.toLowerCase(n.charAt(0)) + n.substring(1);
    }

    /** Parcourt le classpath (répertoires et JAR) pour trouver toutes les classes d'un package. */
    private static List<Class<?>> scan(String basePackage) {
        List<Class<?>> classes = new ArrayList<>();
        String path = basePackage.replace('.', '/');
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> resources = loader.getResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if ("file".equals(url.getProtocol())) {
                    File dir = new File(URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8));
                    scanDirectory(dir, basePackage, loader, classes);
                } else if ("jar".equals(url.getProtocol())) {
                    try (JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile()) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            String name = entries.nextElement().getName();
                            if (name.startsWith(path) && name.endsWith(".class"))
                                classes.add(loader.loadClass(name.replace('/', '.').replace(".class", "")));
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new BeansException("Erreur lors du scan du package " + basePackage, e);
        }
        return classes;
    }

    private static void scanDirectory(File dir, String pkg, ClassLoader loader, List<Class<?>> out)
            throws ClassNotFoundException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) scanDirectory(f, pkg + "." + f.getName(), loader, out);
            else if (f.getName().endsWith(".class"))
                out.add(loader.loadClass(pkg + "." + f.getName().substring(0, f.getName().length() - 6)));
        }
    }
}
