= Test di Hazelcast per Prestazioni e Funzionalità

== Introduzione ai Test di Hazelcast

In questo capitolo esploreremo i diversi approcci di testing utilizzati per verificare le funzionalità e le prestazioni di Hazelcast. La nostra strategia di testing si divide in tre filoni principali: test delle funzionalità in Java con Hazelcast embedded e test di prestazioni in Python attraverso il client.

== Test Funzionali in Java con Hazelcast Embedded

I test in Java ci permettono di verificare l'integrazione di Hazelcast con la JVM in modalità embedded e esplorare le funzionalità core del sistema. Utilizzando Hazelcast embedded, il nodo Hazelcast viene eseguito direttamente all'interno dell'applicazione Java, rendendo più semplice l'interazione con le strutture dati distribuite e le funzionalità avanzate.

- Test delle strutture dati distribuite (Map, Queue, Set, List)
- Validazione dei meccanismi di serializzazione
- Verifica della gestione delle partizioni
- Comportamento durante gli scenari di split-brain
- Corretta implementazione dei listener
- Esplorazione degli executor service
- Esplorazione delle pipeline
- Test dei failover e della resilienza del cluster

== Test di Prestazioni in Python con Client API

In questi test utilizziamo un approccio basato sul client Python con Hazelcast in container Docker. A differenza dei test Java, qui accediamo a Hazelcast come client esterno, simulando scenari di utilizzo reali, testando con diverse dimensioni del cluster. Questa metodologia ci consente di:

- Analizzare i tempi di risposta durante operazioni di lettura e scrittura con diversi batch size
- Analizzare i tempi di risposta dopo un failover e aggiunta di un nuovo nodo
- Analizzare la CPU e la memoria utilizzata dal cluster mentre è in idle

In questi test andiamo ad effettuare dei test semplici che non rispecchiano tutti i possibili scenari reali, questo data la mancanza di Hardware dedicato per eseguire i test più sofisticati.

== Commenti

I test in Java con Hazelcast embedded ci permettono di esplorare le funzionalità e l'integrazione con la JVM. I test con il client Python forniscono dati su prestazioni e scalabilità in ambienti distribuiti da una prospettiva client. Questa combinazione garantisce una valutazione approfondita sia delle funzionalità principali che delle prestazioni in diversi scenari d'uso.
