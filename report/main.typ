#import "unimib-template.typ": unimib

#show: unimib.with(
  title: "Progetto Archittettura Dati: Hazelcast",
  area: [Scuola di Scienza],
  department: [Dipartimento di Informatica, Sistemi e Comunicazione],
  course: [Corso di Scienze Informatiche],
  authors: (
    "Pellegrini Damiano 886261",
    "Sanvito Marco 886493",
  ),
  bibliography: bibliography(style: "ieee", "citations.bib"),
  abstract: include "chapters/abstract.typ",
  dark: false,
  lang: "it",
  // flipped: true
)

#set cite(form: "prose")

#set heading(numbering: none)

#include "chapters/introduction.typ"

#set heading(numbering: "1.1.")

#include "chapters/1.architecture.typ"
#include "chapters/2.data.structures.typ"
#include "chapters/3.distributed.computing.typ"
#include "chapters/4.data.ingestion.typ"
#include "chapters/5.sql.typ"
#include "chapters/6.advanced.features.typ"
#include "chapters/7.comments.typ"

#set heading(numbering: none)
