= Funzionalità Avanzate di Hazelcast

== Serializzazione

La serializzazione è un aspetto critico dell'architettura distribuita di Hazelcast. Quando i dati vengono memorizzati in un cluster Hazelcast o inviati tra i nodi, devono essere serializzati in un formato binario per la trasmissione e deserializzati alla ricezione.

Per i tipi primitivi questo processo è gestito automaticamente da Hazelcast, ma per gli oggetti complessi è necessario implementare la serializzazione in modo esplicito.

Hazelcast fornisce diverse opzioni di serializzazione con differenti compromessi tra prestazioni, facilità d'uso e flessibilità, consigliando di utilizzare la serializzazione Compact per la maggior parte dei casi d'uso:

- *Serializzazione integrata*: Serializzazione Java predefinita
- *Serializzazione Compact*: Serializzazione basata su schema con supporto per il versioning e prestazioni elevate
- *HazelcastJsonValue*: Supporto nativo per oggetti JSON
- *Serializzazione personalizzata*: Implementazione di serializzatori personalizzati
- *Formati esterni*: Supporto per formati come Protobuf e altri

#figure(
  table(
    columns: (auto, auto, auto),
    align: center + horizon,
    inset: 10pt,
    table.header([Metodo di Serializzazione], [Prestazioni], [Caso d'uso]),
    [Serializzazione Java], [Più basse], [Casi semplici, prototipazione],
    [Serializzazione Compact], [Più alte], [Produzione, schemi in evoluzione],
    [JSON], [Medie], [Applicazioni web, integrazione con sistemi esterni],
    [Personalizzata], [Alte], [Formati specializzati, sistemi legacy],
  ),
  caption: [Confronto tra Opzioni di Serializzazione],
)

=== Serializzazione Compact

La Serializzazione Compact è la strategia di serializzazione raccomandata da Hazelcast per la maggior parte dei casi d'uso. Offre:

- Formato binario basato su schema
- Compatibilità all'indietro e in avanti
- Supporto per l'evoluzione degli schemi
- Alte prestazioni con basso utilizzo di memoria

```java
public class Dipendente {
    private int id;
    private String nome;
    private String dipartimento;

    // Getter, setter, costruttori
}

public class DipendenteSerializer implements CompactSerializer<Dipendente> {
    @Override
    public Dipendente read(CompactReader reader) {
        long id = reader.readInt64("id");
        String nome = reader.readString("nome");
        String dipartimento = reader.readString("dipartimento");
        return new Dipendente(id, nome, dipartimento);
    }

    @Override
    public void write(CompactWriter writer, Dipendente dipendente) {
        writer.writeInt64("id", dipendente.getId());
        writer.writeString("nome", dipendente.getNome());
        writer.writeString("dipartimento", dipendente.getDipartimento());
    }

    @Override
    public Class<Dipendente> getCompactClass() {
        return Dipendente.class;
    }

    @Override
    public String getTypeName() {
        return "dipendente";
    }
}
```

La serializzazione Compact funziona con un registro degli schemi che viene mantenuto in tutto il cluster. Quando viene introdotta una nuova versione di classe, Hazelcast gestisce automaticamente la compatibilità.

=== HazelcastJsonValue

Per scenari in cui JSON è il formato preferito, Hazelcast fornisce la classe `HazelcastJsonValue`:

```java
Map<String, HazelcastJsonValue> clienti = hz.getMap("clienti");
String json = "{ \"nome\": \"Giovanni\", \"età\": 35 }";
HazelcastJsonValue jsonValue = new HazelcastJsonValue(json);
clienti.put("cliente1", jsonValue);
```

Vantaggi di `HazelcastJsonValue`:
- Nessuna deserializzazione quando viene utilizzato come chiave o valore di una mappa
- Interrogazioni efficienti con predicati
- Facile integrazione con applicazioni web
- Supporto nativo JSON nelle query SQL

== Ascolto degli Eventi

Hazelcast fornisce un sistema di eventi completo che consente alle applicazioni di reagire a vari cambiamenti nello stato del cluster e nei dati. I listener di eventi permettono di costruire applicazioni reattive che rispondono ai cambiamenti in tempo reale.

=== Eventi del Cluster

Gli eventi del cluster forniscono notifiche sui cambiamenti di appartenenza nel cluster:

```java
hazelcastInstance.getCluster().addMembershipListener(new MembershipListener() {
    @Override
    public void memberAdded(MembershipEvent event) {
        System.out.println("Membro aggiunto: " + event.getMember());
    }

    @Override
    public void memberRemoved(MembershipEvent event) {
        System.out.println("Membro rimosso: " + event.getMember());
    }
});
```

È possibile anche ascoltare i cambiamenti di stato del cluster:

```java
hazelcastInstance.getCluster().addClusterStateListener(event -> {
    System.out.println("Stato del cluster cambiato in: " + event.getNewState());
});
```

=== Listener di Eventi per Client Hazelcast

Gli eventi specifici dei client permettono di tracciare connessioni e disconnessioni dei client:

```java
ClientService clientService = hazelcastInstance.getClientService();
clientService.addClientListener(new ClientListener() {
    @Override
    public void clientConnected(Client client) {
        System.out.println("Client connesso: " + client.getUuid());
    }

    @Override
    public void clientDisconnected(Client client) {
        System.out.println("Client disconnesso: " + client.getUuid());
    }
});
```

=== Eventi degli Oggetti Distribuiti

Le strutture dati distribuite di Hazelcast emettono vari eventi che puoi ascoltare ad esempio:

*Eventi delle Mappe:*
```java
IMap<String, String> mappa = hazelcastInstance.getMap("miaMappa");
mappa.addEntryListener(new EntryAddedListener<String, String>() {
    @Override
    public void entryAdded(EntryEvent<String, String> event) {
        System.out.println("Elemento aggiunto: " + event.getKey() + " -> " + event.getValue());
    }
}, true); // true per includere il valore
```

È possibile anche filtrare gli eventi utilizzando i predicati (disponibile solo per le mappe):

```java
mappa.addEntryListener(entryListener,
    Predicates.sql("età > 30"), true);
```

*Eventi delle Code:*
```java
IQueue<String> coda = hazelcastInstance.getQueue("miaCoda");
coda.addItemListener(new ItemListener<String>() {
    @Override
    public void itemAdded(ItemEvent<String> item) {
        System.out.println("Elemento aggiunto: " + item.getItem());
    }
    @Override
    public void itemRemoved(ItemEvent<String> item) {
        System.out.println("Elemento rimosso: " + item.getItem());
    }
}, true);
```

== Funzionalità di Sicurezza

Hazelcast fornisce un framework di sicurezza completo per proteggere i tuoi dati e controllare l'accesso al cluster nella versione Enterprise.

=== Autenticazione e Autorizzazione

Sono supportati molteplici meccanismi di autenticazione:

```java
Config config = new Config();
SecurityConfig securityConfig = config.getSecurityConfig();

// Abilitare la sicurezza
securityConfig.setEnabled(true);

// Configurare l'autenticazione
securityConfig.setMemberAuthenticationConfig(
    new RealmConfig().setJaasAuthenticationConfig(
        new JaasAuthenticationConfig().setLoginModuleConfigs(
            List.of(new LoginModuleConfig("com.esempio.MyLoginModule", LoginModuleUsage.REQUIRED))
        )
    )
);
```

L'autorizzazione dei client può essere implementata con i permessi:

```java
// Configurazione dei permessi
securityConfig.setClientPermissionConfigs(
    List.of(
        new PermissionConfig(PermissionType.MAP, "clienti", "*"),
        new PermissionConfig(PermissionType.QUEUE, "ordini", "create,read")
    )
);
```

=== Crittografia e TLS/SSL

Comunicazione sicura tra i membri del cluster e i client:

```java
SSLConfig sslConfig = new SSLConfig();
sslConfig.setEnabled(true)
         .setFactoryClassName("com.hazelcast.nio.ssl.BasicSSLContextFactory")
         .setProperty("keyStore", "/path/to/keystore.jks")
         .setProperty("keyStorePassword", "password")
         .setProperty("trustStore", "/path/to/truststore.jks")
         .setProperty("trustStorePassword", "password");

config.getNetworkConfig().setSSLConfig(sslConfig);
```

Per ambienti in cui TLS/SSL non è disponibile, è possibile configurare la crittografia simmetrica.

=== Interceptor di Sicurezza

Implementa logica di sicurezza personalizzata con gli interceptor:

```java
public class MioSecurityInterceptor implements SecurityInterceptor {
    @Override
    public void interceptRequest(Request request) {
        // Implementare logica di sicurezza personalizzata
        if (!isAuthorized(request)) {
            throw new AccessControlException("Accesso non autorizzato");
        }
    }

    @Override
    public void interceptResponse(Response response) {
        // Elaborare la risposta se necessario
    }
}
```

=== Logging di Audit

Traccia eventi relativi alla sicurezza:

```java
config.setProperty("hazelcast.security.audit.enabled", "true");
config.setProperty("hazelcast.security.audit.log.frequency", "10");
```

== Monitoraggio e Management Center

Il Management Center è uno strumento web per il monitoraggio e la gestione dei cluster Hazelcast, che offre:

- Metriche e statistiche in tempo reale
- Visualizzazione della topologia del cluster
- Ispezione delle strutture dati
- Esecuzione e ottimizzazione delle query
- Gestione della sicurezza
- Configurazione del cluster

Oltre a queste funzionalità è possibile anche configurare avvisi e notifiche per condizioni critiche, creare script di monitoraggio personalizzati e integrare il Management Center con strumenti esterni come Prometheus.

Un aspetto però importante da notare è che il Management Center della versione gratuita supporta fino a un massimo di 3 member per cluster.

/*
=== Opzioni di Deployment

Il Management Center può essere distribuito in diversi modi:

```bash
# Deployment con Docker
docker run -p 8080:8080 hazelcast/management-center

# JAR standalone
java -jar hazelcast-management-center-5.3.1.jar
```


=== Integrazione con il Cluster

Connetti il tuo cluster Hazelcast al Management Center:

```java
Config config = new Config();
ManagementCenterConfig mcConfig = config.getManagementCenterConfig();
mcConfig.setEnabled(true);
mcConfig.setUrl("http://localhost:8080/hazelcast-mancenter");
```
*/
/*
=== Monitoraggio Avanzato

Configura il monitoraggio JMX per l'integrazione con strumenti esterni:

```java
config.setProperty("hazelcast.jmx", "true");
```

Raccolta di metriche personalizzate:

```java
MetricsRegistry metricsRegistry = hazelcastInstance.getMetricsRegistry();
metricsRegistry.registerStaticMetrics(new MieMetriche(), "app.miemetriche");
```

=== Avvisi e Notifiche

Configura avvisi per condizioni critiche:

```java
AlertConfig alertConfig = new AlertConfig();
alertConfig.setName("UsoElevatoMemoria")
           .setMetric("memory.usedPercentage")
           .setThreshold(80)
           .setComparison(Comparison.GREATER_THAN)
           .setEnabled(true);

mcConfig.addAlertConfig(alertConfig);
```

=== API Script di Monitoraggio

Crea script di monitoraggio personalizzati:

```javascript
var map = instance.getMap("clienti");
var size = map.size();
print("Dimensione mappa clienti: " + size);

if (size > 10000) {
  sendEmail("admin@esempio.com", "Avviso dimensione mappa", "La mappa clienti ha superato 10K elementi");
}
```
*/

// == Pattern di Deployment nel Cloud

// Hazelcast offre opzioni di deployment flessibili per ambienti cloud.

// === Integrazione con Kubernetes

// Distribuisci Hazelcast su Kubernetes con auto-discovery:

// ```yaml
// apiVersion: hazelcast.com/v1alpha1
// kind: Hazelcast
// metadata:
//   name: hz-cluster
// spec:
//   clusterSize: 3
//   repository: hazelcast/hazelcast
//   version: "5.3.1"
//   resources:
//     requests:
//       memory: 1Gi
//       cpu: 500m
//     limits:
//       memory: 2Gi
// ```

// Abilita il plugin Kubernetes:

// ```java
// Config config = new Config();
// config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
// config.getNetworkConfig().getJoin().getKubernetesConfig()
//       .setEnabled(true)
//       .setProperty("namespace", "default")
//       .setProperty("service-name", "hz-service");
// ```

// === Pattern Cloud-Native

// Implementa il pattern sidecar per applicazioni cloud-native:

// ```yaml
// apiVersion: apps/v1
// kind: Deployment
// metadata:
//   name: mia-applicazione
// spec:
//   replicas: 3
//   template:
//     spec:
//       containers:
//       - name: applicazione
//         image: myapp:latest
//       - name: hazelcast
//         image: hazelcast/hazelcast:5.3.1
//         ports:
//         - containerPort: 5701
// ```

// === Deployment Multi-Regione

// Configura la replica WAN per cluster multi-regione:

// ```java
// Config config = new Config();
// WanReplicationConfig wanConfig = new WanReplicationConfig();
// wanConfig.setName("londra-a-newyork");

// WanBatchPublisherConfig publisherConfig = new WanBatchPublisherConfig();
// publisherConfig.setClusterName("newyork-cluster")
//                .setTargetEndpoints("10.28.10.1:5701,10.28.10.2:5701");

// wanConfig.addWanPublisherConfig(publisherConfig);
// config.addWanReplicationConfig(wanConfig);

// // Collega la mappa alla replica WAN
// config.getMapConfig("clienti")
//       .setWanReplicationRef(new WanReplicationRef("londra-a-newyork"));
// ```

// === Integrazione Serverless

// Usa i client Hazelcast nelle funzioni serverless:

// ```java
// public class LambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
//     private static HazelcastInstance hz;

//     static {
//         ClientConfig config = new ClientConfig();
//         config.getNetworkConfig().addAddress("hz-cluster.internal:5701");
//         hz = HazelcastClient.newHazelcastClient(config);
//     }

//     @Override
//     public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
//         IMap<String, Cliente> clienti = hz.getMap("clienti");
//         // Elabora la richiesta usando Hazelcast
//         return new APIGatewayProxyResponseEvent().withStatusCode(200);
//     }
// }
// ```

== Test delle Applicazioni

L'esecuzione di test ed esperimenti in ambienti distribuiti rappresenta una sfida significativa, in particolare per quanto riguarda la sincronizzazione di stati degradati con operazioni sui dati. Hazelcast offre diversi strumenti e approcci per facilitare la gestione di tali sfide:

- *Unit Testing*: Test di componenti in isolamento.
- *Integration Testing*: Test con un'istanza Hazelcast reale.
- *Framework di Test per Job*: Specializzato per il test di job Jet.
- *Hazelcast Simulator*: Test di performance e stress.

=== Test dei Job

Hazelcast Jet include un framework di testing specificamente progettato per validare i job di elaborazione, per utilizzarlo basta estendere la classe `JetTestSupport` che fornisce metodi utili per la creazione del cluster, gestione dei job e verifica dei risultati.

```java
class MainTest extends JetTestSupport {

    @AfterEach
    public void after() {
        Hazelcast.shutdownAll();
    }

    @Test
    public void testBase() {
        HazelcastInstance instance1 = createHazelcastInstance();
        HazelcastInstance instance2 = createHazelcastInstance();

        assertClusterSize(2, instance1, instance2);

        Pipeline p = Pipeline.create();
        p.readFrom(TestSources.items(1, 2, 3, 4))
        .writeTo(Sinks.logger());

        instance1.getJet().newJob(p).join();
    }
}
```

=== Hazelcast Simulator: Strumento Avanzato per il Testing

Hazelcast Simulator è una piattaforma di testing progettata per valutare in modo rigoroso le prestazioni e l'affidabilità di Hazelcast. Pensato per ambienti di produzione, consente di eseguire test complessi con una configurazione personalizzabile e un elevato livello di automazione, ed è lo strumento utilizzato da Hazelcast stesso per testare le nuove versioni del prodotto.

Tipologie di test supportate:
- *Test di Performance*: Misurazione dettagliata di throughput, latenza, e capacità di scalabilità in vari scenari di carico.
- *Test di Stress*: Valutazione della robustezza del sistema spingendo le risorse oltre i limiti operativi per identificare possibili colli di bottiglia.
- *Test di Stabilità*: Esecuzione di carichi prolungati per analizzare la tenuta nel tempo, l'efficienza nella gestione delle risorse e la prevenzione di memory leak.
- *Test di Scenari di Fallimento (Fault Injection)*: Simulazione di guasti a livello di rete, hardware o software per verificare le capacità di failover e la resilienza dell'infrastruttura.

Caratteristiche tecniche principali:
- *Generazione di Carico Realistico*: Implementazione di modelli di traffico complessi e scenari di workload personalizzati per riprodurre condizioni operative reali.
- *Iniezione di Guasti Controllata*: Integrazione di meccanismi per introdurre errori sistematici e casuali al fine di testare le strategie di recupero e tolleranza ai guasti.
- *Raccolta di Statistiche Avanzate*: Monitoraggio continuo di KPI critici come tempi di risposta, utilizzo delle risorse, throughput e latenza, con report dettagliati per l'analisi delle prestazioni.
- *Automazione del Ciclo di Test*: Framework per l'esecuzione automatizzata di test su larga scala, con possibilità di integrazione in pipeline CI/CD per il testing continuo.

Esempio di test:

```java
public class MapStressTest extends HazelcastTest {
    private IMap<Integer, String> mappa;

    @Setup
    public void setup() {
        mappa = targetInstance.getMap("mappaStress");
    }

    @TimeStep(prob = 0.5)
    public void put(ThreadState state) {
        int chiave = state.randomInt(10_000);
        mappa.put(chiave, "valore-" + chiave);
    }

    @TimeStep(prob = 0.5)
    public void get(ThreadState state) {
        int chiave = state.randomInt(10_000);
        mappa.get(chiave);
    }

    public class ThreadState extends BaseThreadState {
        // Stato locale del thread qui
    }
}
```

Il Simulator fornisce report dettagliati e metriche di performance dopo il completamento del test.

== Commenti

Hazelcast offre una serie di funzionalità avanzate che lo rendono idoneo per applicazioni distribuite complesse. La serializzazione efficiente, il sistema di eventi reattivo, le robuste capacità di sicurezza e il potente Management Center sono solo alcune delle caratteristiche che distinguono Hazelcast nel panorama delle tecnologie in-memory. È inoltre possibile utilizzare Hazelcast senza la limitazione di 3 nodi massimi nel caso in cui non sia necessario il Management Center, ma solamente la piattaforma stessa.
