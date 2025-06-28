#import "../packages.typ": cetz, codly, codly-languages

= Distributed Computing in Hazelcast

Hazelcast oltre a essere usato come database distribuito può essere usato per effettuare il distributed computing. Il distributed computing in Hazelcast permette di distribuire il carico di lavoro tra i membri del cluster, migliorando le prestazioni e la scalabilità delle applicazioni.

In questo capitolo esploreremo due meccanismi fondamentali per il computing distribuito in Hazelcast: gli *Executor Services* e gli *User-Code Namespaces*. Queste funzionalità consentono di eseguire codice in modo distribuito e di gestire in maniera efficiente l'isolamento e il deployment del codice utente all'interno del cluster.

== Executor Services

Gli Executor Services di Hazelcast offrono un framework per l'esecuzione di task in modo distribuito all'interno del cluster, estendendo il modello di concorrenza di Java con funzionalità distribuite.

=== Concetti fondamentali

Hazelcast implementa l'interfaccia `java.util.concurrent.ExecutorService` con implementazioni distribuite che permettono di:

- Eseguire task su un membro specifico del cluster
- Eseguire task su tutti i membri del cluster
- Eseguire task sul membro proprietario di una chiave specifica
- Ottenere risultati futuri (`Future`) di task eseguiti

=== Implementazione

Per utilizzare un Executor Service in Hazelcast, si può procedere come segue:

```java
HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();
IExecutorService executor = hazelcastInstance.getExecutorService("my-executor");

// Eseguire un task su un membro specifico
Member member = ...;
Future<String> future = executor.submitToMember(new MyTask(), member);
String result = future.get();

// Eseguire un task su tutti i membri
Map<Member, Future<String>> results = executor.submitToAllMembers(new MyTask());
```

Hazelcast supporta diversi tipi di executor:

1. *Dedicated Executor Service*: configurato con un thread pool dedicato
2. *Scheduled Executor Service*: per task programmati o periodici
3. *Durable Executor Service*: garantisce che i task vengano eseguiti anche in caso di fallimento del nodo

=== Configurazione

Gli Executor Services possono essere configurati nel file di configurazione Hazelcast:

```xml
<hazelcast>
    <executor-service name="my-executor">
        <pool-size>16</pool-size>
        <queue-capacity>100</queue-capacity>
        <statistics-enabled>true</statistics-enabled>
    </executor-service>
</hazelcast>
```

Parametri principali:
- `pool-size`: numero di thread nel pool
- `queue-capacity`: capacità della coda dei task in attesa
- `statistics-enabled`: abilitare/disabilitare le statistiche

== User Code Namespaces

Gli User Code Namespaces in Hazelcast forniscono un meccanismo per organizzare e isolare il codice client che viene eseguito sui nodi del cluster, disponibile nella versione Hazelcast Enterprise.

=== Concetto e Scopo

Gli User Code Namespaces rappresentano un meccanismo fondamentale in Hazelcast per gestire e isolare il codice eseguibile distribuito all'interno del cluster. Questo sistema risolve diverse sfide critiche nel computing distribuito:

- *Isolamento del codice*: separazione del codice di diverse applicazioni o moduli
- *Gestione delle dipendenze*: risoluzione dei conflitti tra diverse versioni di librerie
- *Deployment flessibile*: aggiornamento di parti del codice senza riavviare l'intero cluster
- *Sicurezza*: controllo granulare su quali classi possono essere eseguite

=== Architettura

Hazelcast implementa gli User Code Namespaces attraverso un sistema gerarchico di ClassLoader che consente di:

1. *Caricare dinamicamente* il codice utente nei nodi del cluster
2. *Isolare i namespace* per evitare conflitti tra applicazioni diverse
3. *Versioning del codice* per supportare aggiornamenti graduali

#figure(
  box(
    radius: 2pt,
    stroke: black.transparentize(90%) + 1pt,
    inset: 1.5em,
    fill: tiling(size: (16pt, 16pt), relative: "parent", place(dx: 5pt, dy: 5pt, rotate(45deg, square(
      size: 2pt,
      fill: black.transparentize(90%),
    )))),
    cetz.canvas({
      import cetz.tree
      import cetz.draw

      draw.set-style(content: (
        padding: .3,
        frame: "rect",
        fill: white,
        stroke: (paint: gradient.radial(center: (0%, 0%), radius: 150%, ..color.map.inferno)),
      ))
      tree.tree(
        (
          [Root],
          ([Hazelcast Core CL], [Namespace A CL]),
          ([Application Base CL], [Namespace B CL], [Namespace C CL]),
        ),
        grow: 2,
        spread: 4,
      )
    }),
  ),
  caption: [Gerarchia dei ClassLoader in Hazelcast User Code Namespaces],
)

=== Caratteristiche Avanzate

1. *Versioning del Codice:* Gli User Code Namespaces supportano il versioning del codice, permettendo di:
  - Mantenere multiple versioni dello stesso namespace attive contemporaneamente
  - Effettuare rolling upgrades del codice senza interruzioni di servizio
  - Specificare quale versione del codice utilizzare per specifici task

2. *Hot Reloading:* Hazelcast può ricaricare dinamicamente i namespace quando vengono rilevate modifiche nei jar o nelle classi associate.

3. *Sicurezza e Permessi:* I namespace possono avere permessi granulari per controllare cosa il codice può fare.

== Casi d'Uso

1. *Micro-Servizi Distribuiti:* I namespace permettono di implementare un'architettura a micro-servizi all'interno del cluster Hazelcast, isolando il codice di diversi servizi e gestendo le dipendenze in modo indipendente.

2. *Regole di Business Dinamiche:* Aggiornamento dinamico delle regole di business senza riavviare il cluster.

*Elaborazione Distribuita di Dati:* Combinando Executor Services e User Code Namespaces per elaborare grandi quantità di dati in parallelo.

== Vantaggi del Computing Distribuito in Hazelcast

L'utilizzo di Executor Services e User Code Namespaces in Hazelcast offre numerosi vantaggi:

1. *Scalabilità*: la possibilità di distribuire i calcoli su più nodi permette di gestire carichi di lavoro crescenti
2. *Località dei dati*: l'esecuzione di codice vicino ai dati (data locality) riduce il trasferimento di dati sulla rete
3. *Resilienza*: il sistema continua a funzionare anche in caso di guasto di alcuni nodi
4. *Gestione del carico*: distribuzione automatica del carico tra i nodi disponibili
5. *Isolamento del codice*: gli User Code Namespaces permettono di isolare il codice utente da quello di sistema
6. *Aggiornamenti senza downtime*: possibilità di aggiornare il codice in esecuzione senza fermare il cluster
7. *Controllo granulare*: permessi e restrizioni specifiche per diversi tipi di codice

== Commenti

L'integrazione di Executor Services e User Code Namespaces consente di sfruttare appieno le potenzialità di Hazelcast come piattaforma di elaborazione distribuita. Queste funzionalità offrono un framework potente e flessibile per costruire applicazioni distribuite scalabili, resilient e manutenibili.

Il distributed computing in Hazelcast permette di avvicinare l'elaborazione ai dati, riducendo la latenza e aumentando l'efficienza del sistema complessivo. Gli User Code Namespaces aggiungono un livello di gestione del codice che rende possibile la costruzione di sistemi modulari ed evolutivi che possono essere aggiornati e modificati senza interruzioni di servizio.

Combinando queste tecnologie, è possibile costruire soluzioni distribuite che rispondono efficacemente alle sfide computazionali moderne, sfruttando al massimo le risorse hardware disponibili nel cluster.
