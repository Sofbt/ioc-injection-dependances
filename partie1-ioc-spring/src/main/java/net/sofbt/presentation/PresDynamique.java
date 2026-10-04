package net.sofbt.presentation;

import net.sofbt.dao.IDao;
import net.sofbt.metier.IMetier;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Scanner;

/** Injection des dépendances par instanciation dynamique (réflexion + fichier config.txt). */
public class PresDynamique {
    public static void main(String[] args) throws Exception {
        InputStream in = PresDynamique.class.getClassLoader().getResourceAsStream("config.txt");
        Scanner scanner = new Scanner(in);

        String daoClassName = scanner.nextLine();
        Class<?> cDao = Class.forName(daoClassName);
        IDao dao = (IDao) cDao.getConstructor().newInstance();

        String metierClassName = scanner.nextLine();
        Class<?> cMetier = Class.forName(metierClassName);
        IMetier metier = (IMetier) cMetier.getConstructor().newInstance();

        Method setDao = cMetier.getMethod("setDao", IDao.class);
        setDao.invoke(metier, dao);

        System.out.println("Résultat = " + metier.calcul());
    }
}
