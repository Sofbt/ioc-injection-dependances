package net.sofbt.demo.dao;

import net.sofbt.minidi.annotations.Component;

@Component("dao")
public class DaoImpl implements IDao {
    @Override
    public double getData() {
        System.out.println("  -> Version base de données");
        return 23;
    }
}
