#import "../packages.typ": codly

= Architettura di Hazelcast

== Panoramica Architetturale

Hazelcast è una piattaforma di computing distribuito sviluppata in Java che combina storage e processing in un unico sistema. La sua architettura è progettata per essere scalabile orizzontalmente, resiliente ai guasti e ad alte prestazioni. In questo capitolo, esploreremo i componenti fondamentali dell'architettura di Hazelcast e come interagiscono tra loro.

== Architettura Distribuita

=== Cluster e Member

Un cluster Hazelcast è composto da più istanze chiamate *member* (membri). Questa terminologia è importante nell'ecosistema Hazelcast, dove i nodi del cluster sono sempre indicati come member. Questi member comunicano tra loro tramite rete e formano un sistema distribuito peer-to-peer.

Ogni member è un peer con pari diritti: non ci sono master/slave che rimangano single point of failure. All'avvio del cluster Hazelcast elegge un “cluster coordinator” (inizialmente il member più anziano attraverso protocolli di gossip) che serializza le modifiche allo stato del cluster (join/leave, partizionamento, merge split-brain). Se il coordinator cade, ne viene subito eletto un altro, senza interruzione del servizio.

Un aspetto fondamentale è che ogni member è consapevole dell'esistenza di tutti gli altri member nel cluster, mantenendo una vista coerente dello stato del cluster attraverso protocolli di membership distribuiti.

=== Lite Members

Hazelcast offre anche un tipo di nodo speciale chiamato *Lite Member*. I Lite Member rappresentano un'opzione intermedia tra i member completi e i client:

- Come i member normali, partecipano al cluster e sono visibili nella membership list
- A differenza dei member normali, non possiedono partizioni di dati e non memorizzano dati
- Eseguono operazioni computazionali ma delegano lo storage ai member regolari

I Lite Member sono particolarmente utili in scenari specifici:

- *Operation Owners dedicati*: Possono essere utilizzati come nodi dedicati per eseguire operazioni distribuite (come query o task Executor) senza il carico di gestione dei dati
- *Pattern Edge/Worker*: In architetture che separano i nodi edge (che gestiscono le connessioni client) dai nodi worker (che archiviano i dati)
- *Endpoint per la gestione*: Come host per Management Center o altri strumenti di monitoraggio
- *Nodi computazionali*: Per eseguire job Jet senza partecipare allo storage dei dati

==== Vantaggi dei Lite Member

- *Risorse ottimizzate*: Consumano meno memoria poiché non memorizzano partizioni di dati
- *Migrazione dati ridotta*: La loro aggiunta o rimozione non causa ribilanciamento delle partizioni
- *Separazione dei ruoli*: Permettono di separare le responsabilità di calcolo e storage
- *Deployment flessibile*: Possono essere implementati o rimossi senza influenzare significativamente la stabilità del cluster

=== Partizioni e Distribuzione dei Dati

Hazelcast suddivide i dati in unità chiamate *partizioni*. Per impostazione predefinita, un cluster Hazelcast è configurato con 271 partizioni. Ogni member del cluster è responsabile di un sottoinsieme di queste partizioni.

Quando si inseriscono dati in una struttura dati distribuita (come una Map), avvengono questi step:

1. Calcolato l'hash della chiave
2. Applicata l'operazione modulo sul numero totale di partizioni per identificare la partizione di destinazione
3. Localizzato il member che possiede quella partizione
4. Archiviato il dato nel member corrispondente

Questo meccanismo di partizionamento garantisce una distribuzione uniforme dei dati e del carico di lavoro attraverso il cluster. Inoltre, la distribuzione delle partizioni viene ricalcolata automaticamente quando i member entrano o escono dal cluster, in un processo chiamato "repartitioning".

Il sistema di partizionamento include diverse sofisticazioni:

- *Distribuzione deterministica delle chiavi*: L'algoritmo di hashing utilizzato garantisce che la stessa chiave venga sempre assegnata alla stessa partizione, permettendo operazioni di ricerca efficaci.

- *Bilanciamento dinamico del carico*: Durante il repartitioning, viene utilizzato un algoritmo proprietario che minimizza il movimento dei dati, trasferendo solo le partizioni necessarie.

- *Consistenza delle partizioni*: Viene mantenuto un "partition table" distribuito che viene sincronizzato tra tutti i member per garantire una vista coerente dell'assegnazione delle partizioni.

- *Ottimizzazioni di accesso*: Le operazioni che coinvolgono chiavi nella stessa partizione sono eseguite in un'unica chiamata di rete, riducendo la latenza.

=== Backup e Replica

Per garantire l'alta disponibilità, Hazelcast crea automaticamente copie di backup delle partizioni. Il numero di backup è configurabile, con 1 come predefinito. Questi backup sono distribuiti su member distinti, assicurando che in caso di fallimento di un member, i dati rimangano disponibili.

Il processo di backup è configurabile con diverse strategie:
- *Synchronous backup*: l'operazione di scrittura attende la conferma che il backup sia stato completato
- *Asynchronous backup*: l'operazione di scrittura non attende la conferma del backup, offrendo maggiore throughput ma minori garanzie
- *Zero backup*: massime prestazioni ma senza tolleranza ai guasti

Il sistema di replicazione include diverse sofisticazioni:

- *Replica anti-affinità*: Le copie di backup vengono distribuite su nodi fisici diversi quando possibile, massimizzando la resilienza.

- *Replicazione intelligente*: Durante il failover, viene selezionata la replica più velocemente accessibile per minimizzare l'impatto sulle prestazioni.

- *Replica consistente*: Il sistema garantisce che le operazioni vengano applicate nello stesso ordine sia sulla partizione primaria che sui backup.

- *Healing automatico*: Quando un member viene ripristinato o un nuovo member si unisce, vengono ribilanciate automaticamente le partizioni e ricrea i backup mancanti.

== Topologia di Rete

=== Comunicazione tra Member

I member Hazelcast comunicano tra loro attraverso un protocollo binario ottimizzato che opera su TCP/IP. Il cluster mantiene connessioni dirette tra tutti i membri per garantire comunicazioni veloci ed efficienti.

La comunicazione interna utilizza:
- Socket TCP per trasferimento dati affidabile
- Heartbeat per rilevare member inattivi e misurare la latenza
- Messaggi di gossip per propagare informazioni sullo stato del cluster
- Protocolli ottimizzati per minimizzare il traffico di rete

=== Meccanismi di Discovery

Vengono offerti diversi meccanismi per la scoperta dei member in un cluster:

- *TCP/IP*: configurazione manuale degli indirizzi IP e porte dei member. Ideale per ambienti con indirizzi IP statici o per test in ambienti di sviluppo.

- *Multicast*: rilevamento automatico in reti locali attraverso il protocollo multicast. I member inviano pacchetti multicast che vengono ricevuti da altri member nella stessa rete. Sebbene semplice da configurare, molti ambienti di produzione disabilitano il multicast.

- *Cloud Discovery*: integrazioni specifiche per ambienti cloud come AWS, Azure, GCP. Utilizza l'API del cloud in uso per scoprire le istanze che sono etichettate come member Hazelcast.

- *Kubernetes Discovery*: integrazione nativa con Kubernetes che utilizza l'API Kubernetes per scoprire i pod che sono etichettati come member Hazelcast.

// - *JGroups*: utilizza JGroups come meccanismo di trasporto e discovery.

- *Consul, Zookeeper, Eureka*: integrazioni con sistemi di service discovery comuni nell'ecosistema enterprise.

== Architettura Client-Server

Hazelcast supporta due modelli di distribuzione principali: modalità Embedded e modalità Client-Server.

=== Modalità Embedded

In questa modalità, l'applicazione e uno o più member condividono lo stesso JVM. L'applicazione diventa un member del cluster con tutti i diritti, gestendo partizioni e backup.

==== Vantaggi dell'embedded mode
- Accesso ai dati con latenza minima (accesso in-memory)
- Nessuna serializzazione/deserializzazione per operazioni locali
- Partecipazione attiva nel cluster

==== Svantaggi
- L'applicazione deve essere scritta in Java
- Maggiore consumo di risorse
- Il ciclo di vita e lo scaling dell'applicazione e del member Hazelcast sono strettamente legati
- Sconsigliata per ambienti di produzione

#codly.codly-disable()
```
Applicazione Java <-> Hazelcast Embedded Member <-> Hazelcast Cluster
```

=== Modalità Client-Server

In questa modalità, i member svolgono il ruolo di server mentre, l'applicazione utilizza un client leggero che si connette a un cluster Hazelcast esterno:

```
Applicazione Java/C++/C#/Node.js/Go <-> Hazelcast Client <-> Hazelcast Cluster
```
#codly.codly-enable()

I client sono leggeri, non archiviano dati e non partecipano alla distribuzione delle partizioni. Sono offerti client per molteplici linguaggi: Java, .NET, C++, Node.js, Python e Go.

Vantaggi della modalità client-server:
- Supporto per applicazioni scritte in linguaggi e tecnologie differenti
- Separazione tra infrastruttura dati e logica applicativa
- Client leggeri con basso overhead
- Possibilità di riavviare o scalare l'applicazione senza influire sul cluster

== In-Memory Storage

Hazelcast è fondamentalmente una piattaforma di computing in-memory, con caratteristiche architetturali specifiche per ottimizzare l'utilizzo della memoria:

=== Architettura di Storage

- *Memory Management nativo (Enterprise)*: Hazelcast Enterprise integra un off-heap memory manager alternativo al GC della JVM, riducendo la pressione sul garbage collector e i picchi di pausa. Nella versione Open Source si fa invece affidamento al GC standard e al formato binario in-memory.

- *Persistenza su disco (Enterprise)*: Sistema di persistenza su disco basato su file logici append-only con garbage collection incrementale e ottimizzazione I/O per minimizzare l'impatto sulle prestazioni.

- *Near Cache*: Meccanismo di caching lato client che mantiene copie locali dei dati frequentemente acceduti, riducendo drasticamente la latenza di lettura.

- *Storage Format ottimizzato*: I dati sono archiviati in un formato binario ottimizzato per minimizzare l'overhead di memorizzazione e accelerare le operazioni di serializzazione/deserializzazione.

=== Persistenza e Durabilità

Hazelcast Enterprise offre la funzionalità di persistenza dei dati su disco e backup automatico, garantendo la durabilità dei dati anche in caso di guasti hardware o crash del sistema.

Questo è utile quando si vuole garantire che i dati siano disponibili anche dopo un riavvio del cluster o in scenari di disaster recovery.

Inoltre è possibile configurare Hazelcast per utilizzare storage esterni come database relazionali o NoSQL, integrando così la piattaforma e il sistema di caching in-memory di Hazelcast con soluzioni di storage persistente. L'utilizzo di storage esterni è possibile anche con la versione open-source, il che la rende un appetibile alternativa per alcuni tipi di utilizzo.

== Elasticità e Scalabilità

L'architettura è progettata per essere elastica, permettendo di:

- *Hot Scaling*: aggiungere member a runtime senza interruzioni del servizio.
- *Graceful Shutdown*: rimuovere member in modo sicuro con migrazione automatica dei dati.
- *Data Rebalancing*: ribilanciare automaticamente il cluster quando la topologia cambia.
- *Smart Client Load Balancing*: distribuzione intelligente delle richieste client attraverso i member.
- *Partitioning Strategies*: strategie personalizzate di partizionamento per ottimizzare la località dei dati.

La scalabilità lineare consente di aggiungere capacità di storage e calcolo proporzionalmente al numero di member aggiunti.

== Protezione e Recupero da Split-Brain

Una delle sfide nei sistemi distribuiti è la condizione di "split-brain" che si verifica quando il cluster si divide in sottogruppi che non possono comunicare tra loro. Sono implementate due principali strategie di protezione:

- *Quorum basato su member*: richiede un numero minimo di member per operare. Ad esempio, in un cluster di 5 member, si può configurare un quorum di 3 member, garantendo che le operazioni siano eseguite solo se almeno 3 member sono attivi.

- *Quorum personalizzato*: Consente di definire regole specifiche per il quorum, come basarsi su un attributo personalizzato dei member.

Esistono inoltre diversi meccanismi per identificare situazioni di partizionamento, tra cui uno basato su approcci probabilistici e un altro fondato sul monitoraggio degli heartbeat tra i nodi.

Per quanto riguarda il recupero da uno split-brain, è implementato un processo di merge che consente di combinare coerentemente i dati provenienti dai diversi sottogruppi. Questo processo è altamente configurabile e supporta diverse politiche di merge, che determinano il modo in cui vengono risolti i conflitti tra i dati.

Ad esempio, è possibile utilizzare la politica LatestUpdateMergePolicy, che conserva il valore aggiornato più di recente, oppure la PutIfAbsentMergePolicy, che inserisce il valore solo se non esiste già nella partizione di destinazione.

== Hazelcast Jet: Elaborazione di Stream e Batch

Jet è il motore di elaborazione distribuita incorporato in Hazelcast, progettato per processare sia dati in tempo reale (stream) sia grandi volumi di dati statici (batch), ed è anche utilizzato per eseguire le query SQL.

=== Modellazione ed Esecuzione dei Job

Jet modella le elaborazioni come un directed acyclic graph (DAG) di operazioni:

- *Vertex*: Rappresenta un'unità logica di elaborazione che esegue una singola operazione (map, filter, aggregate, ecc.)

- *Edge*: Connette i vertex e definisce come i dati fluiscono tra le operazioni (one-to-one, broadcast, distributed, ecc.)

- *Processor*: L'implementazione concreta di un vertex, che contiene la logica di business

- *Execution Plan*: Jet converte il DAG logico in un piano di esecuzione fisico, ottimizzando per località dei dati e parallelismo

- *Distribuzione intelligente*: Jet automaticamente distribuisce le operazioni sui member in modo da minimizzare il movimento dei dati sulla rete, sfruttando l'affinità dei dati

=== Cooperative Multithreading

Il modello di esecuzione di Jet è basato sul cooperative multithreading, un approccio che offre significativi vantaggi rispetto al multithreading tradizionale:

- *Thread condivisi*: Anziché assegnare un thread dedicato a ogni task, Jet condivide un numero fisso di thread (tipicamente uno per core CPU) tra molti task logici.

- *Non-blocking execution*: I processor rilasciano volontariamente il controllo dopo aver elaborato un batch di elementi, permettendo ad altri processor di utilizzare il thread.

- *Eliminazione del context switching*: Riducendo drasticamente i context switch del sistema operativo, Jet ottiene latenze più basse e throughput più elevato.

- *Backpressure naturale*: Il modello cooperativo crea un meccanismo di backpressure naturale, rallentando automaticamente le sorgenti quando i processor a valle non tengono il passo.

- *Event loop*: Ogni thread implementa un event loop che itera tra i processor attivi, garantendo un'equa distribuzione delle risorse di calcolo.

=== Gestione del Disordine degli Eventi

Nei sistemi di stream processing, gli eventi spesso arrivano fuori ordine. Jet implementa diversi meccanismi per gestire questa problematica:

- *Watermarking*: Jet utilizza watermark per tracciare il progresso del tempo eventuale, permettendo di identificare eventi in ritardo.

- *Event-time processing*: Gli eventi sono elaborati in base al loro timestamp di creazione anziché al momento di arrivo, permettendo risultati corretti nonostante il disordine.

- *Late event handling*: Configurazioni per gestire eventi tardivi, come includerli nei risultati (aggiornando aggregazioni precedenti), scartarli o indirizzarli verso un flusso separato.

- *Allowed Lateness*: Possibilità di configurare una "tolleranza al ritardo" che permette di includere eventi tardivi fino a una certa soglia.

=== Sliding Window Aggregation

Le finestre scorrevoli (sliding window) sono una tecnica fondamentale nell'elaborazione di stream per aggregare dati su intervalli di tempo mobili:

- *Modello di aggregazione*: Jet implementa un approccio a due fasi con aggregazione locale seguita da aggregazione globale per massimizzare l'efficienza.

- *Finestre temporali*: Supporto per diversi tipi di finestre:
  - *Tumbling Window*: Intervalli fissi non sovrapposti
  - *Sliding Window*: Finestre che si sovrappongono e avanzano con incrementi più piccoli della dimensione della finestra
  - *Session Window*: Raggruppano eventi vicini in sessioni, con timeout configurabile

- *Gestione efficiente della memoria*: Anziché memorizzare tutti gli eventi all'interno di una finestra, Jet conserva solo gli aggregati parziali, riducendo così l'utilizzo di memoria.

- *Fault tolerance*: Jet crea periodicamente punti di controllo (checkpoint) dello stato delle finestre, consentendo il ripristino in caso di problemi.
