#import "unimib-template.typ": unimib
#import "packages.typ": codly, codly-languages

#show: unimib.with(
  title: "Progetto Architettura Dati -- Hazelcast",
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


#show: codly.codly-init
#codly.codly(languages: codly-languages.codly-languages, breakable: true)

#set table(stroke: none, gutter: 0.2em, fill: (x, y) => {
  if y == 0 { luma(120) } else {
    if calc.odd(y) {
      luma(240)
    } else {
      luma(220)
    }
  }
})

#show table.cell: it => {
  if it.y == 0 {
    set text(white)
    strong(it)
  } else {
    it
  }
}

#set cite(form: "prose")

#set heading(numbering: none)

#include "chapters/introduction.typ"

#set heading(numbering: "1.1.")

#include "chapters/1.architecture.typ"
#include "chapters/2.data.structures.typ"
#include "chapters/3.distributed.computing.typ"
#include "chapters/4.data.ingestion.typ"
#include "chapters/5.distributed.query.typ"
#include "chapters/6.advanced.features.typ"
#include "chapters/7.comments.typ"
#include "chapters/8.compare.typ"
#include "chapters/9.tests.typ"
#include "chapters/10.conclusions.typ"

#set heading(numbering: none)
