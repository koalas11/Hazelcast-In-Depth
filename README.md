# Progetto Hazelcast In-Depth

Un'esplorazione completa di Hazelcast, una piattaforma di elaborazione distribuita in-memory.

## Autori
- Sanvito Marco (886493)
- Pellegrini Damiano (886261)

## Struttura del Progetto

- **report/** - Report latex del progetto
- **tests/** - Codice per i test effettuati
  - **java/**
    - **app/** - Directory contenente una applicazione per verificare le funzionalità di hazelcast in modalità embedded
      - **src/** - Directory contenente il codice sorgente java
      - **reports/** - Directory contenente i risultati dei test
    - **gradle files**
  - **python/**
    - **docker-compose.yml** - File Compose per la configurazione del cluster Hazelcast
    - **requirements.txt** - Dipendenze Python
    - **src/** - Directory contenente i file per verificare le prestazioni di hazelcast in modalità client - server
    - **reports/** - Directory contenente i risultati dei test
- **.gitattributes** - File Git attributes
- **.gitignore** - File Git ignore
- **.gitmodules** - File Git modules
- **README.md** - Documentazione del progetto
- **run.bat** - Bat file per runnare tutti i test (Windows)
- **run.sh** - Sh file per runnare tutti i test (Linux)


## Getting Started

### Prerequisiti

- Docker 
- Python 3.x
- Java JDK 17+

### Esecuzione

Per eseguire i test, avviare __run.bat__ o __run.sh__ da console. È possibile specificare come argomento __java__, __python__ . Di default, vengono eseguiti tutti i test.

## Report

Per generare il report, è necessario:

1. Scaricare l'estensione TinyMist per VS Code
2. Compilare il file main.typ utilizzando l'estensione

## Licenza

Questo progetto fa parte del corso di "Architettura Dati" presso l'Università degli Studi di Milano Bicocca.