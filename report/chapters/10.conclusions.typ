#import "../macros.typ": all_configs, plot1a, plot1b, plot2a, plot2b, test_configs, usage_test_sizes

= Conclusioni

== Risultati dei Test Java

Dai test in Java emerge chiaramente che Hazelcast offre un'ampia gamma di funzionalità, tutte documentate. La configurazione è molto flessibile, ma richiede una solida comprensione delle opzioni disponibili per evitare errori comuni.

Inoltre, Hazelcast segnala chiaramente eventuali errori di configurazione e programmazione, facilitando la risoluzione dei problemi. Tuttavia, la curva di apprendimento può risultare ripida per i nuovi utenti.

Nel prosieguo di questo capitolo, analizzeremo alcune funzionalità interessanti emerse durante i test, come il partizionamento personalizzato e la località dei dati, che possono migliorare significativamente le prestazioni in scenari specifici.

== Analisi Latenze

=== Test di Latenza Operazioni di Lettura/Scrittura

In questo test, si inseriscono in una mappa distribuita elementi della forma `{key: “key_i”, value: “value_i”}` con `i` un numero e si misurano le latenze delle operazioni di lettura e scrittura, sia per operazioni singole che per batch di operazioni. Questo per osservare come le latenze variano all'aumentare del numero di nodi. Vengono prima inseriti un dato alla volta con get/put e successivamente con get all/put all.

#figure(
  caption: [Confronto latenze in operazioni di lettura/scrittura],
  plot1a,
)

#figure(
  caption: [Confronto latenze in operazioni di lettura/scrittura],
  plot1b,
)

I risultati evidenziano che le operazioni di get e put presentano prestazioni peggiori rispetto a get all e put all. Un aspetto interessante è che, nonostante l'aumento della dimensione del cluster, le latenze rimangono relativamente stabili, indicando che l'espansione del cluster non introduce un sovraccarico significativo nelle operazioni di lettura e scrittura. È importante sottolineare che questo test si concentra sulla latenza delle operazioni, piuttosto che sul carico di lavoro.

=== Test di Latenza dopo Failover e Aggiunta di un Nodo

In questo test, vengono misurati i tempi di ripristino dello stato del cluster successivi a un failover e all'aggiunta di un nuovo nodo al cluster, al fine di analizzare la gestione di tali scenari da parte di Hazelcast. Sono stati eseguiti due test: uno con inserimento di dati di piccole dimensioni (80 byte) e uno con dati di dimensioni maggiori (8 MB). L'obiettivo era valutare come le latenze variano in funzione della dimensione dei dati.
Inoltre andiamo ad analizzare l'utilizzo di rete durante il failover e l'aggiunta di un nodo.
Quindi ad esempio con 3 nodi, viene simulato un failover di un nodo e successivamente viene aggiunto un nuovo nodo al cluster, misurando i tempi di ripristino e l'utilizzo di rete.

#figure(
  caption: [Confronto tempo dopo failover e aggiunta di un nodo per dati di dimensioni maggiori],
  plot2a,
)

#figure(
  caption: [Confronto tempo dopo failover e aggiunta di un nodo per dati di piccola dimensione],
  plot2b,
)

In scenari di failover, Hazelcast richiede generalmente più tempo per il ripristino dopo un failover rispetto all'aggiunta di un nodo. Questo è comprensibile, poiché il failover avviene in condizioni impreviste, dove uno o più nodi risultano improvvisamente non disponibili. In tali situazioni, il sistema deve prima rilevare il guasto, riequilibrare le partizioni, riassegnare i ruoli primari alle repliche secondarie e ristabilire la consistenza interna del cluster. Questi passaggi, inevitabilmente, introducono una latenza maggiore rispetto all'aggiunta di un nodo, dove le risorse e i dati possono essere gestiti in modo coordinato e proattivo.

Si è osservato che, all'aumentare del numero di nodi nel cluster, i tempi tendono a ridursi, soprattutto con dataset di grandi dimensioni. Questo è sensato, in quanto una maggiore distribuzione delle partizioni tra i nodi riduce la probabilità che un singolo nodo contenga troppe partizioni primarie, migliorando la resilienza complessiva del sistema. Di conseguenza, i cluster più grandi, come ad esempio di cinque nodi, riescono a gestire i guasti in modo più efficiente rispetto a configurazioni con un numero ridotto di nodi, come ad esempio due.

Inoltre, anche la trasmissione e la ricezione di dati sulla rete beneficiano dell'aumento del numero di nodi: la suddivisione del carico tra più componenti riduce la latenza e contribuisce ulteriormente a tempi di ripristino più rapidi. Anche questo comportamento è coerente rispetto al metodo di partizionamento.

== Analisi delle risorse utilizzate

In questo test andiamo a analizzare l'utilizzo di risorse in idle di un cluster hazelcast, i test sono stati effettuati variando il numero di nodi del cluster e la quantità di dati gestiti.

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

Per quanto concerne l'utilizzo di CPU, i risultati evidenziano che l'aumento del numero di nodi e dei dati non incide significativamente, che si mantiene in tutti i casi al di sotto del 2%. Ciò indica che l'espansione del cluster non comporta un overhead rilevante in termini di risorse CPU, consentendo di scalare senza impatti negativi sulla CPU.

Per quanto riguarda l'utilizzo del disco, si osserva che, in modalità idle, non viene effettuato alcun utilizzo. Questo è coerente con il fatto che Hazelcast è progettato per operare principalmente in-memory, evitando operazioni su disco (ed è per questo che nelle tabelle non viene riportato).

Per quanto riguarda l'utilizzo della memoria in Hazelcast, si osserva una diminuzione proporzionale all'aumentare del numero di nodi e della dimensione dei dati. Tuttavia, aggiungere un singolo nodo non è sufficiente per ottenere un miglioramento significativo. Ad esempio, passando da 1 a 2 nodi, la memoria utilizzata può risultare simile o addirittura maggiore a causa dei meccanismi di replica. Anche il passaggio da 2 a 3 nodi non comporta necessariamente un guadagno sostanziale. Per vedere un'effettiva differenza nell'efficienza dell'utilizzo della memoria e nella distribuzione del carico, è necessario aggiungere un numero più consistente di nodi al cluster, come ad esempio notando la differenza tra 2 e 5 nodi.

Infine riguardo all'utilizzo della rete, si osserva che, in modalità idle, l'attività di rete è minima, ma comunque presente. Inoltre, tende ad aumentare all'aumentare del numero di nodi e non sembra influenzato dalla quantità di dati. Questo è un comportamento atteso data la natura peer-to-peer di Hazelcast, in cui i nodi comunicano tra loro segnali di heartbeat e altre informazioni di stato quando in modalità idle.

#pagebreak(weak: true)

== Caratteristiche Interessanti

Attraverso i test Java precedentemente effettuati, sono state individuate alcune caratteristiche e funzionalità di Hazelcast che, combinate, risultano interessanti o problematiche.

=== Limitazione principale della versione Open Source

Nonostante le numerose funzionalità disponibili nella versione open source, esiste una limitazione significativa rispetto alla versione Enterprise:

*Sicurezza:* la mancanza di funzionalità di sicurezza nella versione open source rappresenta la limitazione più critica. La versione open source non include:
- Autenticazione client-server
- Controllo degli accessi granulare
- Crittografia delle comunicazioni tra i nodi
- Integrazione con sistemi di identità aziendali

Ciò rappresenta un ostacolo per utenti o aziende che desiderano valutare Hazelcast: senza prima adottare la versione Enterprise, non è possibile esplorare le capacità del sistema in termini di sicurezza né verificarne la conformità ai requisiti aziendali.

=== Limitazione tra diversi linguaggi

Hazelcast offre un'API client per diversi linguaggi di programmazione, ma esistono alcune limitazioni significative tra le versioni client. In particolare, la versione Java offre funzionalità avanzate che non sono disponibili in altri linguaggi, come Python.

Questo aspetto è importante da considerare quando si vuole utilizzare Hazelcast in un ambiente multi-linguaggio. La versione Java consente di sfruttare appieno le potenzialità del sistema, mentre le versioni client in altri linguaggi potrebbero non supportare tutte le funzionalità e quindi risultare un problema durante lo sviluppo.

=== Configurazione Avanzata e Personalizzazione

Hazelcast offre una configurazione altamente personalizzabile, che consente di adattare il comportamento del cluster alle specifiche esigenze dell'applicazione. Questo risulta particolarmente utile in scenari complessi in cui è necessario ottimizzare le prestazioni o la resilienza. Tuttavia, la configurazione risulta più complessa, richiedendo una buona comprensione delle opzioni disponibili.

=== Tolleranza ai guasti (Strutture AP)

Dai test effettuati, abbiamo riscontrato che le query SQL e le transazioni 2PC non sono resilienti ai guasti. In caso di fallimento di un nodo durante l'esecuzione di una query o di una transazione, il sistema non riesce a completare l'operazione e restituisce un errore.

Perché le transazioni 2PC falliscono: Il protocollo Two-Phase Commit richiede che tutti i nodi partecipanti confermino l'operazione in entrambe le fasi (prepare e commit). Se un nodo fallisce durante questo processo, il coordinatore non può ottenere il consenso unanime necessario per garantire la consistenza ACID, causando l'abort della transazione. Questo comportamento è fondamentale per evitare stati inconsistenti nei dati distribuiti.

Perché le query SQL falliscono: Le query SQL in Hazelcast spesso richiedono l'aggregazione di dati da multiple partizioni distribuite su diversi nodi. Durante l'esecuzione, se un nodo contenente partizioni necessarie per completare la query diventa non disponibile, il motore SQL non può produrre un risultato completo e accurato, preferendo fallire piuttosto che restituire dati parziali o potenzialmente inconsistenti.

Al contrario, abbiamo osservato che l'utilizzo dell'API Predicate per le query offre una maggiore resilienza ai guasti. Le query Predicate operano in modalità "best effort": se un nodo fallisce durante l'esecuzione, il sistema riesce comunque a completare l'operazione sugli altri nodi disponibili, restituendo i risultati delle partizioni accessibili. Questo approccio privilegia la disponibilità (Availability) rispetto alla consistenza (Consistency), riflettendo i compromessi previsti dal teorema CAP in un sistema distribuito che sceglie di rimanere operativo anche in presenza di fallimenti.

=== Partizionamento Base

Le analisi effettuate evidenziano come, con le impostazioni predefinite, Hazelcast distribua automaticamente i dati tra i nodi del cluster in modo uniforme. Ad esempio, con due nodi e cinquecento elementi, ciascun nodo conterrà approssimativamente 250 elementi e metà delle 271 partizioni.

Tuttavia, è interessante notare come Hazelcast offra diverse strategie di raggruppamento delle partizioni, configurabili per ottimizzare la località dei dati, migliorare le prestazioni e aumentare la resilienza. Queste strategie possono anche impedire che le repliche delle partizioni siano collocate su host appartenenti alla stessa rete o zona di disponibilità.

Tra queste, le principali sono:

- *HOST_AWARE*: i nodi che risiedono nello stesso host o rete vengono raggruppati, migliorando la resilienza in caso di guasti a livello di rete locale.

- *ZONE_AWARE*: consente di raggruppare i nodi per zona di disponibilità (availability zone), utile in ambienti cloud per prevenire la collocazione di dati e repliche nella stessa zona.

=== Partizionamento Personalizzato e Località dei Dati

Una delle funzionalità avanzate di Hazelcast è la possibilità di implementare strategie di partizionamento personalizzate per ottimizzare la località dei dati. Ciò consente di:

1. *Collocare dati correlati sugli stessi nodi*: Implementando una `PartitionStragegy` personalizzata, è possibile garantire che i dati elaborati frequentemente risiedano sullo stesso nodo. È inoltre possibile implementare chiavi che implementano l'interfaccia `PartitionAware` per garantire che i dati associati a una chiave di partizione specifica risiedano sullo stesso membro.

2. *Esecuzione di codice con località dei dati*: Combinando il partizionamento personalizzato con l'esecuzione di codice distribuito, è possibile inviare la computazione direttamente dove risiedono i dati, minimizzando il traffico di rete.

Questa caratteristica è particolarmente rilevante per applicazioni con requisiti di latenza ultra-bassa o con elaborazioni complesse su grandi volumi di dati.

=== Durable Executor Service

Una delle funzionalità più interessanti offerte da Hazelcast è il Durable Executor Service, che consente l'esecuzione di task asincroni con garanzie di persistenza. A differenza degli executor tradizionali, i Durable Executor mantengono lo stato delle operazioni anche in caso di failover dei nodi, assicurando così la continuità dell'elaborazione e l'integrità del processo.

Questa caratteristica si può rilevare interessante ad esempio generazione di report sui dati, con la sicurezza che, anche in caso di guasti, i task in esecuzione non vengano persi e possano essere ripresi una volta che il cluster torna operativo.

=== Data Ingestion con Pipeline e CDC

Il supporto di Hazelcast per pipeline di dati e Change Data Capture (CDC) rappresenta un vantaggio significativo per le architetture moderne. Ciò consente di ampliare un'applicazione esistente con funzionalità di streaming e integrazione continua dei dati, senza dover riscrivere l'intera logica applicativa.

=== Serializzazione e Query

Oltre a consentire la serializzazione di oggetti complessi, Hazelcast offre un potente motore di query che permette di eseguire ricerche avanzate basate su tali oggetti, come illustrato nel Capitolo 5. Inoltre, grazie all'utilizzo del compat serializer, è possibile serializzare oggetti in modo compatibile tra diverse versioni della stessa classe, facilitando così l'evoluzione del modello dati senza compromettere la compatibilità con le versioni precedenti. Si tratta di una caratteristica particolarmente utile in scenari in cui le modifiche al modello dati sono frequenti e il supporto per la retrocompatibilità è fondamentale.

=== Lite Member

La possibilità di utilizzare i Lite Member in Hazelcast rappresenta un'ulteriore funzionalità di rilievo. Questi nodi leggeri possono essere impiegati per operazioni di lettura e query senza partecipare attivamente alla gestione del cluster, riducendo il carico sui nodi principali e incrementando l'efficienza complessiva.

=== Strutture Dati per l'AI

Una struttura dati distribuita poco esplorata ma di grande interesse è la Vector Collection di Hazelcast, che consente la memorizzazione e gestione di vettori ad alta dimensione. Questa struttura risulta particolarmente utile in ambito intelligenza artificiale e machine learning, dove la gestione efficiente di grandi volumi di dati vettoriali è fondamentale.

Inoltre, l'integrazione con le data pipeline apre le porte a scenari applicativi avanzati, come ad esempio un sistema di ricerca immagini basato su vettori: le immagini vengono convertite in vettori e indicizzate per una ricerca rapida e scalabile. Grazie alle pipeline, l'indice può essere aggiornato in tempo reale con nuove immagini, garantendo una gestione continua e dinamica dei dati.

Un esempio pratico è disponibile nella documentazione ufficiale di Hazelcast al seguente link: https://docs.hazelcast.com/hazelcast/5.5/data-structures/vector-search-tutorial

== Considerazioni Finali

Hazelcast si configura come una soluzione matura e performante per il computing distribuito in-memory, particolarmente indicata in scenari che necessitano di integrare storage e computazione con latenze minime.

La sua curva di apprendimento, piuttosto elevata, rappresenta uno dei principali ostacoli per i nuovi utenti. Tuttavia, la documentazione dettagliata e le numerose risorse disponibili contribuiscono a mitigare tale criticità.

La versione open source offre un valido punto di ingresso per sperimentare le capacità della piattaforma. Per implementazioni produttive in contesti aziendali, la versione Enterprise è generalmente preferibile, soprattutto per le sue funzionalità di sicurezza.

Hazelcast si distingue per la sua architettura completa, che unisce storage ed elaborazione distribuita dei dati in un'unica soluzione. Questo consente di evitare l'adozione e l'integrazione di più tecnologie differenti, semplificando notevolmente l'architettura dei sistemi. Al tempo stesso, Hazelcast offre un'elevata interoperabilità, grazie alla possibilità di integrarsi facilmente con numerose tecnologie e ambienti esistenti.
