package net.sofbt.minidi.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

/** Racine du fichier de configuration : &lt;beans&gt;. */
@XmlRootElement(name = "beans")
@XmlAccessorType(XmlAccessType.FIELD)
public class Beans {
    @XmlElement(name = "bean")
    private List<BeanDefinition> beans = new ArrayList<>();

    public List<BeanDefinition> getBeans() {
        return beans;
    }
}
