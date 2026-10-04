package net.sofbt.demo.metier;

import net.sofbt.demo.dao.IDao;
import net.sofbt.minidi.annotations.Autowired;
import net.sofbt.minidi.annotations.Component;
import net.sofbt.minidi.annotations.Qualifier;

/** a- Injection via le constructeur. */
@Component("metierConstructeur")
public class MetierConstructeur implements IMetier {
    private final IDao dao;

    @Autowired
    public MetierConstructeur(@Qualifier("dao") IDao dao) {
        this.dao = dao;
    }

    @Override
    public double calcul() {
        return dao.getData() * 2;
    }
}
