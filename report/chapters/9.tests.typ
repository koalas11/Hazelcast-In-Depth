= Test di Hazelcast per Prestazioni e Funzionalità

== Introduzione ai Test di Hazelcast

In questo capitolo esploreremo i diversi approcci di testing utilizzati per verificare le funzionalità e le prestazioni di Hazelcast. La nostra strategia di testing si divide in tre filoni principali: test delle funzionalità in Java con Hazelcast embedded, test di prestazioni in Python attraverso il client e benchmark con Hazelcast Simulator.

== Test Funzionali in Java con Hazelcast Embedded

I test in Java ci permettono di verificare l'integrazione di Hazelcast con la JVM in modalità embedded e esplorare le funzionalità core del sistema. Utilizzando Hazelcast embedded, il nodo Hazelcast viene eseguito direttamente all'interno dell'applicazione Java, rendendo più semplice l'interazione con le strutture dati distribuite e le funzionalità avanzate.

- Test delle strutture dati distribuite (Map, Queue, Set, List)
- Validazione dei meccanismi di serializzazione
- Comportamento durante gli scenari di split-brain
- Corretta implementazione dei listener
- Esplorazione degli executor service
- Esplorazione delle pipeline
- Test dei failover e della resilienza del cluster

== Test di Prestazioni in Python con Client API

Per analizzare scalabilità, concorrenza e prestazioni temporali, utilizziamo un approccio basato sul client Python con Hazelcast in container Docker. A differenza dei test Java, qui accediamo a Hazelcast come client esterno, simulando scenari di utilizzo reali, testando con diverse dimensioni del cluster. Questa metodologia ci consente di:

- Verificare la scalabilità orizzontale aggiungendo nodi al cluster
- Analizzare i tempi di risposta durante operazioni di lettura e scrittura con diversi batch size
- Analizzare i tempi di risposta dopo un failover e aggiunta di un nuovo nodo
- Analizzare la CPU e la memoria utilizzata dal cluster mentre è in idle

In questi test non andiamo a effettuare load e stress test, ma piuttosto a verificare le prestazioni in scenari di utilizzo tipici, come operazioni di lettura e scrittura su strutture dati distribuite, questo data la mancanza di Hardware dedicato per eseguire i test di carico.

=== Limitazioni del Client Python

È importante sottolineare che il client Python, pur offrendo molte funzionalità di Hazelcast, presenta alcune limitazioni rispetto alla versione embedded in Java. In particolare:

- Non è possibile creare executor service schedulati attraverso il client Python
- Alcune funzionalità avanzate di partizionamento sono accessibili solo in Java
- La gestione dei listener complessi è più limitata rispetto alla versione Java
- Molte funzionalità avanzate richiedono l'uso di Java

== Commenti

I test in Java con Hazelcast embedded ci permettono di esplorare le funzionalità e l'integrazione con la JVM. I test con il client Python forniscono dati su prestazioni e scalabilità in ambienti distribuiti da una prospettiva client. Questa combinazione garantisce una valutazione approfondita sia delle funzionalità principali che delle prestazioni in diversi scenari d'uso.
