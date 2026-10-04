package net.sofbt.demo.presentation;

import net.sofbt.demo.metier.IMetier;
import net.sofbt.minidi.context.ApplicationContext;
import net.sofbt.minidi.context.XmlApplicationContext;

public class PresXml {
    public static void main(String[] args) {
        ApplicationContext context = new XmlApplicationContext("beans.xml");
        for (String id : new String[]{"metierConstructeur", "metierSetter", "metierAttribut"}) {
            IMetier metier = (IMetier) context.getBean(id);
            System.out.println(id + " :");
            System.out.println("  Résultat = " + metier.calcul());
        }
    }
}
