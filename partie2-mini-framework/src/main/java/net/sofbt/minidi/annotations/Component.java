package net.sofbt.minidi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marque une classe comme bean géré par le conteneur. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
    /** Identifiant du bean (par défaut : nom simple de la classe en minuscule initiale). */
    String value() default "";
}
