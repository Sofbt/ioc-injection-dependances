package net.sofbt.demo.presentation;

import net.sofbt.demo.metier.IMetier;
import net.sofbt.minidi.context.AnnotationApplicationContext;
import net.sofbt.minidi.context.ApplicationContext;

public class PresAnnotations {
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationApplicationContext("net.sofbt.demo");
        for (String id : new String[]{"metierConstructeur", "metierSetter", "metierAttribut"}) {
            IMetier metier = (IMetier) context.getBean(id);
            System.out.println(id + " :");
            System.out.println("  Résultat = " + metier.calcul());
        }
    }
}
