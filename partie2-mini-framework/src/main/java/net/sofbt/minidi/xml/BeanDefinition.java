package net.sofbt.minidi.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/** &lt;bean id="..." class="..."&gt; avec ses injections (constructor-arg, property, field). */
@XmlAccessorType(XmlAccessType.FIELD)
public class BeanDefinition {
    @XmlAttribute(required = true)
    private String id;

    @XmlAttribute(name = "class", required = true)
    private String className;

    @XmlElement(name = "constructor-arg")
    private List<InjectionDefinition> constructorArgs = new ArrayList<>();

    @XmlElement(name = "property")
    private List<InjectionDefinition> properties = new ArrayList<>();

    @XmlElement(name = "field")
    private List<InjectionDefinition> fields = new ArrayList<>();

    public String getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public List<InjectionDefinition> getConstructorArgs() {
        return constructorArgs;
    }

    public List<InjectionDefinition> getProperties() {
        return properties;
    }

    public List<InjectionDefinition> getFields() {
        return fields;
    }
}
