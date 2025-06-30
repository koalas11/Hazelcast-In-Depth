= Strutture Dati Distribuite in Hazelcast

== Teorema CAP e Modelli di Consistenza in Hazelcast

=== Il Teorema CAP in Hazelcast

Il teorema CAP afferma che un sistema distribuito non può garantire simultaneamente:
- *Consistency* (C): tutti i nodi vedono gli stessi dati nello stesso momento
- *Availability* (A): ogni richiesta riceve una risposta, senza garanzia che contenga i dati più recenti
- *Partition tolerance* (P): il sistema continua a funzionare nonostante partizioni di rete

Hazelcast offre una caratteristica unica: supporta sia strutture dati AP (Availability e Partition tolerance) che CP (Consistency e Partition tolerance).

=== Strutture AP in Hazelcast

Sono disponibili numerose strutture dati distribuite che seguono il modello AP del teorema CAP, privilegiando l'alta disponibilità e la tolleranza alle partizioni, a scapito della consistenza forte in scenari di rete instabile o partizionata.

Strutture AP:
- *Map:* garantisce disponibilità anche durante le partizioni, ma con possibile lettura temporaneamente obsoleta (*eventually consistent*).

- *ReplicatedMap:* mappa completamente replicata su tutti i nodi del cluster, ottimizzata per letture frequenti con aggiornamenti meno frequenti.

- *Cache (JCache):* caching distribuito conforme a JSR-107, con semantica simile a Map in ottica di disponibilità.

- *Topic / ReliableTopic:* consegna dei messaggi anche in presenza di partizioni. ReliableTopic aggiunge buffering locale e retry automatico.

- *Queue / PriorityQueue:* strutture FIFO distribuite per scenari producer-consumer, disponibili anche in presenza di partizioni.

- *List / Set / MultiMap:* strutture collezione che privilegiano la disponibilità, tollerando inconsistenze temporanee.

- *Ringbuffer:* buffer circolare altamente scalabile, resiliente a partizioni e utile per stream processing.

- *Flake ID Generator:* generatore distribuito di identificativi univoci, AP per definizione, sicuro anche in reti partizionate.

- *PN Counter:* contatore CRDT (Conflict-free Replicated Data Type) che supporta increment e decrement distribuiti, convergendo automaticamente verso un valore consistente anche in presenza di partizioni di rete.

Alcune di queste strutture dati, come ad esempio List, Set e Queue, memorizzano tutti i dati su un unica partizione e prevedono repliche su altri nodi per garantire la disponibilità.
Inoltre anche se Hazelcast offre tutte queste strutture dati distribuite, la principale è *Map*, con molte funzionalità non presenti in altre strutture, come il TTL (Time to Live).

Queste strutture AP sono ideali per:
- Caching distribuito e invalidazione efficiente
- Applicazioni orientate alle prestazioni che richiedono alta disponibilità
- Ambienti con partizioni di rete o latenza variabile
- Elaborazione di eventi, messaggi e stream
- Sistemi che tollerano la consistenza eventuale in cambio di robustezza e scalabilità

=== CP Subsystem

Hazelcast offre anche un CP Subsystem, che implementa il paradigma CP del teorema CAP. Questa è una caratteristica distintiva, poiché consente di scegliere il modello di consistenza più adatto per ogni caso d'uso all'interno della stessa piattaforma. Per quanto sia utile, questa funzionalità è disponibile solamente nella versione Enterprise.

Il CP Subsystem è basato sull'algoritmo Raft(@Raft) e fornisce:

- *CPMap*: una mappa distribuita con consistenza forte, che garantisce che tutte le operazioni siano atomiche e visibili a tutti i nodi in modo lineare.
- *FencedLock*: lock distribuito con consistenza forte
- *IAtomicLong / IAtomicReference*: contatori e riferimenti atomici con consistenza linearizzabile
- *ICountDownLatch / ISemaphore*: primitive di sincronizzazione distribuite

Queste strutture CP sono ideali per:
- Coordinamento distribuito
- Gestione dell'ordine di elaborazione
- Leader election
- Sincronizzazione tra processi distribuiti
- Casi d'uso che richiedono garanzie transazionali

=== Il valore dell'approccio ibrido AP/CP

L'approccio ibrido di Hazelcast offre vantaggi significativi:

1. *Flessibilità architetturale*: i sistemi distribuiti reali spesso richiedono sia componenti AP che CP; Hazelcast consente di utilizzarli nella stessa piattaforma
2. *Ottimizzazione per caso d'uso*: è possibile scegliere il modello di consistenza ottimale per ciascun componente dell'applicazione
3. *Bilanciamento prestazioni/consistenza*: utilizzare strutture AP per operazioni ad alto throughput e strutture CP solo dove necessaria la consistenza forte
4. *Evoluzione incrementale*: possibilità di migrare gradualmente parti del sistema verso modelli di consistenza diversi

== Streaming Data Structures
Hazelcast offre una struttura dati specializzata per gestire in modo efficiente i dati in streaming.

=== Event Journal
L'Event Journal è una struttura dati distribuita che cattura automaticamente tutte le modifiche (inserimenti, aggiornamenti, cancellazioni) apportate a Map e ICache, memorizzandole in un log ordinato e distribuito.

Caratteristiche principali:
- *Capture automatico*: registra tutte le operazioni senza impatto sulle prestazioni
- *Ordering garantito*: mantiene l'ordine delle operazioni per partizione
- *Retention configurabile*: politiche di ritenzione basate su tempo o dimensione
- *Stream processing*: integrazione nativa con Hazelcast Jet per elaborazione in tempo reale

/*
== AI/ML Data Structures
Strutture dati specializzate ottimizzate per carichi di lavoro di machine learning e intelligenza artificiale.

=== Cardinality Estimator Service
Una struttura dati probabilistica per stimare in modo efficiente la cardinalità di grandi set di dati.

=== Vector Collection
La Vector Collection è ottimizzata per memorizzare vettori di embedding utilizzati in applicazioni di machine learning:

- *Similarity search*: ricerca per similarità utilizzando metriche come cosine similarity
- *Indexing avanzato*: supporto per indici vettoriali ad alte prestazioni
- *Scalabilità orizzontale*: distribuzione automatica dei vettori nel cluster
- *Integrazione ML*: compatibilità con framework di machine learning comuni
*/

== Strutture Dati per AI e Machine Learning

=== Vector Collection
Un oggetto in Beta per interagire con lo storage vettoriale, che contiene informazioni sui vettori e metadati associati, disponibile nella versione Enterprise.

==== Caratteristiche
- *Engine vettoriale specializzato*: Database engine specializzato ottimizzato per memorizzare, cercare e gestire embedding vettoriali e metadati aggiuntivi
- *Ricerca per similarità*: Supporto per metriche come cosine similarity, euclidean distance, dot product
- *Filtering integrato*: Possibilità di filtrare risultati basandosi sui metadati
- *Indicizzazione avanzata*: Utilizzo di algoritmi di indicizzazione vettoriale ad alte prestazioni
- *Distribuzione automatica*: I vettori sono automaticamente distribuiti nel cluster seguendo il modello di partizionamento

==== Algoritmi di Ricerca
La Vector Collection utilizza algoritmi ottimizzati per la ricerca approssimativa del vicino più prossimo (ANN):
- Supporto per indici HNSW (Hierarchical Navigable Small World)
- Bilanciamento tra precisione e velocità di ricerca
- Ottimizzazioni per grandi volumi di vettori ad alta dimensionalità

==== Integrazione con Pipeline AI
- Compatible con framework ML comuni (TensorFlow, PyTorch, scikit-learn)
- Supporto per embedding di diversi modelli (word2vec, BERT, custom embeddings)
- API per batch operations e real-time inference

== Strutture Dati Probabilistiche

=== Cardinality Estimator Service
Struttura dati probabilistica basata sull'algoritmo HyperLogLog per stimare in modo efficiente la cardinalità di grandi set di dati:

- *Stima approssimativa*: Fornisce stime accurate con utilizzo di memoria constante
- *Scalabilità*: Gestisce set di dati di qualsiasi dimensione con footprint di memoria fisso
- *Tolleranza ai guasti*: Supporto per split-brain protection
- *Merge operations*: Possibilità di combinare stime da diversi estimatori

== Commenti

Hazelcast fornisce un'ampia gamma di strutture dati distribuite che possono essere utilizzate per costruire applicazioni scalabili e resilienti. La possibilità di scegliere tra modelli di consistenza AP e CP consente agli sviluppatori di ottimizzare le prestazioni e consistenza in base ai requisiti specifici dell'applicazione. Inoltre le strutture dati specializzate per lo streaming e il machine learning offrono funzionalità avanzate per applicazioni.
