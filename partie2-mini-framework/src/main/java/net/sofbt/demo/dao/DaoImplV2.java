package net.sofbt.demo.dao;

import net.sofbt.minidi.annotations.Component;

@Component("dao2")
public class DaoImplV2 implements IDao {
    @Override
    public double getData() {
        System.out.println("  -> Version capteurs");
        return 12;
    }
}
