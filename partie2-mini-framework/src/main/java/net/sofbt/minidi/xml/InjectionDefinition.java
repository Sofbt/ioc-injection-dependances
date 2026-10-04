package net.sofbt.minidi.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/** Une dépendance à injecter : une référence vers un bean (ref) ou une valeur littérale (value). */
@XmlAccessorType(XmlAccessType.FIELD)
public class InjectionDefinition {
    @XmlAttribute
    private String name;

    @XmlAttribute
    private String ref;

    @XmlAttribute
    private String value;

    public String getName() {
        return name;
    }

    public String getRef() {
        return ref;
    }

    public String getValue() {
        return value;
    }
}
