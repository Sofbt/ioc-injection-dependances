package net.sofbt.presentation;

import net.sofbt.ext.DaoImplV2;
import net.sofbt.metier.MetierImpl;

/** Injection des dépendances par instanciation statique (new). */
public class PresStatique {
    public static void main(String[] args) {
        DaoImplV2 dao = new DaoImplV2();
        MetierImpl metier = new MetierImpl(dao);
        // ou bien : metier.setDao(dao);
        System.out.println("Résultat = " + metier.calcul());
    }
}
