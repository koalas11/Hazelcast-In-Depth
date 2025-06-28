= Introduzione

Hazelcast è una piattaforma di computing distribuito in-memory che offre un sistema di gestione dei dati ad alte prestazioni progettato per applicazioni moderne. Fondata nel 2008, Hazelcast si è evoluta da una semplice griglia di dati in-memory a una piattaforma completa per l'elaborazione di stream e l'analisi in tempo reale.

Hazelcast è sviluppato in Java e fornisce client per numerosi linguaggi, tra cui C\#, Python, Node.js e Go.

== Componenti principali

Il nucleo dell'ecosistema Hazelcast include:

- *IMDG (In-Memory Data Grid)*: La componente fondamentale che fornisce strutture dati distribuite come mappe, code, set e liste.
- *Jet*: Il motore di elaborazione di stream che consente l'analisi in tempo reale su dati in movimento.
- *Management Center*: Un'interfaccia web per il monitoraggio e la gestione dei cluster Hazelcast.

La versione di Hazelcast analizzata è la 5.5 l'ultima versione stabile al momento della stesura di questo documento.

In questa relazione, esploreremo le caratteristiche principali di Hazelcast, il suo funzionamento interno e le differenze tra le versioni open source ed enterprise. Inoltre eseguiremo dei test su un cluster di nodi Hazelcast per valutare le performance e la scalabilità della piattaforma. Infine faremo un confronto con altre soluzioni di computing distribuito in-memory, evidenziando i punti di forza e le aree di miglioramento di Hazelcast.
