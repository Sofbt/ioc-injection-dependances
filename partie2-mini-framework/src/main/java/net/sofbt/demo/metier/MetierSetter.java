package net.sofbt.demo.metier;

import net.sofbt.demo.dao.IDao;
import net.sofbt.minidi.annotations.Autowired;
import net.sofbt.minidi.annotations.Component;
import net.sofbt.minidi.annotations.Qualifier;

/** b- Injection via le setter. */
@Component("metierSetter")
public class MetierSetter implements IMetier {
    private IDao dao;

    @Override
    public double calcul() {
        return dao.getData() * 2;
    }

    @Autowired
    @Qualifier("dao2")
    public void setDao(IDao dao) {
        this.dao = dao;
    }
}
