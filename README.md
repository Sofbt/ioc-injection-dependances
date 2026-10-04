# Compte rendu — Inversion de contrôle et injection des dépendances

Activité pratique reprenant l'exemple traité dans la vidéo
[Injection des dépendances](https://www.youtube.com/watch?v=vOLqabN-n2k).

Le dépôt contient deux projets Maven indépendants :

| Dossier | Contenu |
|---|---|
| [`partie1-ioc-spring`](partie1-ioc-spring) | Couplage faible, injection statique, dynamique et avec Spring (XML + annotations) |
| [`partie2-mini-framework`](partie2-mini-framework) | Mini framework d'injection des dépendances inspiré de Spring IoC |

---

## Objectif

Une application doit être **fermée à la modification et ouverte à l'extension** (principe Open/Closed).
Pour cela, chaque couche dépend d'**interfaces** et non de classes concrètes (**couplage faible**),
et c'est un acteur externe (le `main`, un fichier de configuration ou un framework) qui crée les objets
et les relie entre eux : c'est l'**inversion de contrôle**, et la mise en relation s'appelle
l'**injection des dépendances**.

Architecture utilisée :

```
presentation  ──>  IMetier  <──  MetierImpl  ──>  IDao  <──  DaoImpl / DaoImplV2
```

---

## Partie 1 — Couplage faible et injection des dépendances

### 1. Interface `IDao`

```java
public interface IDao {
    double getData();
}
```

### 2. Implémentations de `IDao`

`DaoImpl` simule une lecture depuis une base de données, `DaoImplV2` (package `ext`) une lecture
depuis des capteurs. La seconde est une **extension** : elle a été ajoutée sans modifier le code existant.

```java
@Component("dao")
public class DaoImpl implements IDao {
    @Override
    public double getData() {
        System.out.println("Version base de données");
        double temperature = 23;
        return temperature;
    }
}
```

### 3. Interface `IMetier`

```java
public interface IMetier {
    double calcul();
}
```

### 4. Implémentation `MetierImpl` avec couplage faible

`MetierImpl` ne connaît que l'interface `IDao`. Aucune classe concrète n'est instanciée avec `new`
dans la couche métier : la dépendance est fournie de l'extérieur, par le constructeur ou par le setter.

```java
@Component("metier")
public class MetierImpl implements IMetier {
    private IDao dao; // couplage faible

    public MetierImpl() { }

    @Autowired
    public MetierImpl(@Qualifier("dao") IDao dao) { this.dao = dao; }

    @Override
    public double calcul() {
        double t = dao.getData();
        return t * 12 * Math.PI / 2 * Math.cos(t);
    }

    public void setDao(IDao dao) { this.dao = dao; }
}
```

### 5. Injection des dépendances

#### a. Par instanciation statique — [`PresStatique`](partie1-ioc-spring/src/main/java/net/sofbt/presentation/PresStatique.java)

```java
DaoImplV2 dao = new DaoImplV2();
MetierImpl metier = new MetierImpl(dao);   // ou metier.setDao(dao)
System.out.println("Résultat = " + metier.calcul());
```

Simple, mais pour changer d'implémentation il faut modifier le code et recompiler.

```
Version capteurs
Résultat = 190.87526861687965
```

#### b. Par instanciation dynamique — [`PresDynamique`](partie1-ioc-spring/src/main/java/net/sofbt/presentation/PresDynamique.java)

Les noms des classes sont lus dans [`config.txt`](partie1-ioc-spring/src/main/resources/config.txt),
puis les objets sont créés et reliés par **réflexion** :

```
net.sofbt.ext.DaoImplV2
net.sofbt.metier.MetierImpl
```

```java
Class<?> cDao = Class.forName(scanner.nextLine());
IDao dao = (IDao) cDao.getConstructor().newInstance();

Class<?> cMetier = Class.forName(scanner.nextLine());
IMetier metier = (IMetier) cMetier.getConstructor().newInstance();

Method setDao = cMetier.getMethod("setDao", IDao.class);
setDao.invoke(metier, dao);
```

Pour changer d'implémentation, il suffit de modifier `config.txt` : **aucune recompilation**.

```
Version capteurs
Résultat = 190.87526861687965
```

#### c. Avec le framework Spring

Dépendances Maven : `spring-core`, `spring-context`, `spring-beans`.

**Version XML** — [`config.xml`](partie1-ioc-spring/src/main/resources/config.xml) +
[`PresSpringXML`](partie1-ioc-spring/src/main/java/net/sofbt/presentation/PresSpringXML.java)

```xml
<bean id="dao" class="net.sofbt.dao.DaoImpl"/>

<bean id="metier" class="net.sofbt.metier.MetierImpl">
    <property name="dao" ref="dao"/>   <!-- injection via le setter -->
</bean>
```

```java
ApplicationContext context = new ClassPathXmlApplicationContext("config.xml");
IMetier metier = (IMetier) context.getBean("metier");
System.out.println("Résultat = " + metier.calcul());
```

**Version annotations** — [`PresSpringAnnotation`](partie1-ioc-spring/src/main/java/net/sofbt/presentation/PresSpringAnnotation.java)

Les classes sont annotées avec `@Component`, la dépendance avec `@Autowired`. Comme deux beans
implémentent `IDao` (`dao` et `dao2`), `@Qualifier("dao")` indique lequel injecter.

```java
ApplicationContext context = new AnnotationConfigApplicationContext("net.sofbt");
IMetier metier = context.getBean(IMetier.class);
System.out.println("Résultat = " + metier.calcul());
```

---

## Partie 2 — Mini framework d'injection des dépendances

Un mini framework similaire à Spring IoC, sans aucune dépendance à Spring.
Il propose deux conteneurs qui implémentent la même interface :

```java
public interface ApplicationContext {
    Object getBean(String id);
    <T> T getBean(Class<T> type);
}
```

### Structure

```
net.sofbt.minidi
├── annotations
│   ├── Component        @Component("id") : déclare un bean
│   ├── Autowired        sur un constructeur, un setter ou un attribut
│   └── Qualifier        choisit le bean à injecter quand il y a plusieurs candidats
├── xml                  classes JAXB (mapping Objet/XML)
│   ├── Beans            <beans>
│   ├── BeanDefinition   <bean id class> + constructor-arg / property / field
│   └── InjectionDefinition   ref="..." ou value="..."
└── context
    ├── ApplicationContext
    ├── AbstractApplicationContext     registre des singletons, getBean, conversions
    ├── AnnotationApplicationContext   version annotations
    ├── XmlApplicationContext          version XML
    └── BeansException
```

### 1. Configuration XML avec JAX Binding (OXM)

Le fichier XML est transformé en objets Java par **JAXB** (`jakarta.xml.bind`) grâce aux annotations
`@XmlRootElement`, `@XmlElement` et `@XmlAttribute` :

```java
@XmlRootElement(name = "beans")
@XmlAccessorType(XmlAccessType.FIELD)
public class Beans {
    @XmlElement(name = "bean")
    private List<BeanDefinition> beans = new ArrayList<>();
}
```

```java
Beans beans = (Beans) JAXBContext.newInstance(Beans.class)
        .createUnmarshaller().unmarshal(in);
```

Fichier de configuration [`beans.xml`](partie2-mini-framework/src/main/resources/beans.xml) :

```xml
<beans>
    <bean id="dao" class="net.sofbt.demo.dao.DaoImpl"/>
    <bean id="dao2" class="net.sofbt.demo.dao.DaoImplV2"/>

    <!-- a- constructeur -->
    <bean id="metierConstructeur" class="net.sofbt.demo.metier.MetierConstructeur">
        <constructor-arg ref="dao"/>
    </bean>

    <!-- b- setter -->
    <bean id="metierSetter" class="net.sofbt.demo.metier.MetierSetter">
        <property name="dao" ref="dao2"/>
    </bean>

    <!-- c- attribut (Field) -->
    <bean id="metierAttribut" class="net.sofbt.demo.metier.MetierAttribut">
        <field name="dao" ref="dao"/>
    </bean>
</beans>
```

Chaque injection accepte soit `ref` (un autre bean), soit `value` (une valeur littérale convertie vers
`String`, `int`, `long`, `double`, `float` ou `boolean`).

```java
ApplicationContext context = new XmlApplicationContext("beans.xml");
IMetier metier = (IMetier) context.getBean("metierSetter");
```

### 2. Configuration par annotations

`AnnotationApplicationContext` **scanne le classpath** (dossiers et fichiers JAR) du package donné,
enregistre toutes les classes annotées `@Component`, puis crée chaque bean et injecte ses dépendances.

```java
ApplicationContext context = new AnnotationApplicationContext("net.sofbt.demo");
```

Résolution d'une dépendance :
- avec `@Qualifier("id")` → le bean portant cet identifiant ;
- sinon **par type** → l'unique bean compatible (erreur explicite si aucun ou plusieurs candidats).

### 3. Les trois modes d'injection

**a. Constructeur**

```java
@Component("metierConstructeur")
public class MetierConstructeur implements IMetier {
    private final IDao dao;

    @Autowired
    public MetierConstructeur(@Qualifier("dao") IDao dao) { this.dao = dao; }
}
```

Le framework choisit le constructeur annoté `@Autowired` (ou l'unique constructeur, ou le
constructeur sans argument), résout chacun de ses paramètres, puis appelle `newInstance(args)`.

**b. Setter**

```java
@Component("metierSetter")
public class MetierSetter implements IMetier {
    private IDao dao;

    @Autowired
    @Qualifier("dao2")
    public void setDao(IDao dao) { this.dao = dao; }
}
```

Les méthodes annotées `@Autowired` sont invoquées par réflexion (`method.invoke(bean, dep)`).

**c. Attribut (accès direct au Field)**

```java
@Component("metierAttribut")
public class MetierAttribut implements IMetier {
    @Autowired
    @Qualifier("dao")
    private IDao dao;
}
```

L'attribut, même `private`, est rendu accessible (`field.setAccessible(true)`) puis affecté
directement avec `field.set(bean, dep)`, sans passer par un setter.

### Fonctionnement interne

1. Lecture des définitions (fichier XML via JAXB, ou scan des `@Component`).
2. Pour chaque bean : création via le constructeur (avec ses dépendances résolues récursivement),
   enregistrement comme singleton, puis injection des attributs et des setters.
3. Les beans sont des **singletons** : `getBean` renvoie toujours la même instance.
4. Une dépendance circulaire entre constructeurs est détectée et signalée par une `BeansException`.

### Résultat d'exécution — [`PresAnnotations`](partie2-mini-framework/src/main/java/net/sofbt/demo/presentation/PresAnnotations.java)

```
metierConstructeur :
  -> Version base de données
  Résultat = 46.0
metierSetter :
  -> Version capteurs
  Résultat = 24.0
metierAttribut :
  -> Version base de données
  Résultat = 46.0
```

Chaque bean métier a reçu la bonne implémentation de `IDao` selon son mode d'injection.
[`PresXml`](partie2-mini-framework/src/main/java/net/sofbt/demo/presentation/PresXml.java) produit
la même chose à partir de `beans.xml`.

---

## Exécution

Prérequis : JDK 17+ et Maven.

```bash
cd partie1-ioc-spring
mvn compile exec:java -Dexec.mainClass=net.sofbt.presentation.PresSpringAnnotation

cd ../partie2-mini-framework
mvn compile exec:java -Dexec.mainClass=net.sofbt.demo.presentation.PresAnnotations
mvn compile exec:java -Dexec.mainClass=net.sofbt.demo.presentation.PresXml
```

## Conclusion

- Le **couplage faible** (dépendre d'interfaces) rend l'application ouverte à l'extension :
  `DaoImplV2` a été ajoutée sans modifier `MetierImpl`.
- L'instanciation **dynamique** permet de changer d'implémentation sans recompiler.
- **Spring** automatise la création des objets et l'injection, via XML ou via annotations.
- Le **mini framework** reproduit ce mécanisme avec la réflexion Java et JAXB, et supporte l'injection
  par constructeur, par setter et par attribut.
