#import "../packages.typ": colorGrad, fletcher

= Confronto tra Hazelcast e Altre Tecnologie Distribuite

Hazelcast si posiziona in un ecosistema di tecnologie distribuite dove diverse soluzioni offrono approcci alternativi per risolvere sfide simili. In questo capitolo, analizzeremo come Hazelcast si confronta con altre tecnologie popolari, evidenziando somiglianze e differenze architetturali.

== Confronto Architetturale

=== Hazelcast vs Apache Cassandra

*Similarità:*
- *Architettura peer-to-peer*: Entrambi utilizzano un design peer-to-peer dove tutti i nodi hanno ruoli equivalenti, eliminando single point of failure
- *Distribuzione automatica dei dati*: Entrambi partizionano automaticamente i dati tra i nodi
- *Scalabilità orizzontale*: Entrambi permettono di aggiungere nodi a runtime
- *Replica dei dati*: Entrambi supportano la replicazione per fault tolerance

*Differenze:*
- *Modello di storage*: Hazelcast è primariamente in-memory, Cassandra è disk-based con cache in-memory
- *Modello di consistenza*: Hazelcast offre sia modelli AP che CP (in versione Enterprise), Cassandra è progettato come sistema AP con consistenza eventuale e livelli di consistenza configurabili
- *Paradigma di accesso*: Hazelcast offre strutture dati distribuite, Cassandra è un database wide-column
- *Query*: Hazelcast supporta SQL completo, Cassandra usa CQL con limitazioni significative
- *Computing*: Hazelcast include capacità di computing distribuito, Cassandra è principalmente orientato allo storage
- *Comunicazione tra nodi*: In  Hazelcast, i nodi comunicano direttamente tra loro, mentre in Cassandra la comunicazione avviene attraverso un protocollo di gossip e un coordinatore per le operazioni di scrittura

#let diagramma(title, description) = {
  import fletcher: *
  stack(
    dir: direction.ttb,
    spacing: 1em,
    title,
    diagram(
      node-stroke: colorGrad + 1pt,
      edge-stroke: black + .75pt,
      node((0, 0), [Node1]),
      edge("<|-|>"),
      node((2, 0), [Node2]),
      edge("<|-|>"),
      node((2, 2), [Node3]),
      edge("<|-|>"),
      node((0, 2), [Node4]),
      edge((0, 0), "<|-|>"),
      edge((0, 0), (2, 2), "<|-|>"),
      edge((2, 0), (0, 2), "<|-|>"),
    ),
    description,
  )
}

#figure(caption: [Schema di confronto tra nodi: Hazelcast vs Cassandra], {
  grid(
    columns: (auto,) * 2,
    column-gutter: 1em,
    fill: tiling(size: (16pt, 16pt), relative: "parent", place(dx: 5pt, dy: 5pt, rotate(45deg, square(
      size: 2pt,
      fill: black.transparentize(90%),
    )))),
    diagramma(strong[Hazelcast], align()[In-Memory Storage\ Strutture Dati + Compute\ Full Mesh Network]),
    stack(
      dir: direction.ttb,
      spacing: 1em,
      strong[Cassandra],
      {
        import fletcher: *
        diagram(
          node-stroke: colorGrad + 1pt,
          edge-stroke: black + .75pt,
          node((0, 0), [Node1]),
          edge("<|-|>"),
          node((2, 0), [Node2]),
          edge("<|-|>"),
          node((2, 2), [Node3]),
          edge("<|-|>"),
          node((0, 2), [Node4]),
          edge((0, 0), "<|-|>"),
        )
      },
      align()[Disk-Based Storage\ Wide-Column DB\ Gossiped Network],
    ),
  )
})

=== Hazelcast vs Redis

*Similarità:*
- *In-memory*: Entrambi sono progettati come sistemi di storage in-memory ad alte prestazioni
- *Strutture dati*: Entrambi offrono strutture dati oltre il semplice key-value (liste, set, etc.)
- *Persistenza opzionale*: Entrambi offrono meccanismi di persistenza

*Differenze:*
- *Architettura*: Hazelcast è peer-to-peer, Redis tradizionale ha un'architettura master-slave (sebbene Redis Cluster sia più distribuito)
- *Distribuzione*: Hazelcast distribuisce automaticamente i dati, Redis richiede configurazione manuale dei nodi per lo sharding (automatico con Redis Enterprise)
- *Computing*: Hazelcast ha capacità di computing distribuite native, Redis offre scripting Lua ma con limitazioni
- *Linguaggio*: Hazelcast è basato su Java/JVM, Redis è scritto in C
- *Consistenza*: Hazelcast offre un sottosistema CP, Redis ha consistenza più limitata nel cluster

#figure(caption: [Schema di confronto tra nodi: Hazelcast vs Redis], {
  grid(
    columns: (auto,) * 2,
    column-gutter: 1em,
    fill: tiling(size: (16pt, 16pt), relative: "parent", place(dx: 5pt, dy: 5pt, rotate(45deg, square(
      size: 2pt,
      fill: black.transparentize(90%),
    )))),
    diagramma(strong[Hazelcast], align()[Peer-to-Peer, Symmetric]),
    stack(
      dir: direction.ttb,
      spacing: 1em,
      strong[Redis],
      {
        import fletcher: *
        diagram(
          node-stroke: colorGrad + 1pt,
          edge-stroke: black + .75pt,
          node((1, 0), [Master]),
          edge("<|-|>", corner: right),
          node((2, 2), [Slave]),
          node((0, 2), [Slave]),
          edge((1, 0), "<|-|>", corner: right),
        )
      },
      [Master-Slave Model],
    ),
  )
})

Sul sito ufficiale di Hazelcast è disponibile un confronto tra Hazelcast e Redis, basato su un benchmark effettuato qualche anno fa: https://hazelcast.com/resources/hazelcast-vs-redis/. Curiosamente, anche se in altri test Redis mostrava prestazioni superiori rispetto a Hazelcast, gli ingegneri di Hazelcast hanno successivamente approfondito la questione e scoperto una criticità nella gestione delle repliche di Redis sotto carichi elevati. In queste condizioni, infatti, le repliche non venivano eseguite correttamente, con il rischio concreto di perdita di dati.
(Al seguente link è disponibile il post che spiega la questione: https://hazelcast.com/blog/redis-load-handling-vs-data-integrity/)

=== Hazelcast vs Apache Ignite

*Similarità:*
- *Computing in-memory*: Entrambi combinano storage e computing in-memory
- *SQL*: Entrambi offrono support SQL distribuito
- *Integrazione con sistemi esterni*: Entrambi offrono connettori per varie fonti dati
- *Architettura*: Entrambi utilizzano un design peer-to-peer

*Differenze:*
- *Persistenza nativa*: Ignite offre storage disk-nativo, Hazelcast è primariamente in-memory
- *ACID*: Ignite offre transazioni ACID complete, Hazelcast ha supporto transazionale più limitato
- *Maturità streaming*: Hazelcast Jet è più maturo per stream processing
- *Approccio di prodotto*: Ignite ha più funzionalità nella versione open-source, Hazelcast riserva feature avanzate per l'Enterprise

=== Hazelcast vs Apache Kafka

*Similarità:*
- *Distribuzione*: Entrambi sono sistemi distribuiti con partizionamento automatico
- *Scalabilità*: Entrambi offrono scalabilità orizzontale
- *Stream processing*: Entrambi supportano elaborazione di stream (Kafka Streams vs Jet)

*Differenze:*
- *Caso d'uso primario*: Kafka è progettato come piattaforma di messaggistica e log di eventi, Hazelcast è un data grid con capacità di streaming
- *Persistenza*: Kafka persiste tutto su disco, Hazelcast è primariamente in-memory
- *Retention*: Kafka mantiene i dati per periodi configurabili, Hazelcast tipicamente mantiene lo stato corrente
- *Modello di interazione*: Kafka utilizza un modello publish-subscribe, Hazelcast offre molteplici strutture dati
- *Computing*: Hazelcast ha capacità di computing più ampie e integrate

=== Hazelcast vs Elasticsearch

*Similarità:*
- *Distribuzione*: Entrambi sono sistemi distribuiti con sharding automatico
- *Scalabilità*: Entrambi offrono scalabilità orizzontale
- *Query distribuite*: Entrambi eseguono query in parallelo sui nodi

*Differenze:*
- *Caso d'uso primario*: Elasticsearch è ottimizzato per ricerca full-text, Hazelcast per computing distribuito
- *Storage*: Elasticsearch è disk-based, Hazelcast è in-memory
- *Modello di dati*: Elasticsearch è document-oriented, Hazelcast offre strutture dati distribuite
- *Query*: Elasticsearch eccelle in query di ricerca complesse, Hazelcast offre SQL tradizionale

/*
== Confronto di Prestazioni

=== Latenza

Nelle operazioni di lettura/scrittura, i sistemi in-memory come Hazelcast e Redis generalmente offrono latenze inferiori rispetto a sistemi disk-based:

#figure(caption: [Contronto latenze operazioni I/O], table(
  columns: (auto,) * 4,
  inset: .5em,
  align: center + horizon,
  table.header([Tecnologia], [Latenza di Lettura], [Latenza di Scrittura], [Note]),
  [Hazelcast], [Sub-millisecondo], [Sub-millisecondo], [In-memory con località dei dati],
  [Redis], [Sub-millisecondo], [Sub-millisecondo], [In-memory ottimizzato per latenza],
  [Cassandra], [Millisecondi], [Millisecondi], [Compromesso per alta scalabilità],
  [Ignite], [Sub-millisecondo], [Millisecondi], [Dipende dalla modalità di persistenza],
  [Kafka], [Millisecondi], [Millisecondi], [Ottimizzato per throughput, non latenza],
))

=== Throughput

Il throughput dipende fortemente dal carico di lavoro e dalla configurazione:

#figure(caption: [Confronto throughput], table(
  columns: (auto,) * 3,
  inset: .5em,
  align: center + horizon,
  table.header([Tecnologia], [Throughput Caratteristico], [Fattori Influenti]),
  [Hazelcast], [Centinaia di migliaia/secondo], [Numero di member, bilanciamento partizioni],
  [Redis], [Centinaia di migliaia/secondo], [Architettura master-slave può limitare],
  [Cassandra], [Decine di migliaia/secondo], [Limitato da accesso disco, ottimo per scritture],
  [Ignite], [Decine/centinaia di migliaia/secondo], [Dipende dalla persistenza],
  [Kafka], [Milioni di messaggi/secondo], [Ottimizzato per alto throughput],
))
*/

=== Scalabilità Orizzontale

Tutte le tecnologie menzionate supportano la scalabilità orizzontale, ma con caratteristiche diverse:

#figure(caption: [Confronto limiti di scalabilità], table(
  columns: (auto,) * 3,
  inset: .5em,
  align: center + horizon,
  table.header([Tecnologia], [Limite Pratico di Nodi], [Caratteristiche di Scalabilità]),
  [Hazelcast], [Centinaia], [Ribilanciamento automatico, limitato da memoria disponibile],
  [Cassandra], [Migliaia], [Progettato per scalabilità massiva, degradazione gracile],
  [Redis Cluster], [Centinaia], [Richiede pianificazione dello sharding],
  [Ignite], [Centinaia], [Simile a Hazelcast, bilanciamento automatico],
  [Kafka], [Decine/Centinaia], [Scalabile per broker e consumer group],
))

== Casi d'Uso Ottimali

- *Caching distribuito* con necessità di query avanzate
- *Elaborazione stream* in tempo reale con bassa latenza
- *Computazione distribuita* con località dei dati
- *Architetture event-driven* che richiedono storage e processing

/*
=== Cassandra
- *Big data* con pattern di scrittura intensivi
- *Time-series data* distribuiti globalmente
- *Disponibilità* prioritaria rispetto alla consistenza forte
- *Scalabilità massiva* fino a migliaia di nodi

=== Redis
- *Caching* semplice e ad alte prestazioni
- *Rate limiting* e contatori distribuiti
- *Leaderboard* e sistemi di punteggio
- *Message broker* leggero

=== Apache Ignite
- *Database in-memory* con persistenza nativa
- *Applicazioni ACID* distribuite
- *Compute grid* per carichi HPC
- *Casi ibridi* che richiedono transazionalità forte

=== Apache Kafka
- *Message broker* ad alto throughput
- *Log di eventi* centralizzato e distribuito
- *Integrazione di sistemi* disaccoppiati
- *Pipeline di dati* con garanzie di ordinamento
*/
/*
== Valutazione Complessiva

=== Forza di Hazelcast

Hazelcast eccelle particolarmente quando:

1. *Performance in-memory* è critica
2. *Elaborazione e storage* devono essere integrati
3. *Semplicità operativa* è importante (rispetto a stack più complessi)
4. *Query SQL* su dati distribuiti sono necessarie
5. *Località dei dati* è importante per minimizzare il movimento dei dati

=== Limitazioni di Hazelcast

Hazelcast potrebbe non essere la scelta ottimale quando:

1. *Persistenza duratura* è il requisito principale
2. *Set di dati massivi* superano significativamente la memoria disponibile
3. *Distribuzione geografica globale* con write locality è fondamentale
*/

== Commenti

Dall'analisi comparativa emergono alcune considerazioni architetturali:

1. *L'approccio peer-to-peer* di Hazelcast è una scelta architettonica significativa che favorisce la resilienza e la semplicità operativa, in modo simile a Cassandra ma con focus in-memory.

2. *L'integrazione storage-compute* è un vantaggio distintivo rispetto a sistemi focalizzati solo su storage o messaging.

3. *Il modello di partizionamento* di Hazelcast condivide principi con Cassandra e Kafka, ma è ottimizzato per operazioni in-memory e con maggiore località dei dati.

4. *Il dualismo AP/CP* è un approccio architetturale flessibile che differenzia Hazelcast da sistemi che offrono solo un modello di consistenza.

5. *L'architettura di streaming* integrata rappresenta un vantaggio rispetto a sistemi che richiedono componenti aggiuntivi per lo stream processing.

Quando si seleziona tra queste tecnologie, la decisione dovrebbe basarsi su:
- Requisiti di latenza vs throughput
- Modello di consistenza necessario
- Pattern di accesso ai dati
- Requisiti di calcolo distribuito
