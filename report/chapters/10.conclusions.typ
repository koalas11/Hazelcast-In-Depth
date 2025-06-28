#import "../macros.typ": plot1a, plot1b, plot2a, plot2b

= Conclusioni

== Risultati dei Test Java

Dai test in Java emerge chiaramente che Hazelcast offre un’ampia gamma di funzionalità, tutte documentate. La configurazione è molto flessibile, ma richiede una solida comprensione delle opzioni disponibili per evitare errori comuni.

Inoltre, Hazelcast segnala chiaramente eventuali errori di configurazione e programmazione, facilitando la risoluzione dei problemi. Tuttavia, la curva di apprendimento può risultare ripida per i nuovi utenti.

Nel prosieguo di questo capitolo, analizzeremo alcune funzionalità interessanti emerse durante i test, come il partizionamento personalizzato e la località dei dati, che possono migliorare significativamente le prestazioni in scenari specifici.

== Risultati Python

In questo test, si inseriscono in una mappa distribuita elementi della forma `{key: “key_i”, value: “value_i”}` con un numero e si misurano le latenze delle operazioni di lettura e scrittura, sia per operazioni singole che per batch di operazioni. Questo per osservare come le latenze variano all’aumentare del numero di nodi.

#figure(
  caption: [Confronto latenze in operazioni di lettura/scrittura],
  plot1a,
)

#figure(
  caption: [Confronto latenze in operazioni di lettura/scrittura],
  plot1b,
)

I risultati evidenziano che le operazioni di get e put presentano prestazioni migliori rispetto a get all e put all. Un aspetto interessante è che, nonostante l’aumento della dimensione del cluster, le latenze rimangono relativamente stabili, indicando che l’espansione del cluster non introduce un sovraccarico significativo nelle operazioni di lettura e scrittura. È importante sottolineare che questo test si concentra sulla latenza delle operazioni in scenari di utilizzo tipici, piuttosto che sul carico di lavoro. Inoltre, il test valuta il tempo di risposta medio per operazione, anziché il numero di operazioni per batch.

In questo test, vengono misurati i tempi di risposta successivi a un failover e all’aggiunta di un nuovo nodo al cluster, al fine di analizzare la gestione di tali scenari da parte di Hazelcast. Sono stati eseguiti due test: uno con inserimento di dati di piccole dimensioni (80 byte) e uno con dati di dimensioni maggiori (8 MB). L’obiettivo era valutare come le latenze variano in funzione della dimensione dei dati. I risultati presentati si riferiscono esclusivamente al test con dati di dimensioni maggiori, in quanto quello con dati di piccole dimensioni non ha fornito risultati significativi.

#figure(
  caption: [Confronto tempo dopo failover e aggiunta di un nodo per dati di dimensioni maggiori],
  plot2a,
)

#figure(
  caption: [Confronto tempo dopo failover e aggiunta di un nodo per dati di piccola dimensione],
  plot2b,
)

In scenari di failover, Hazelcast ha dimostrato un’elevata capacità di recupero, con tempi di ripristino pressoché invariati indipendentemente dalla quantità di dati. Questo aspetto risulta cruciale per le applicazioni mission-critical, dove la disponibilità continua è fondamentale. Tale comportamento è coerente con l’architettura di Hazelcast, che prevede la replica delle partizioni tra i nodi del cluster, garantendo la disponibilità costante dei dati anche in caso di guasti.
Si è osservato che, con dataset di piccole dimensioni, Hazelcast presenta tempi di ripristino molto rapidi.
Tuttavia, con dataset di dimensioni maggiori e un numero limitato di nodi, come ad esempio due, il tempo di ripristino risulta più lungo rispetto a cluster più grandi. Questo è dovuto al fatto che, con pochi nodi, aumenta la probabilità che un nodo contenente una partizione primaria vada in errore, comportando un tempo di ripristino più lungo.

#figure(caption: [Confronto utilizzo memoria in idle (1 nodo)], table(
  columns: (auto, auto, auto, auto),
  inset: 10pt,
  align: center + horizon,
  table.header([Nodo], [Load], [CPU Usage], [Mem (MB)]),
  [node1], [0], [1.2%], [339.03],
  [node1], [1000], [0.8%], [284.47],
  [node1], [10000], [0.9%], [284.07],
  [node1], [100000], [0.9%], [370.52],
))

#figure(caption: [Confronto utilizzo memoria in idle (2 nodi)], table(
  columns: (auto, auto, auto, auto),
  inset: 10pt,
  align: center + horizon,
  table.header([Nodo], [Load], [CPU Usage], [Mem (MB)]),
  [node1], [0], [1.9%], [429.63],
  [node1], [1000], [1.0%], [432.43],
  [node1], [10000], [0.9%], [443.64],
  [node1], [100000], [0.9%], [644.79],
  [node2], [0], [0.9%], [582.00],
  [node2], [1000], [1.0%], [557.36],
  [node2], [10000], [0.9%], [539.86],
  [node2], [100000], [1.0%], [569.31],
))

#figure(caption: [Confronto utilizzo memoria in idle (3 nodi)], table(
  columns: (auto, auto, auto, auto),
  inset: 10pt,
  align: center + horizon,
  table.header([Nodo], [Load], [CPU Usage], [Mem (MB)]),
  [node1], [0], [0.8&], [449.29],
  [node1], [1000], [1.1%], [488.95],
  [node1], [10000], [0.9%], [525.05],
  [node1], [100000], [0.8%], [554.41],

  [node2], [0], [1.7%], [442.12],
  [node2], [1000], [0.8%], [470.13],
  [node2], [10000], [0.9%], [479.36],
  [node2], [100000], [1.3%], [505.72],

  [node3], [0], [1.3%], [344.49],
  [node3], [1000], [0.9%], [358.19],
  [node3], [10000], [1.6%], [380.54],
  [node3], [100000], [0.9%], [444.15],
))

#figure(caption: [Confronto utilizzo memoria in idle (4 nodi)], table(
  columns: (auto, auto, auto, auto),
  inset: 10pt,
  align: center + horizon,
  table.header([Nodo], [Load], [CPU Usage], [Mem (MB)]),
  [node1], [0], [1.0%], [425.86],
  [node1], [1000], [0.9%], [476.61],
  [node1], [10000], [0.9%], [461.23],
  [node1], [100000], [0.9%], [477.78],

  [node2], [0], [1.5%], [328.50],
  [node2], [1000], [1.1%], [335.07],
  [node2], [10000], [1.2%], [337.49],
  [node2], [100000], [1.0%], [436.79],

  [node3], [0], [1.2%], [383.34],
  [node3], [1000], [1.2%], [384.78],
  [node3], [10000], [1.0%], [375.88],
  [node3], [100000], [1.1%], [401.27],

  [node4], [0], [1.2%], [492.81],
  [node4], [1000], [1.0%], [493.62],
  [node4], [10000], [0.9%], [466.55],
  [node4], [100000], [0.8%], [481.33],
))

#pagebreak(weak: true)
#show figure: set block(breakable: true)
#figure(caption: [Confronto utilizzo memoria in idle (5 nodi)], table(
  columns: (auto, auto, auto, auto),
  inset: 10pt,
  align: center + horizon,
  table.header([Nodo], [Load], [CPU Usage], [Mem (MB)]),
  [node1], [0], [1.1%], [468.77],
  [node1], [1000], [0.9%], [470.66],
  [node1], [10000], [0.9%], [473.05],
  [node1], [100000], [0.8%], [490.18],

  [node2], [0], [1.7%], [325.10],
  [node2], [1000], [1.9%], [392.76],
  [node2], [10000], [1.0%], [420.56],
  [node2], [100000], [1.4%], [500.22],

  [node3], [0], [1.7%], [363.15],
  [node3], [1000], [1.0%], [365.34],
  [node3], [10000], [0.9%], [366.48],
  [node3], [100000], [1.0%], [408.91],

  [node4], [0], [1.3%], [347.59],
  [node4], [1000], [0.9%], [366.12],
  [node4], [10000], [1.0%], [371.23],
  [node4], [100000], [0.9%], [400.48],

  [node5], [0], [1.1%], [328.61],
  [node5], [1000], [1.0%], [368.37],
  [node5], [10000], [0.8%], [377.05],
  [node5], [100000], [0.9%], [384.84],
))
#show figure: set block(breakable: false)

Per quanto concerne l’utilizzo di CPU e memoria in modalità idle, i risultati evidenziano che l’aumento del numero di nodi e dei dati non incide significativamente sull’utilizzo della CPU, che si mantiene in tutti i casi al di sotto del 2%. Ciò indica che l’espansione del cluster non comporta un overhead rilevante in termini di risorse CPU, consentendo di scalare senza impatti negativi sulle prestazioni.

Per quanto riguarda l’utilizzo della memoria, non si osservano variazioni significative all’aumentare di nodi e dati. Tale comportamento potrebbe essere attribuito a ottimizzazioni interne di Hazelcast che consentono una serializzazione intelligente dei dati, riducendo il consumo di memoria, oppure all’API di Docker in Python non completamente precisa nell’analizzare la memoria dei container.

In linea teorica, ci si aspetterebbe che all’aumentare del numero di nodi, la memoria utilizzata per nodo diminuisca, tenendo conto tuttavia dell’esistenza delle repliche distribuite sugli altri nodi.

== Caratteristiche Interessanti

Attraverso i test Java, sono state individuate alcune funzionalità che, combinate, risultano interessanti.

==== Limitazione principale della versione Open Source

Nonostante le numerose funzionalità disponibili nella versione open source, esiste una limitazione significativa rispetto alla versione Enterprise:

*Sicurezza:* la mancanza di funzionalità di sicurezza nella versione open source rappresenta la limitazione più critica. La versione open source non include:
- Autenticazione client-server
- Controllo degli accessi granulare
- Crittografia delle comunicazioni tra i nodi
- Integrazione con sistemi di identità aziendali

Ciò rappresenta un ostacolo per utenti o aziende che desiderano valutare Hazelcast: senza prima adottare la versione Enterprise, non è possibile esplorare le capacità del sistema in termini di sicurezza né verificarne la conformità ai requisiti aziendali.

==== Configurazione Avanzata e Personalizzazione

Hazelcast offre una configurazione altamente personalizzabile, che consente di adattare il comportamento del cluster alle specifiche esigenze dell’applicazione. Questo risulta particolarmente utile in scenari complessi in cui è necessario ottimizzare le prestazioni o la resilienza. Tuttavia, la configurazione risulta più complessa, richiedendo una buona comprensione delle opzioni disponibili.

==== Partizionamento Base

Le analisi effettuate evidenziano come, con le impostazioni predefinite, Hazelcast distribua automaticamente i dati tra i nodi del cluster in modo uniforme. Ad esempio, con due nodi e cinquecento elementi, ciascun nodo conterrà approssimativamente duecentocinquantanta elementi e metà delle ventisette partizioni.

Tuttavia, è interessante notare come offra diverse strategie di raggruppamento delle partizioni, configurabili per ottimizzare la località dei dati, migliorare le prestazioni e aumentare la resilienza. Queste strategie possono anche impedire che le repliche delle partizioni siano collocate su host appartenenti alla stessa rete o zona di disponibilità.

Tra queste, le principali sono:

- *HOST_AWARE*: i nodi che risiedono nello stesso host o rete vengono raggruppati, migliorando la resilienza in caso di guasti a livello di rete locale.

- *ZONE_AWARE*: consente di raggruppare i nodi per zona di disponibilità (availability zone), utile in ambienti cloud per prevenire la collocazione di dati e repliche nella stessa zona.

==== Partizionamento Personalizzato e Località dei Dati

Una delle funzionalità più avanzate di Hazelcast è la possibilità di implementare strategie di partizionamento personalizzate per ottimizzare la località dei dati. Ciò consente di:

1. *Collocare dati correlati sugli stessi nodi*: Implementando una `PartitionStragegy` personalizzata, è possibile garantire che i dati elaborati frequentemente risiedano sullo stesso nodo. È inoltre possibile implementare chiavi che implementano l’interfaccia `PartitionAware` per garantire che i dati associati a una chiave di partizione specifica risiedano sullo stesso membro.

2. *Esecuzione di codice con località dei dati*: Combinando il partizionamento personalizzato con l’esecuzione di codice distribuito, è possibile inviare la computazione direttamente dove risiedono i dati, minimizzando il traffico di rete.

Questa caratteristica è particolarmente rilevante per applicazioni con requisiti di latenza ultra-bassa o con elaborazioni complesse su grandi volumi di dati.

==== Data Ingestion con Pipeline e CDC

Il supporto di Hazelcast per pipeline di dati e Change Data Capture (CDC) rappresenta un vantaggio significativo per le architetture moderne. Ciò consente di ampliare un’applicazione esistente con funzionalità di streaming e integrazione continua dei dati, senza dover riscrivere l’intera logica applicativa.

==== Serializzazione e Query

Oltre a consentire la serializzazione di oggetti complessi, Hazelcast offre un potente motore di query che consente di eseguire ricerche avanzate basate su tali oggetti. Consentendo l’esecuzione di query complesse su strutture dati distribuite, Hazelcast si distingue da molte altre soluzioni in-memory che offrono solo operazioni CRUD di base.

==== Lite Member

La possibilità di utilizzare i Lite Member in Hazelcast rappresenta un’ulteriore funzionalità di rilievo. Questi nodi leggeri possono essere impiegati per operazioni di lettura e query senza partecipare attivamente alla gestione del cluster, riducendo il carico sui nodi principali e incrementando l’efficienza complessiva.

== Considerazioni Finali

Hazelcast si configura come una soluzione matura e performante per il computing distribuito in-memory, particolarmente indicata in scenari che necessitano di integrare storage e computazione con latenze minime.

La sua curva di apprendimento, piuttosto elevata, rappresenta uno dei principali ostacoli per i nuovi utenti. Tuttavia, la documentazione dettagliata e le numerose risorse disponibili contribuiscono a mitigare tale criticità.

La versione open source offre un valido punto di ingresso per sperimentare le capacità della piattaforma. Per implementazioni produttive in contesti aziendali, la versione Enterprise è generalmente preferibile, soprattutto per le sue funzionalità di sicurezza.

Le eccellenti performance rilevate nei benchmark, unite alla flessibilità dell’architettura e alla semplicità operativa, rendono Hazelcast una tecnologia da considerare seriamente per lo sviluppo di microservizi, l’elaborazione di eventi in tempo reale, il caching distribuito e il computing grid.
