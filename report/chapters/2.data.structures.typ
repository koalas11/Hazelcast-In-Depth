= Strutture Dati Distribuite in Hazelcast

== Teorema CAP e Modelli di Consistenza in Hazelcast

=== Il Teorema CAP in Hazelcast

Il teorema CAP afferma che un sistema distribuito non può garantire simultaneamente:
- *Consistency* (C): tutti i nodi vedono gli stessi dati nello stesso momento
- *Availability* (A): ogni richiesta riceve una risposta, senza garanzia che contenga i dati più recenti
- *Partition tolerance* (P): il sistema continua a funzionare nonostante partizioni di rete

Hazelcast offre una caratteristica unica: supporta sia strutture dati AP (Availability e Partition tolerance) che CP (Consistency e Partition tolerance).

=== Strutture AP in Hazelcast

Hazelcast offre numerose strutture dati distribuite che seguono il modello AP del teorema CAP, privilegiando l'alta disponibilità e la tolleranza alle partizioni, a scapito della consistenza forte in scenari di rete instabile o partizionata.

Principali strutture AP:
- *Map / IMap / JCache:* garantiscono disponibilità anche durante le partizioni, ma con possibile lettura temporaneamente obsoleta (consistenza eventuale).

- *ReplicatedMap:* altamente disponibile, replica passiva dei dati. Ottimo per letture frequenti, ma con aggiornamenti asincroni.

- *Cache (JCache):* caching distribuito conforme a JSR-107, con semantica simile a IMap in ottica di disponibilità.

- *Topic / ReliableTopic:* consegna dei messaggi anche in presenza di partizioni. ReliableTopic aggiunge buffering locale e retry automatico.

- *Queue / PriorityQueue:* strutture FIFO distribuite per scenari producer-consumer, disponibili anche in presenza di partizioni.

- *List / Set / MultiMap:* strutture collezione che privilegiano la disponibilità, tollerando inconsistenze temporanee.

- *Ringbuffer:* buffer circolare altamente scalabile, resiliente a partizioni e utile per stream processing.

- *Flake ID Generator:* generatore distribuito di identificativi univoci, AP per definizione, sicuro anche in reti partizionate.

- *PN Counter:* contatore incrementabile/decrementabile che sfrutta CRDT per mantenere consistenza eventuale, ideale in ambienti distribuiti con latenza.

Le strutture AP di Hazelcast sono ideali per:
- Caching distribuito e invalidazione efficiente
- Applicazioni orientate alle prestazioni che richiedono alta disponibilità
- Ambienti con partizioni di rete o latenza variabile
- Elaborazione di eventi, messaggi e stream
- Sistemi che tollerano la consistenza eventuale in cambio di robustezza e scalabilità

=== CP Subsystem

Hazelcast offre anche un CP Subsystem, che implementa il paradigma CP del teorema CAP. Questa è una caratteristica distintiva di Hazelcast, poiché consente di scegliere il modello di consistenza più adatto per ogni caso d'uso all'interno della stessa piattaforma. Per quanto sia utile, questa funzionalità è disponibile solamente nella versione Enterprise di Hazelcast.

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
Una struttura dati distribuita che memorizza la cronologia delle modifiche apportate alle strutture dati.

== AI/ML Data Structures
Strutture dati specializzate ottimizzate per carichi di lavoro di machine learning e intelligenza artificiale.

=== Cardinality Estimator Service
Una struttura dati probabilistica per stimare in modo efficiente la cardinalità di grandi set di dati.

=== Vector Collection
Una collezione distribuita per memorizzare e interrogare vettori di embedding, utile per applicazioni di intelligenza artificiale.

== Commenti

Hazelcast fornisce un'ampia gamma di strutture dati distribuite che possono essere utilizzate per costruire applicazioni scalabili e resilienti. La possibilità di scegliere tra modelli di consistenza AP e CP consente agli sviluppatori di ottimizzare le prestazioni e consistenza in base ai requisiti specifici dell'applicazione.
