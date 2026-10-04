package net.sofbt.metier;

import net.sofbt.dao.IDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("metier")
public class MetierImpl implements IMetier {
    // Couplage faible : la couche métier ne dépend que de l'interface IDao
    private IDao dao;

    public MetierImpl() {
    }

    // Injection via le constructeur (utilisée par la version annotations)
    @Autowired
    public MetierImpl(@Qualifier("dao") IDao dao) {
        this.dao = dao;
    }

    @Override
    public double calcul() {
        double t = dao.getData();
        double res = t * 12 * Math.PI / 2 * Math.cos(t);
        return res;
    }

    // Injection via le setter (utilisée par l'instanciation dynamique et la version XML)
    public void setDao(IDao dao) {
        this.dao = dao;
    }
}
