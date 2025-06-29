#import "../macros.typ": all_configs, plot1a, plot1b, plot2a, plot2b, test_configs, usage_test_sizes

= Conclusioni

== Risultati dei Test Java

Dai test in Java emerge chiaramente che Hazelcast offre un'ampia gamma di funzionalità, tutte documentate. La configurazione è molto flessibile, ma richiede una solida comprensione delle opzioni disponibili per evitare errori comuni.

Inoltre, Hazelcast segnala chiaramente eventuali errori di configurazione e programmazione, facilitando la risoluzione dei problemi. Tuttavia, la curva di apprendimento può risultare ripida per i nuovi utenti.

Nel prosieguo di questo capitolo, analizzeremo alcune funzionalità interessanti emerse durante i test, come il partizionamento personalizzato e la località dei dati, che possono migliorare significativamente le prestazioni in scenari specifici.

== Risultati Python

In questo test, si inseriscono in una mappa distribuita elementi della forma `{key: “key_i”, value: “value_i”}` con un numero e si misurano le latenze delle operazioni di lettura e scrittura, sia per operazioni singole che per batch di operazioni. Questo per osservare come le latenze variano all'aumentare del numero di nodi.

#figure(
  caption: [Confronto latenze in operazioni di lettura/scrittura],
  plot1a,
)

#figure(
  caption: [Confronto latenze in operazioni di lettura/scrittura],
  plot1b,
)

I risultati evidenziano che le operazioni di get e put presentano prestazioni migliori rispetto a get all e put all. Un aspetto interessante è che, nonostante l'aumento della dimensione del cluster, le latenze rimangono relativamente stabili, indicando che l'espansione del cluster non introduce un sovraccarico significativo nelle operazioni di lettura e scrittura. È importante sottolineare che questo test si concentra sulla latenza delle operazioni in scenari di utilizzo tipici, piuttosto che sul carico di lavoro. Inoltre, il test valuta il tempo di risposta medio per operazione, anziché il numero di operazioni per batch.

In questo test, vengono misurati i tempi di risposta successivi a un failover e all'aggiunta di un nuovo nodo al cluster, al fine di analizzare la gestione di tali scenari da parte di Hazelcast. Sono stati eseguiti due test: uno con inserimento di dati di piccole dimensioni (80 byte) e uno con dati di dimensioni maggiori (8 MB). L'obiettivo era valutare come le latenze variano in funzione della dimensione dei dati.

#figure(
  caption: [Confronto tempo dopo failover e aggiunta di un nodo per dati di dimensioni maggiori],
  plot2a,
)

#figure(
  caption: [Confronto tempo dopo failover e aggiunta di un nodo per dati di piccola dimensione],
  plot2b,
)

In scenari di failover, Hazelcast ha dimostrato un'elevata capacità di recupero, con tempi di ripristino pressoché invariati indipendentemente dalla quantità di dati. Questo aspetto risulta cruciale per le applicazioni mission-critical, dove la disponibilità continua è fondamentale. Tale comportamento è coerente con l'architettura di Hazelcast, che prevede la replica delle partizioni tra i nodi del cluster, garantendo la disponibilità costante dei dati anche in caso di guasti.

Si è osservato che, con dataset di piccole dimensioni, Hazelcast presenta tempi di ripristino molto rapidi.
Tuttavia, con dataset di dimensioni maggiori e un numero limitato di nodi, come ad esempio due, il tempo di ripristino risulta più lungo rispetto a cluster più grandi. Questo è dovuto al fatto che, con pochi nodi, aumenta la probabilità che un nodo contenente una partizione primaria vada in errore, comportando un tempo di ripristino più lungo.

== Analisi delle risorse utilizzate

Tutti i test sono stati effettuati variando la configurazione di nodi del cluster e la quantità di dati gestiti. Di seguito le configurazioni di nodi utilizzati nello specifico:

_Configurazioni di Test._
#for config in test_configs {
  let node_count = all_configs.at(config).keys().len()
  [- *#config*: #node_count nodi]
}

=== Confronto delle risorse per nodo

Le tabelle che seguono mostrano i risultati dell'analisi di utilizzo delle risorse per nodo al variare della dimensioni di dati gestite.

#for size in usage_test_sizes {
  if size == "0" [
    ==== Baseline confronto (#size entries)
  ] else [
    ==== Confronto con #size entries
  ]

  for config in test_configs {
    let nodes = all_configs.at(config)
    let node_list = nodes.keys().sorted()
    let node_index = 1
    let node_count = all_configs.at(config).keys().len()

    // [==== #config (#node_list.len() nodi)]
    figure(
      caption: [Confronto utilizzo risorse in idle (#{ if node_count == 1 { [#node_count nodo totale] } else { [#node_count nodi totali] } }).],
      table(
        columns: (auto, auto, auto, auto, auto, auto),
        inset: 10pt,
        align: center + horizon,
        table.header(
          [*Nodo*],
          [*CPU (%)*],
          [*Memory (MB)*],
          strong("Network RX (" + text("KB/s", size: 10pt) + ")"),
          strong("Network TX (" + text("KB/s", size: 10pt) + ")"),
          [*Delta Memory*],
        ),

        ..for node in node_list {
          let cpu_val = if size in nodes.at(node) and "cpu" in nodes.at(node).at(size) {
            nodes.at(node).at(size).cpu
          } else { "N/A" }

          let mem_bytes = if size in nodes.at(node) and "mem_bytes" in nodes.at(node).at(size) {
            nodes.at(node).at(size).mem_bytes
          } else { "N/A" }

          let network_rx = if size in nodes.at(node) and "network_rx" in nodes.at(node).at(size) {
            nodes.at(node).at(size).network_rx
          } else { "N/A" }

          let network_tx = if size in nodes.at(node) and "network_tx" in nodes.at(node).at(size) {
            nodes.at(node).at(size).network_tx
          } else { "N/A" }

          // Calcola delta rispetto al baseline
          let delta = if (
            size != "0" and "0" in nodes.at(node) and "mem_bytes" in nodes.at(node).at("0") and mem_bytes != "N/A"
          ) {
            let baseline = float(nodes.at(node).at("0").mem_bytes)
            let current = float(mem_bytes)
            let delta_val = current - baseline
            let delta_mb = delta_val / 1024 / 1024
            if delta_mb > 0 { "+" + str(calc.round(delta_mb, digits: 1)) + " MB" } else {
              str(calc.round(delta_mb, digits: 1)) + " MB"
            }
          } else { "-" }

          (
            "Nodo " + str(node_index),
            if cpu_val != "N/A" { str(calc.round(float(cpu_val), digits: 2)) + "%" } else { cpu_val },
            if mem_bytes != "N/A" { str(calc.round(float(mem_bytes) / 1024 / 1024, digits: 1)) + " MB" } else {
              mem_bytes
            },
            if network_rx != "N/A" { str(calc.round(float(network_rx) / 1024, digits: 2)) + " KB/s" } else {
              network_rx
            },
            if network_tx != "N/A" { str(calc.round(float(network_tx) / 1024, digits: 2)) + " KB/s" } else {
              network_tx
            },
            delta,
          )
          node_index += 1
        },
      ),
    )
  }
  pagebreak(weak: true)
}

=== Confronto tra medie per dimensione

#for size in usage_test_sizes {
  // if size == "0" [
  //   ===== Confronto Baseline
  // ] else [
  //   ===== Confronto #size entries
  // ]

  figure(caption: [Confronto utilizzo risorse (#size entries)], table(
    columns: (auto, auto, auto, auto, auto),
    align: center,
    inset: .75em,
    table.header(
      [*\# Nodi*], [*CPU Media (%)*], [*Memory Media (MB)*], [*Network RX Media (KB/s)*], [*Network TX Media (KB/s)*]
    ),

    ..for config in test_configs {
      let nodes = all_configs.at(config)
      let node_list = nodes.keys().sorted()

      // Calcola medie
      let cpu_sum = 0
      let mem_sum = 0
      let rx_sum = 0
      let tx_sum = 0
      let valid_nodes = 0

      for node in node_list {
        if size in nodes.at(node) {
          if "cpu" in nodes.at(node).at(size) {
            cpu_sum += float(nodes.at(node).at(size).cpu)
          }
          if "mem_bytes" in nodes.at(node).at(size) {
            mem_sum += float(nodes.at(node).at(size).mem_bytes)
          }
          if "network_rx" in nodes.at(node).at(size) {
            rx_sum += float(nodes.at(node).at(size).network_rx)
          }
          if "network_tx" in nodes.at(node).at(size) {
            tx_sum += float(nodes.at(node).at(size).network_tx)
          }
          valid_nodes += 1
        }
      }

      (
        str(node_list.len()),
        if valid_nodes > 0 { str(calc.round(cpu_sum / valid_nodes, digits: 2)) + "%" } else { "N/A" },
        if valid_nodes > 0 { str(calc.round(mem_sum / valid_nodes / 1024 / 1024, digits: 1)) + " MB" } else { "N/A" },
        if valid_nodes > 0 { str(calc.round(rx_sum / valid_nodes / 1024, digits: 2)) + " KB/s" } else { "N/A" },
        if valid_nodes > 0 { str(calc.round(tx_sum / valid_nodes / 1024, digits: 2)) + " KB/s" } else { "N/A" },
      )
    },
  ))
}

Per quanto concerne l'utilizzo di CPU, memoria, disco e rete in modalità idle, i risultati evidenziano che l'aumento del numero di nodi e dei dati non incide significativamente sull'utilizzo della CPU, che si mantiene in tutti i casi al di sotto del 2%. Ciò indica che l'espansione del cluster non comporta un overhead rilevante in termini di risorse CPU, consentendo di scalare senza impatti negativi sulle prestazioni.

Per quanto riguarda l'utilizzo del disco, si osserva che, in modalità idle, non viene effettuato alcun utilizzo. Questo è coerente con il fatto che Hazelcast è progettato per operare principalmente in-memory, evitando operazioni su disco (ed è per questo che nelle tabelle non viene riportato).

Per quanto riguarda l'utilizzo della memoria in Hazelcast, si osserva un incremento proporzionale all'aumentare del numero di nodi e della dimensione dei dati. Tuttavia, aggiungere un singolo nodo non è sufficiente per ottenere un miglioramento significativo. Ad esempio, passando da 1 a 2 nodi, la memoria utilizzata può risultare simile o addirittura maggiore a causa dei meccanismi di replica. Anche il passaggio da 2 a 3 nodi non comporta necessariamente un guadagno sostanziale. Per vedere un'effettiva differenza nell'efficienza dell'utilizzo della memoria e nella distribuzione del carico, è necessario aggiungere un numero più consistente di nodi al cluster.

Infine riguardo all'utilizzo della rete, si osserva che, in modalità idle, l'attività di rete è minima, ma comunque presente. Inoltre, tende all'aumenta all'aumentare del numero di nodi e non sembrerebbe influenzato dalla quantità di dati. Questo è un comportamento atteso data la natura peer-to-peer di Hazelcast, in cui i nodi comunicano tra loro segnali di heartbeat e altre informazioni di stato quando in modalità idle.

#pagebreak(weak: true)

== Caratteristiche Interessanti

Attraverso i test Java, sono state individuate alcune funzionalità che, combinate, risultano interessanti.

=== Limitazione principale della versione Open Source

Nonostante le numerose funzionalità disponibili nella versione open source, esiste una limitazione significativa rispetto alla versione Enterprise:

*Sicurezza:* la mancanza di funzionalità di sicurezza nella versione open source rappresenta la limitazione più critica. La versione open source non include:
- Autenticazione client-server
- Controllo degli accessi granulare
- Crittografia delle comunicazioni tra i nodi
- Integrazione con sistemi di identità aziendali

Ciò rappresenta un ostacolo per utenti o aziende che desiderano valutare Hazelcast: senza prima adottare la versione Enterprise, non è possibile esplorare le capacità del sistema in termini di sicurezza né verificarne la conformità ai requisiti aziendali.

=== Configurazione Aavanzata e Personalizzazione

Hazelcast offre una configurazione altamente personalizzabile, che consente di adattare il comportamento del cluster alle specifiche esigenze dell'applicazione. Questo risulta particolarmente utile in scenari complessi in cui è necessario ottimizzare le prestazioni o la resilienza. Tuttavia, la configurazione risulta più complessa, richiedendo una buona comprensione delle opzioni disponibili.

=== Tolleranza ai guasti (Strutture AP)

Dai test effettuati, abbiamo riscontrato che le query SQL e le transazioni 2PC non sono resilienti ai guasti. In caso di fallimento di un nodo durante l'esecuzione di una query o di una transazione, il sistema non riesce a completare l'operazione e restituisce un errore. Questo comportamento è comprensibile, considerando la natura distribuita di Hazelcast, in cui le operazioni che coinvolgono più nodi devono essere gestite con attenzione per garantire la coerenza dei dati.

Al contrario, abbiamo osservato che l'utilizzo dell'API Predicate per le query offre una maggiore resilienza ai guasti. Se un nodo fallisce durante l'esecuzione di una query Predicate, il sistema riesce comunque a completare l'operazione sugli altri nodi, restituendo i risultati disponibili. Tali risultati potrebbero non essere completamente coerenti, ma riflettono i compromessi previsti dal teorema CAP.

=== Partizionamento Base

Le analisi effettuate evidenziano come, con le impostazioni predefinite, Hazelcast distribua automaticamente i dati tra i nodi del cluster in modo uniforme. Ad esempio, con due nodi e cinquecento elementi, ciascun nodo conterrà approssimativamente duecentocinquantanta elementi e metà delle ventisette partizioni.

Tuttavia, è interessante notare come offra diverse strategie di raggruppamento delle partizioni, configurabili per ottimizzare la località dei dati, migliorare le prestazioni e aumentare la resilienza. Queste strategie possono anche impedire che le repliche delle partizioni siano collocate su host appartenenti alla stessa rete o zona di disponibilità.

Tra queste, le principali sono:

- *HOST_AWARE*: i nodi che risiedono nello stesso host o rete vengono raggruppati, migliorando la resilienza in caso di guasti a livello di rete locale.

- *ZONE_AWARE*: consente di raggruppare i nodi per zona di disponibilità (availability zone), utile in ambienti cloud per prevenire la collocazione di dati e repliche nella stessa zona.

=== Partizionamento Personalizzato e Località dei Dati

Una delle funzionalità più avanzate di Hazelcast è la possibilità di implementare strategie di partizionamento personalizzate per ottimizzare la località dei dati. Ciò consente di:

1. *Collocare dati correlati sugli stessi nodi*: Implementando una `PartitionStragegy` personalizzata, è possibile garantire che i dati elaborati frequentemente risiedano sullo stesso nodo. È inoltre possibile implementare chiavi che implementano l'interfaccia `PartitionAware` per garantire che i dati associati a una chiave di partizione specifica risiedano sullo stesso membro.

2. *Esecuzione di codice con località dei dati*: Combinando il partizionamento personalizzato con l'esecuzione di codice distribuito, è possibile inviare la computazione direttamente dove risiedono i dati, minimizzando il traffico di rete.

Questa caratteristica è particolarmente rilevante per applicazioni con requisiti di latenza ultra-bassa o con elaborazioni complesse su grandi volumi di dati.

=== Data Ingestion con Pipeline e CDC

Il supporto di Hazelcast per pipeline di dati e Change Data Capture (CDC) rappresenta un vantaggio significativo per le architetture moderne. Ciò consente di ampliare un'applicazione esistente con funzionalità di streaming e integrazione continua dei dati, senza dover riscrivere l'intera logica applicativa.

=== Serializzazione e Query

Oltre a consentire la serializzazione di oggetti complessi, Hazelcast offre un potente motore di query che consente di eseguire ricerche avanzate basate su tali oggetti. Consentendo l'esecuzione di query complesse su strutture dati distribuite, Hazelcast si distingue da molte altre soluzioni in-memory che offrono solo operazioni CRUD di base.

=== Lite Member

La possibilità di utilizzare i Lite Member in Hazelcast rappresenta un'ulteriore funzionalità di rilievo. Questi nodi leggeri possono essere impiegati per operazioni di lettura e query senza partecipare attivamente alla gestione del cluster, riducendo il carico sui nodi principali e incrementando l'efficienza complessiva.

== Considerazioni Finali

Hazelcast si configura come una soluzione matura e performante per il computing distribuito in-memory, particolarmente indicata in scenari che necessitano di integrare storage e computazione con latenze minime.

La sua curva di apprendimento, piuttosto elevata, rappresenta uno dei principali ostacoli per i nuovi utenti. Tuttavia, la documentazione dettagliata e le numerose risorse disponibili contribuiscono a mitigare tale criticità.

La versione open source offre un valido punto di ingresso per sperimentare le capacità della piattaforma. Per implementazioni produttive in contesti aziendali, la versione Enterprise è generalmente preferibile, soprattutto per le sue funzionalità di sicurezza.

Le eccellenti performance rilevate nei benchmark, unite alla flessibilità dell'architettura e alla semplicità operativa, rendono Hazelcast una tecnologia da considerare seriamente per lo sviluppo di microservizi, l'elaborazione di eventi in tempo reale, il caching distribuito e il computing grid, come spiegato nel capitolo 7.
