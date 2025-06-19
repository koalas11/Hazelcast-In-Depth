= Funzionalità Avanzate di Hazelcast

Questo capitolo esplora le capacità avanzate di Hazelcast che permettono applicazioni distribuite robuste, sicure e ad alte prestazioni.

== Serializzazione

La serializzazione è un aspetto critico dell'architettura distribuita di Hazelcast. Quando i dati vengono memorizzati in un cluster Hazelcast o inviati tra i nodi, devono essere serializzati in un formato binario per la trasmissione e deserializzati alla ricezione.

Hazelcast fornisce diverse opzioni di serializzazione con differenti compromessi tra prestazioni, facilità d'uso e flessibilità:

- *Serializzazione integrata*: Serializzazione Java predefinita e versioni ottimizzate
- *Serializzazione Compact*: Serializzazione basata su schema con supporto per il versioning
- *HazelcastJsonValue*: Supporto nativo per oggetti JSON
- *Serializzazione personalizzata*: Implementazione di serializzatori personalizzati
- *Formati esterni*: Supporto per formati come Avro, Protobuf e altri

#figure(
  table(
    columns: (auto, auto, auto),
    align: center + horizon,
    inset: 10pt,
    [*Metodo di Serializzazione*], [*Prestazioni*], [*Caso d'uso*],
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
@Compact
public class Dipendente implements Serializable {
    private int id;
    private String nome;
    private Dipartimento dipartimento;

    // Getter, setter, costruttori
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

=== Serializzazione Personalizzata

Quando i meccanismi di serializzazione integrati non soddisfano le tue esigenze, puoi implementare serializzatori personalizzati:

```java
public class DipendenteSerializer implements StreamSerializer<Dipendente> {
    @Override
    public void write(ObjectDataOutput out, Dipendente dipendente) throws IOException {
        out.writeInt(dipendente.getId());
        out.writeString(dipendente.getNome());
        // Scrivere altri campi
    }

    @Override
    public Dipendente read(ObjectDataInput in) throws IOException {
        int id = in.readInt();
        String nome = in.readString();
        // Leggere altri campi e costruire Dipendente
        return new Dipendente(id, nome, ...);
    }

    @Override
    public int getTypeId() {
        return 1000; // Identificatore unico per questo serializzatore
    }
}
```

=== Altre Opzioni di Serializzazione

Hazelcast supporta diversi meccanismi di serializzazione aggiuntivi:

- *Identified Data Serialization*: Simile a Java Externalizable ma più efficiente
- *Portable Serialization*: Serializzazione consapevole della versione con interrogazioni sui campi
- *Global Serialization*: Serializzazione di fallback per tutti gli altri oggetti
- *Librerie Esterne*: Integrazione con Avro, Protobuf e framework simili

=== Configurazione dei Serializzatori

I serializzatori possono essere configurati programmaticamente o tramite file di configurazione:

```java
Config config = new Config();
SerializationConfig serializationConfig = config.getSerializationConfig();

// Configurare la Serializzazione Compact
serializationConfig.getCompactSerializationConfig().setEnabled(true);

// Registrare un serializzatore personalizzato
serializationConfig.addSerializerConfig(
    new SerializerConfig()
        .setImplementation(new DipendenteSerializer())
        .setTypeClass(Dipendente.class)
);
```

Configurazione XML:

```xml
<hazelcast>
    <serialization>
        <compact-serialization enabled="true"/>
        <serializers>
            <serializer type-class="com.esempio.Dipendente">
                com.esempio.DipendenteSerializer
            </serializer>
        </serializers>
    </serialization>
</hazelcast>
```

== Ascolto degli Eventi

Hazelcast fornisce un sistema di eventi completo che consente alle applicazioni di reagire a vari cambiamenti nello stato del cluster e nei dati. I listener di eventi permettono di costruire applicazioni reattive che rispondono ai cambiamenti in tempo reale.

Vantaggi principali del sistema di eventi di Hazelcast:
- Propagazione distribuita degli eventi
- Consegna affidabile degli eventi
- Capacità di filtraggio
- Opzioni di gestione sia sincrone che asincrone

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

Puoi anche ascoltare i cambiamenti di stato del cluster:

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

=== Configurazione Globale degli Eventi

Puoi configurare il comportamento degli eventi globalmente in Hazelcast:

```java
Config config = new Config();
// Configurare il numero di thread per gli eventi
config.setProperty("hazelcast.event.thread.count", "5");
// Configurare la capacità della coda degli eventi
config.setProperty("hazelcast.event.queue.capacity", "1000000");
```

=== Eventi degli Oggetti Distribuiti

Le strutture dati distribuite di Hazelcast emettono vari eventi che puoi ascoltare:

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

Puoi anche filtrare gli eventi utilizzando i predicati:

```java
mappa.addEntryListener(entryListener,
    Predicates.sql("età > 30"), true);
```

== Funzionalità di Sicurezza

Hazelcast fornisce un framework di sicurezza completo per proteggere i tuoi dati e controllare l'accesso al cluster.

=== Autenticazione e Autorizzazione

Hazelcast supporta molteplici meccanismi di autenticazione:

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

=== Crittografia Simmetrica

Per ambienti in cui TLS/SSL non è disponibile:

```xml
<hazelcast>
    <network>
        <symmetric-encryption enabled="true">
            <algorithm>AES/CBC/PKCS5Padding</algorithm>
            <salt>thesalt</salt>
            <password>thepass</password>
            <iteration-count>19</iteration-count>
        </symmetric-encryption>
    </network>
</hazelcast>
```

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

Hazelcast fornisce capacità di monitoraggio complete attraverso il suo Management Center.

=== Panoramica del Management Center

Il Management Center è uno strumento web per il monitoraggio e la gestione dei cluster Hazelcast, che offre:

- Metriche e statistiche in tempo reale
- Visualizzazione della topologia del cluster
- Ispezione delle strutture dati
- Esecuzione e ottimizzazione delle query
- Gestione della sicurezza
- Configurazione del cluster

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

== Pattern di Deployment nel Cloud

Hazelcast offre opzioni di deployment flessibili per ambienti cloud.

=== Integrazione con Kubernetes

Distribuisci Hazelcast su Kubernetes con auto-discovery:

```yaml
apiVersion: hazelcast.com/v1alpha1
kind: Hazelcast
metadata:
  name: hz-cluster
spec:
  clusterSize: 3
  repository: hazelcast/hazelcast
  version: "5.3.1"
  resources:
    requests:
      memory: 1Gi
      cpu: 500m
    limits:
      memory: 2Gi
```

Abilita il plugin Kubernetes:

```java
Config config = new Config();
config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
config.getNetworkConfig().getJoin().getKubernetesConfig()
      .setEnabled(true)
      .setProperty("namespace", "default")
      .setProperty("service-name", "hz-service");
```

=== Pattern Cloud-Native

Implementa il pattern sidecar per applicazioni cloud-native:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mia-applicazione
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: applicazione
        image: myapp:latest
      - name: hazelcast
        image: hazelcast/hazelcast:5.3.1
        ports:
        - containerPort: 5701
```

=== Deployment Multi-Regione

Configura la replica WAN per cluster multi-regione:

```java
Config config = new Config();
WanReplicationConfig wanConfig = new WanReplicationConfig();
wanConfig.setName("londra-a-newyork");

WanBatchPublisherConfig publisherConfig = new WanBatchPublisherConfig();
publisherConfig.setClusterName("newyork-cluster")
               .setTargetEndpoints("10.28.10.1:5701,10.28.10.2:5701");

wanConfig.addWanPublisherConfig(publisherConfig);
config.addWanReplicationConfig(wanConfig);

// Collega la mappa alla replica WAN
config.getMapConfig("clienti")
      .setWanReplicationRef(new WanReplicationRef("londra-a-newyork"));
```

=== Integrazione Serverless

Usa i client Hazelcast nelle funzioni serverless:

```java
public class LambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static HazelcastInstance hz;

    static {
        ClientConfig config = new ClientConfig();
        config.getNetworkConfig().addAddress("hz-cluster.internal:5701");
        hz = HazelcastClient.newHazelcastClient(config);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        IMap<String, Cliente> clienti = hz.getMap("clienti");
        // Elabora la richiesta usando Hazelcast
        return new APIGatewayProxyResponseEvent().withStatusCode(200);
    }
}
```

== Test delle Applicazioni

Il test di applicazioni distribuite presenta sfide uniche. Hazelcast fornisce diversi strumenti e approcci per facilitare il testing:

- *Unit Testing*: Test dei componenti in isolamento
- *Integration Testing*: Test con un'istanza Hazelcast reale
- *Framework di Test per Job*: Specializzato per testare i job Jet
- *Hazelcast Simulator*: Test di performance e stress

=== Test dei Job

Hazelcast Jet include un framework di testing specificamente progettato per validare i job di elaborazione:

```java
@Test
public void testJobProcessing() {
    JetInstance jet = Jet.newJetInstance();
    try {
        Pipeline pipeline = buildPipeline();

        JobConfig config = new JobConfig();
        Job job = jet.newJob(pipeline, config);

        // Attendere il completamento del job
        job.join();

        // Verificare i risultati
        IMap<String, Long> risultati = jet.getMap("risultati");
        assertEquals(42L, risultati.get("chiaveAttesa").longValue());
    } finally {
        jet.shutdown();
    }
}
```

Per scenari più complessi, la classe `TestSupport` fornisce utility utili:

```java
@Test
public void testWithMockSource() {
    TestSupport.testJobEvents(
        createPipeline(),
        Arrays.asList("input1", "input2"),
        Arrays.asList("atteso1", "atteso2")
    );
}
```

=== Hazelcast Simulator

Hazelcast Simulator è uno strumento di test di livello produzione progettato per:
- Test di performance
- Test di stress
- Test di stabilità
- Test di scenari di fallimento

Caratteristiche principali:
- Generazione di carico realistico
- Iniezione di guasti
- Statistiche dettagliate sulle prestazioni
- Esecuzione automatica dei test

Esempio di test con simulator:

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

Esecuzione del test simulator:

```bash
simulator-coordinator --duration 2h \
  --members 4 \
  --clients 10 \
  --workerVmOptions "-Xms2g -Xmx2g" \
  --tests MapStressTest
```

Il Simulator fornisce report dettagliati e metriche di performance dopo il completamento del test.
