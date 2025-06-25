= Conclusioni

== Risultati dei Test Java

Dai test in Java emerge chiaramente che Hazelcast offre un'ampia gamma di funzionalità, tutte ben documentate. La configurazione è molto flessibile, ma richiede una solida comprensione delle opzioni disponibili per evitare errori comuni.

Inoltre, Hazelcast segnala chiaramente eventuali errori di configurazione, facilitando la risoluzione dei problemi. Tuttavia, la curva di apprendimento può risultare ripida per i nuovi utenti, in particolare per chi non ha familiarità con i concetti di calcolo distribuito.

== Risultati dei Benchmark

I benchmark condotti durante questo studio hanno confermato molte delle caratteristiche distintive di Hazelcast. Utilizzando la metodologia descritta nel capitolo sui test, abbiamo valutato le prestazioni di Hazelcast in diversi scenari operativi.

#figure(
  caption: [Confronto latenze in operazioni di lettura/scrittura],
  // Spazio per grafico di latenza: x operazione, y tempi
  [],
)

I risultati mostrano che Hazelcast mantiene le promesse di latenza sub-millisecondo per le operazioni in-memory, posizionandosi favorevolmente rispetto ad altre tecnologie distribuite. Particolarmente notevole è la capacità di mantenere prestazioni stabili all'aumentare del carico.

#figure(
  caption: [Scalabilità con incremento di nodi],
  // Spazio per grafico di scalabilità: x thread, y ops/sec
  [],
)

Nel test avendo più thread che eseguono operazioni di lettura e scrittura, Hazelcast ha dimostrato una scalabilità lineare, con un incremento delle operazioni al secondo proporzionale al numero di thread. Questo è un indicatore chiave della capacità di Hazelcast di gestire carichi elevati in scenari reali.

#figure(
  caption: [Confronto tempo dopo failover e aggiunta di un nodo],
  // Spazio per grafico di confronto: x quantità di dati, y tempo di recupero
  [],
)

In scenari di failover, Hazelcast ha dimostrato una rapida capacità di recupero, con tempi di ripristino che non hanno grosse variazioni rispetto alla quantità di dati. Questo è un aspetto cruciale per applicazioni mission-critical dove la disponibilità continua è fondamentale. Tale comportamento è coerente con l'architettura di Hazelcast, che prevede la replica delle partizioni tra i nodi del cluster, garantendo che i dati siano sempre disponibili anche in caso di guasti.

== Limitazione Principale della Versione Open Source

Nonostante le numerose funzionalità disponibili nella versione open source, esiste una limitazione significativa rispetto alla versione Enterprise:

*Sicurezza*: La mancanza di funzionalità di sicurezza nella versione open source rappresenta la limitazione più critica. La versione open source non include:
- Autenticazione client-server
- Controllo degli accessi granulare
- Crittografia delle comunicazioni tra i nodi
- Integrazione con sistemi di identità aziendali

Questo rappresenta un ostacolo per utenti o aziende che desiderano valutare Hazelcast: senza prima adottare la versione Enterprise, non è possibile esplorare le capacità del sistema in termini di sicurezza né verificarne l'aderenza ai requisiti aziendali.

== Caratteristiche Interessanti

Attraverso i test Java, abbiamo individuato alcune funzionalità che combinate risultano interessanti.

==== Configurazione Avanzata e Personalizzazione

Hazelcast offre una configurazione altamente personalizzabile, che consente di adattare il comportamento del cluster alle specifiche esigenze dell'applicazione. Questo è particolarmente utile in scenari complessi dove è necessario ottimizzare le prestazioni o la resilienza.
Ma rende anche la configurazione più complessa, richiedendo una buona comprensione delle opzioni disponibili.

==== Partizionamento Personalizzato e Località dei Dati

Una delle funzionalità più potenti di Hazelcast è la possibilità di implementare strategie di partizionamento personalizzate per ottimizzare la località dei dati. Questo permette di:

1. *Collocare dati correlati sugli stessi nodi*: Implementando un `PartitioningStrategy` personalizzato, è possibile garantire che dati che vengono spesso elaborati insieme risiedano sullo stesso nodo.

2. *Esecuzione di codice con data locality*: Combinando il custom partitioning con l'esecuzione di codice distribuito, è possibile inviare la computazione direttamente dove risiedono i dati, minimizzando il traffico di rete.

Questa caratteristica è particolarmente rilevante per applicazioni con requisiti di latenza ultra-bassa o con elaborazioni complesse su grandi volumi di dati.

==== Data Ingestion con Pipeline e CDC

Il fatto che Hazelcast supporti pipeline di dati e Change Data Capture (CDC) rappresenta un vantaggio significativo per le architetture moderne. Questo perchè consente di ampliare una applicazione esistente con funzionalità di streaming e integrazione continua dei dati, senza dover riscrivere l'intera logica applicativa.

==== Serializzazione e Query

Hazelcast oltre a permettere la serializzazione di oggetti complessi, offre anche un potente motore di query che consente di eseguire ricerche avanzate basandosi su di essi. Permettendo  di eseguire query complesse su strutture dati distribuite, Hazelcast si distingue da molte altre soluzioni in-memory che offrono solo operazioni CRUD di base.

==== Lite Member

La possibilità di utilizzare i "Lite Member" in Hazelcast è un'altra caratteristica interessante. Questi nodi leggeri possono essere utilizzati per operazioni di lettura e query senza partecipare attivamente alla gestione del cluster, riducendo il carico sui nodi principali e migliorando l'efficienza complessiva.

== Considerazioni Finali

Hazelcast rappresenta una soluzione matura e performante per il computing distribuito in-memory, particolarmente indicata in scenari che richiedono l'integrazione di storage e computazione con latenze minime.

La sua curva di apprendimento, piuttosto elevata, rappresenta uno dei principali ostacoli per i nuovi utenti. Tuttavia, la documentazione dettagliata e le numerose risorse disponibili contribuiscono a mitigare tale criticità.

La versione open source offre un valido punto di ingresso per sperimentare le capacità della piattaforma. Per implementazioni produttive in contesti aziendali, la versione Enterprise è generalmente preferibile, soprattutto per le sue funzionalità di sicurezza.

Le eccellenti performance rilevate nei benchmark, unite alla flessibilità dell'architettura e alla semplicità operativa, rendono Hazelcast una tecnologia da considerare seriamente per lo sviluppo di microservizi, l'elaborazione di eventi in tempo reale, il caching distribuito e il computing grid.
