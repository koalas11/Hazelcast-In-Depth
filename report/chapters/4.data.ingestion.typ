#import "../packages.typ": colorGrad

= Data Ingestion in Hazelcast

Hazelcast offre diverse opzioni per il data ingestion che permettono di trasferire dati da sistemi esterni a Hazelcast. Questi metodi di ingestione consentono di integrare Hazelcast con l'infrastruttura esistente e di costruire pipeline di dati complete.

== Pipeline di Dati e Connettori

Le pipeline di dati rappresentano il meccanismo principale. Funzionano come un framework di elaborazione che permette di definire flussi di dati da sorgenti esterne, attraverso trasformazioni, fino a destinazioni che possono essere all'interno o all'esterno di Hazelcast.

=== Connettori per Pipeline

Hazelcast fornisce numerosi connettori pronti all'uso per le pipeline di dati, che si suddividono in:

==== Connettori di Sorgente (Source)

- *Database*:
  - JDBC (per database relazionali come MySQL, PostgreSQL, Oracle)
  - MongoDB
  - Couchbase

- *Messaggistica*:
  - Apache Kafka
  - Apache Pulsar
  - RabbitMQ
  - JMS (Java Message Service)

- *Storage e file*:
  - File locali (CSV, JSON, Parquet)
  - HDFS (Hadoop Distributed File System)
  - Amazon S3
  - Azure Blob Storage

- *Streaming*:
  - Socket TCP/UDP
  - HTTP/WebSocket

==== Connettori di Destinazione (Sink)

- Tutti i connettori di sorgente possono essere usati anche come destinazione
- Hazelcast Map, Cache, Topic (per storage interno)
- Elasticsearch, Solr
- InfluxDB, Prometheus (per dati di serie temporali)

=== Change Data Capture (CDC) con Pipeline

Il Change Data Capture è un pattern implementato attraverso connettori specializzati nelle pipeline di dati di Hazelcast. CDC permette di catturare le modifiche apportate ai database e replicarle in tempo reale in Hazelcast.

==== Connettori CDC in Hazelcast

Hazelcast fornisce connettori CDC che si integrano con:

- *Debezium*: una piattaforma CDC open source che monitora i database e cattura le modifiche dei dati
- *Database specifici*:
  - MySQL CDC
  - PostgreSQL CDC
  - MongoDB CDC
  - SQL Server CDC
  - Oracle CDC (tramite LogMiner o XStream)

==== Vantaggi del CDC con Pipeline

- *Ingestione in tempo reale*: cattura le modifiche non appena avvengono
- *Basso impatto*: utilizza i log di transazione del database anziché query pesanti
- *Completezza*: cattura tutti i tipi di modifiche (inserimenti, aggiornamenti, eliminazioni)
- *Consistenza*: mantiene l'ordine delle transazioni

== MapStore

MapStore è un meccanismo alternativo per l'integrazione con storage esterni, orientato specificamente alle mappe Hazelcast:

=== Caratteristiche di MapStore

- *Read-through*: caricamento automatico dei dati dallo storage esterno quando non sono presenti nella cache
- *Write-through*: scrittura automatica dei dati nello storage esterno quando vengono modificati nella cache
- *Write-behind*: scrittura asincrona con coda di operazioni per migliorare le performance

==== Configurazione di MapStore

```xml
<map name="mia-mappa">
  <map-store enabled="true" initial-mode="LAZY">
    <class-name>com.esempio.MioMapStore</class-name>
    <write-delay-seconds>5</write-delay-seconds>
    <write-batch-size>100</write-batch-size>
    <properties>
      <property name="url">jdbc:postgresql://localhost:5432/database</property>
      <property name="user">utente</property>
      <property name="password">password</property>
    </properties>
  </map-store>
</map>
```

== Confronto tra Pipeline/CDC e MapStore

#figure(
  caption: [Confronto tra Pipeline/CDC e MapStore],
  table(
    columns: (auto,) * 3,
    table.header([Caratteristica], [Pipeline con CDC], [MapStore]),
    [Modello di integrazione], [Push (basato su eventi)], [Pull (read-through) e Push (write-through/behind)],
    [Tipo di dati], [Stream di modifiche], [Operazioni individuali su mappa],
    [Supporto per trasformazioni], [Completo (mapping, filtraggio, aggregazioni)], [Limitato (solo durante store/load)],
    [Latenza], [Generalmente bassa], [Bassa per cache-hit, variabile per cache-miss],
    [Scalabilità], [Eccellente per grandi volumi], [Buona per carichi moderati],
    [Complessità], [Media], [Bassa],
  ),
)

== Quando Usare i Diversi Metodi di Ingestione

=== Quando Usare Pipeline con CDC

- *Sincronizzazione continua*: per mantenere Hazelcast aggiornato con i database operativi
- *Elaborazione di stream complessi*: quando i dati richiedono trasformazioni e arricchimenti
- *Architettura event-driven*: quando si lavora con sistemi basati su eventi
- *Integrazione multi-sorgente*: quando i dati provengono da fonti diverse

=== Quando Usare MapStore

- *Applicazioni CRUD*: quando Hazelcast è usato come cache per operazioni di lettura/scrittura
- *Persistenza trasparente*: quando si vuole che la persistenza sia gestita automaticamente
- *Modello di accesso basato su chiavi*: per carichi di lavoro con accesso diretto per chiave
- *Caricamento lazy*: quando si preferisce caricare i dati solo quando necessario

== Commenti

Hazelcast offre diverse opzioni per il data ingestion, con le pipeline che forniscono un framework flessibile e potente con connettori per numerose tecnologie, incluso il supporto per CDC. Il MapStore rappresenta invece un'alternativa più semplice e specifica per scenari in cui Hazelcast viene utilizzato principalmente come cache con persistenza. La scelta del metodo dipende dalle esigenze specifiche dell'applicazione e dal modello di integrazione desiderato.
