= Query Distribuite

Hazelcast fornisce supporto SQL (non completo, ma con alcune estensioni) come modalità potente e familiare per interrogare e manipolare dati distribuiti nel cluster. Questo permette agli utenti di sfruttare le proprie conoscenze SQL esistenti mentre lavorano con strutture dati distribuite.

== Metodi di Accesso ai Dati

SQL in Hazelcast può interrogare dati da molteplici fonti:

#figure(caption: [Fonte dati e azione intrapresa da Hazelcast], table(
  columns: (auto,) * 2,
  table.header([Fonte Dati], [Descrizione azione]),
  [Maps], [Interroga le map distribuite di Hazelcast],
  [Kafka], [Interroga dati da topic Kafka],
  [File Systems], [Accede ai dati in file esterni],
  [JDBC], [Connette a database esterni],
  [MongoDB], [Interroga collezioni MongoDB],
))

== Meccanismo di Esecuzione Distribuita delle Query

=== Processo di Distribuzione

Il motore di query distribuito di Hazelcast utilizza un approccio multi-fase:

- *Pianificazione della Query*: Il membro che riceve la query crea un piano di esecuzione
- *Esecuzione Partition-Aware*: Le query vengono indirizzate alle partizioni specifiche dove risiedono i dati
- *Elaborazione Parallela*: Ogni membro esegue porzioni della query sulle proprie partizioni locali
- *Aggregazione dei Risultati*: I risultati da tutti i membri vengono raccolti e combinati

```sql
SELECT AVG(stipendio) FROM dipendenti WHERE dipartimento = 'Ingegneria'
```

Per questa query, il processo di distribuzione avviene come segue:
1. Analizzata e ottimizzata la query
2. Inviati i criteri di filtro a ciascun membro
3. Eseguito il filtro localmente su ogni partizione
4. Calcolate le medie parziali su ogni membro
5. Aggregato il risultato finale

=== Transparenza delle Query

Hazelcast fornisce trasparenza di localizzazione per le query, astraendo la natura distribuita dei dati:

- *Vista Unificata dei Dati*: Le query operano su dati distribuiti come se fossero una singola collezione
- *Indipendenza dalla Localizzazione*: Le applicazioni non devono sapere dove sono memorizzati i dati
- *Routing Partition-Aware*: Le query si indirizzano automaticamente alle partizioni rilevanti
- *Gestione Dinamica della Topologia*: Le query si adattano ai membri che entrano o escono dal cluster

Questa trasparenza permette agli sviluppatori di lavorare con dati distribuiti utilizzando paradigmi familiari, senza dover gestire le complessità dello stato distribuito.

=== Join Distribuiti

I join distribuiti sono ottimizzati attraverso:

- *Join Co-localizzati*: Quando possibile, i join vengono eseguiti su dati co-localizzati sullo stesso membro
- *Join Partizionati*: I dati vengono temporaneamente ripartizionati per collocare insieme le chiavi di join
- *Join Broadcast*: Dataset più piccoli vengono trasmessi a tutti i membri per il join con dataset più grandi

```sql
SELECT o.id_ordine, c.nome
FROM ordini o JOIN clienti c ON o.id_cliente = c.id
WHERE o.importo > 1000
```

A seconda delle dimensioni e della distribuzione dei dati, viene selezionata la strategia di join ottimale.

=== Query Pushdown

Per ottimizzare le prestazioni delle query è utilizzato un aggressivo pushdown:

- *Filter Pushdown*: Le clausole WHERE vengono spinte alle fonti dati
- *Projection Pushdown*: Solo le colonne richieste vengono recuperate
- *Aggregation Pushdown*: Aggregazioni parziali avvengono nelle fonti dati
- *External Source Pushdown*: I filtri vengono spinti a sistemi esterni (JDBC, MongoDB, ecc.)

=== Supporto agli Indici Distribuiti

Gli indici migliorano significativamente le prestazioni delle query e sono gestiti in modo distribuito:

```sql
CREATE INDEX idx_cliente_nome ON clienti(nome);
```

L'indice viene:
- Creato su ogni partizione
- Mantenuto localmente da ogni membro
- Utilizzato automaticamente dall'ottimizzatore di query
- Aggiornato atomicamente con le modifiche ai dati

Hazelcast supporta:
- Indici singoli e compositi
- Indici ordinati
- Indici bitmap per dati ad alta cardinalità

=== Caching delle Query Distribuite

Le query ripetute sono ottimizzate attraverso:

- Caching di query parametrizzate
- Caching del piano di esecuzione
- Caching dei risultati per query qualificanti

== SQL su Map

Le map distribuite sono la struttura dati primaria in Hazelcast, e SQL fornisce un modo potente per interrogarle (la Map è l'unica struttura dati distribuita con supporto completo di SQL).

=== Concetti di Mapping

Per interrogare una map con SQL, è necessario prima definire il suo schema utilizzando:

- *Mapping esplicito*: Definire manualmente nomi e tipi di colonne
- *Mapping basato su reflection*: Derivare automaticamente lo schema dalle classi Java/serializzazione

```sql
CREATE MAPPING mia_mappa (
  id INT,
  nome VARCHAR,
  eta INT
) TYPE IMap OPTIONS (
  'keyFormat' = 'int',
  'valueFormat' = 'json'
);
```

Per i tipi di dati primitivi, il processo avviene automaticamente, mentre per tipi creati dall'utente è necessario definire esplicitamente il mapping.

== API `Predicate`: Un'Alternativa a SQL

Oltre all'interfaccia SQL, Hazelcast offre una potente API Predicati che consente di interrogare i dati in modo programmatico:

```java
IMap<Integer, Dipendente> dipendenti = hazelcastInstance.getMap("dipendenti");

// Filtro semplice
Collection<Dipendente> risultato = dipendenti.values(Predicates.equal("dipartimento", "Ingegneria"));

// Filtri composti
Predicate<Integer, Dipendente> predicate = Predicates.and(
    Predicates.equal("dipartimento", "Ingegneria"),
    Predicates.greaterThan("stipendio", 50000)
);
Collection<Dipendente> ingegneriSenior = dipendenti.values(predicate);
```

=== Funzionalità avanzate dell'API `Predicate`

- *Predicati Compositi*: Combinazione di più condizioni con `and`, `or`, `not`
- *Predicati su Partizioni*: Esecuzione di filtri su partizioni specifiche
- *Predicati di Paging*: Supporto per paginazione dei risultati
- *Predicati Personalizzati*: Implementazione di logica di filtro personalizzata
- *Supporto per tipi complessi*: Predicati su mappe, liste e altri tipi di dati complessi

Oltre a queste funzionalità è possibile anche aggregare i risultati utilizzando l'API `Aggregation` e trasformare i risultati dei predicati con l'API `Projection`.

=== Vantaggi dell'API `Predicate`

- *Integrazione naturale con Java*: Ideale per sviluppatori che preferiscono un approccio programmatico
- *Tipizzazione forte*: Rileva errori di tipo a tempo di compilazione
- *Flessibilità*: Permette di costruire predicati complessi e dinamici in fase di esecuzione

```java
// Predicato personalizzato
Predicate<Integer, Dipendente> predicatoPersonalizzato = new Predicate<Integer, Dipendente>() {
    @Override
    public boolean apply(Map.Entry<Integer, Dipendente> entry) {
        Dipendente dipendente = entry.getValue();
        return dipendente.getAnniServizio() > 5 &&
               dipendente.getValutazioneAnnuale() > 4.0;
    }
};

Collection<Dipendente> dipendentiPremiati = dipendenti.values(predicatoPersonalizzato);
```

=== Predicati Distribuiti

Come per SQL, i predicati vengono eseguiti in modo distribuito:
- Vengono serializzati e inviati ai membri del cluster
- Vengono eseguiti localmente su ogni partizione
- Solo i risultati filtrati vengono restituiti, riducendo il traffico di rete

=== Quando usare Predicati vs SQL

- *Usa Predicati quando*:
  - Lavori in un contesto puramente Java
  - Necessiti di logica di filtro molto complessa o personalizzata
  - Hai bisogno di costruire filtri dinamicamente a runtime

- *Usa SQL quando*:
  - Preferisci un approccio dichiarativo
  - Necessiti di join complessi o aggregazioni
  - Hai bisogno di interoperabilità con altri sistemi
  - Desideri sfruttare ottimizzazioni avanzate del query planner

== SQL su Fonti Dati Esterne

Come estensione naturale delle capacità di data ingestion descritte nel Capitolo 4, Hazelcast permette di interrogare direttamente fonti dati esterne utilizzando SQL. Mentre nel Capitolo 4 abbiamo visto come inserire dati attraverso pipeline e connettori, qui vedremo come SQL fornisce un'interfaccia unificata per accedere a questi stessi dati.

=== Integrazione con Kafka

In Hazelcast è possibile interrogare direttamente i topic Kafka tramite SQL:

```sql
CREATE MAPPING kafka_topic (
  __key VARCHAR,
  messaggio VARCHAR
) TYPE Kafka OPTIONS (
  'bootstrap.servers' = 'kafka:9092',
  'topic' = 'mio-topic',
  'auto.offset.reset' = 'earliest'
);
```

Questa integrazione complementa i connettori Kafka descritti nel Capitolo 4, offrendo:
- Elaborazione di stream in tempo reale utilizzando SQL
- Join di dati Kafka con map Hazelcast
- Filtro e trasformazione dei messaggi prima dell'elaborazione

=== Accesso ai File System

SQL può anche interrogare file memorizzati in varie posizioni:

- File locali
- Hadoop Distributed File System (HDFS)
- Storage cloud (S3, Azure Blob Storage)

```sql
CREATE MAPPING file_csv (
  id INT,
  nome VARCHAR
) TYPE File OPTIONS (
  'format' = 'csv',
  'path' = '/data/*.csv'
);
```

Questo approccio fornisce un'alternativa dichiarativa ai connettori di file source descritti nel Capitolo 4.

=== Connettore JDBC

Il connettore JDBC consente di interrogare database relazionali esterni:

```sql
CREATE MAPPING tabella_esterna
TYPE JDBC OPTIONS (
  'jdbcUrl' = 'jdbc:mysql://database:3306/db',
  'username' = 'utente',
  'password' = 'password',
  'table' = 'clienti'
);
```

Questo meccanismo offre un'alternativa al pattern MapStore per l'integrazione con database, con il vantaggio di:
- Query federate tra Hazelcast e database esterni
- Integrazione dati senza processi ETL espliciti

=== Integrazione MongoDB

È possibile interrogare collezioni MongoDB attraverso il Mapping SQL:

```sql
CREATE MAPPING collezione_mongo
TYPE MongoDB OPTIONS (
  'connectionString' = 'mongodb://localhost:27017',
  'database' = 'test',
  'collection' = 'dipendenti'
);
```

Rispetto al connettore MongoDB descritto nel Capitolo 4, l'approccio SQL offre un'interfaccia più dichiarativa e familiare.

== Lavorare con JSON

È presente anche un robusto supporto per lavorare con dati JSON:

- Interrogare strutture JSON con notazione a punti
- Estrarre campi e array annidati
- Trasformare tra formati JSON e relazionali

```sql
SELECT cliente.nome, cliente.indirizzo.citta
FROM clienti
WHERE cliente.ordini[0].stato = 'SPEDITO';
```

== Elaborazione di Stream con SQL

In Hazelcast è possibile avere query continue sui dati in streaming (nell'API Predicate è necessario utilizzare un meccanismo diverso, il Continuous Query Cache).

=== Fondamenti di Streaming

- Le query vengono eseguite continuamente sui dati in arrivo
- I risultati vengono prodotti incrementalmente all'arrivo di nuovi dati
- Supporto per operazioni di windowing (tumbling, sliding, session)

```sql
SELECT stream prodotto, COUNT(*)
FROM ordini
GROUP BY prodotto
HAVING COUNT(*) > 100;
```

Questa funzionalità si integra perfettamente con le pipeline di dati descritte nel Capitolo 4, offrendo un approccio dichiarativo all'elaborazione di stream.

== Tipi di Dati SQL

SQL di Hazelcast supporta i tipi di dati SQL standard:

#figure(caption: [Tipi supportati da Hazelcast SQL], table(
  columns: (auto,) * 8,
  table.header([Categoria], table.cell(colspan: 7)[Tipi]),
  [Numerici], [TINYINT], [SMALLINT], [INT], [BIGINT], [DECIMAL], [REAL], [DOUBLE],
  [Stringa], table.cell(colspan: 4)[VARCHAR], table.cell(colspan: 3)[CHAR],
  [Temporali], [DATE], [TIME], table.cell(colspan: 2)[TIMESTAMP], table.cell(colspan: 3)[TIMESTAMP WITH TIME ZONE],
  [Altri], table.cell(colspan: 3)[BOOLEAN], table.cell(colspan: 2)[JSON], table.cell(colspan: 2)[OBJECT],
))

== Tipi Definiti dall'Utente

Oltre a i tipi SQL Standard sono supportati tipi di dati personalizzati:

- Oggetti Serializzabili
- Oggetti compat serialization
- Oggetti serializzabili personalizzati

Per utilizzare efficacemente i tipi personalizzati è necessario:
- Registrare i serializzatori con Hazelcast
- Configurare le impostazioni di reflection se necessario
- Creare mapping appropriati

== Query su Collezioni e Array

Data la possibilità di avere strutture dati complesse, Hazelcast SQL supporta query su collezioni e array sia per SQL che per l'API Predicate:

```java
class Motorbike {
  Wheel[] wheels;
}

class Wheel {
  String name;
}
```

Ad esempio dato questo oggetto, è possibile eseguire query come:
```sql
SELECT m.wheels[0].name FROM Motorbike m WHERE m.wheels[0].name = 'front-wheel';
```

oppure utilizzare l'API Predicate per filtrare:

```java
Predicate<Integer, Motorbike> predicate = Predicates.equal("wheels[0].name", "front-wheel");
Collection<Motorbike> result = motorbikes.values(predicate);
```

E se invece di un indice preciso si desidera verificare se l'elemento esiste in un array, si può impostare `any` come indice.

== Transazioni Distribuite

Hazelcast fornisce un robusto supporto per transazioni distribuite che garantiscono operazioni atomiche su dati distribuiti in tutto il cluster.

=== Tipi di Transazioni

È possibile eseguire transazioni distribuite scegliendo tra due approcci:

==== Commit a Due Fasi (2PC)

Il protocollo di commit a due fasi è l'approccio principale per le transazioni distribuite:

```java
TransactionContext contesto = hazelcastInstance.newTransactionContext();
contesto.beginTransaction();
try {
    TransactionalMap<String, String> mappa = contesto.getMap("mappa-transazionale");
    mappa.put("chiave", "valore");

    TransactionalQueue<String> coda = contesto.getQueue("coda-transazionale");
    coda.offer("messaggio");

    contesto.commitTransaction();
} catch (Exception e) {
    contesto.rollbackTransaction();
    throw e;
}
```

Il commit a due fasi garantisce:
- Atomicità attraverso multiple operazioni
- Consistenza su più strutture dati
- Isolamento da altre transazioni
- Durabilità una volta effettuato il commit

==== Commit a Una Fase (1PC)

Per operazioni su singola partizione, è possibile utilizzare il commit a una fase:

```java
TransactionOptions opzioni = new TransactionOptions()
    .setTransactionType(TransactionOptions.TransactionType.ONE_PHASE);
TransactionContext contesto = hazelcastInstance.newTransactionContext(opzioni);
```

*Vantaggi del 1PC*:
- Migliori prestazioni eliminando la fase di preparazione
- Minore latenza per le transazioni

*Svantaggi del 1PC*:
- Applicabile solo quando tutte le operazioni interessano una singola partizione
- Minore resilienza ai guasti: in caso di crash durante il commit, lo stato potrebbe rimanere inconsistente
- Nessuna possibilità di ripristino automatico in caso di fallimento
- Non adatto per operazioni che coinvolgono più strutture dati su partizioni diverse

=== Livelli di Isolamento

Sono supportati diversi livelli di isolamento delle transazioni:

- *READ_COMMITTED*: Livello predefinito, previene letture sporche
- *REPEATABLE_READ*: Previene letture sporche e non ripetibili

```java
TransactionOptions opzioni = new TransactionOptions()
    .setTransactionType(TransactionOptions.TransactionType.TWO_PHASE)
    .setIsolationLevel(TransactionOptions.IsolationLevel.REPEATABLE_READ);
```

=== Strutture Dati Transazionali

Hazelcast supporta transazioni su:

- TransactionalMap
- TransactionalMultiMap
- TransactionalSet
- TransactionalList
- TransactionalQueue

Ciascuna fornisce semantica transazionale per le rispettive operazioni.

=== Transazioni XA

Per l'integrazione con transaction manager conformi a JTA:

```java
XAResource xaResource = hazelcastInstance.getXAResource();
Transaction transaction = transactionManager.getTransaction();
transaction.enlistResource(xaResource);
```

Le transazioni XA abilitano:
- Integrazione con transaction manager esterni
- Partecipazione a transazioni distribuite che coprono risorse multiple
- Recupero da guasti di sistema

=== Configurazione delle Transazioni

Hazelcast fornisce proprietà configurabili per le transazioni:

```xml
<hazelcast>
    <properties>
        <property name="hazelcast.transaction.max.timeout">120000</property>
    </properties>
</hazelcast>
```

Le opzioni di configurazione chiave includono:
- Timeout della transazione
- Dimensione del log di transazione
- Impostazioni di durabilità

=== Limitazioni delle Transazioni

Le transazioni hanno vincoli importanti:
- Non possono coinvolgere più cluster
- C'è un overhead per mantenere lo stato transazionale
- Transazioni di lunga durata possono impattare le prestazioni
- Le transazioni hanno un timeout massimo ma configurabile

== Integrazione con le Strategie di Data Ingestion

Come abbiamo visto nel Capitolo 4, Hazelcast offre diverse opzioni per l'ingestion dei dati. L'interfaccia SQL si integra perfettamente con queste strategie:

- *Pipeline e CDC*: I dati inseriti tramite pipeline possono essere immediatamente interrogati con SQL
- *MapStore*: I dati caricati tramite MapStore sono accessibili attraverso query SQL una volta mappati
- *Dati streaming*: SQL può essere utilizzato per query continue sui dati in arrivo attraverso i connettori streaming

Questa integrazione crea un'esperienza coerente: i dati possono essere inseriti attraverso vari meccanismi (come descritto nel Capitolo 4) e poi interrogati uniformemente tramite SQL o API Predicati.

== Commenti

Hazelcast fornisce un'interfaccia potente e conforme agli standard per interrogare dati distribuiti. Supportando molteplici fonti di dati e offrendo funzionalità di ottimizzazione, permette agli utenti di costruire applicazioni complesse di elaborazione dati con la familiare sintassi SQL o attraverso l'API Predicati, sfruttando al contempo i vantaggi di performance e scalabilità dell'architettura distribuita.

Le transazioni distribuite completano il quadro, offrendo diverse strategie (1PC o 2PC) per garantire atomicità e consistenza nelle operazioni su dati distribuiti, con la possibilità di scegliere il giusto compromesso tra prestazioni e resilienza ai guasti.

L'esecuzione delle query distribuite inoltre è intelligentemente ottimizzata minimizzando la trasmissione di dati non necessari al nodo di calcolo finale.
