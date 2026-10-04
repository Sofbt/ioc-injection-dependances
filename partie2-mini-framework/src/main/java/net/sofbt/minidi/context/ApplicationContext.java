package net.sofbt.minidi.context;

public interface ApplicationContext {
    Object getBean(String id);

    <T> T getBean(Class<T> type);
}
