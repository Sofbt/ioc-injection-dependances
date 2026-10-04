package net.sofbt.demo.metier;

import net.sofbt.demo.dao.IDao;
import net.sofbt.minidi.annotations.Autowired;
import net.sofbt.minidi.annotations.Component;
import net.sofbt.minidi.annotations.Qualifier;

/** c- Injection via l'attribut (accès direct au Field, même privé). */
@Component("metierAttribut")
public class MetierAttribut implements IMetier {
    @Autowired
    @Qualifier("dao")
    private IDao dao;

    @Override
    public double calcul() {
        return dao.getData() * 2;
    }
}
