// Workaround for the lack of an `std` scope.
#let _std-bibliography = bibliography

#let _enStrings = (
  university: "University of Milan - Bicocca",
  helper: "With the help of",
  reportOf: "Report of",
  academic-year: "Academic year",
  chapter: "Chapter",
  appendix: "Appendix",
  introduction: "Introduction",
  abstract: "Abstract",
)

#let _itStrings = (
  university: "Università degli Studi di Milano - Bicocca",
  helper: "Con l'aiuto di",
  reportOf: "Relazione di",
  academic-year: "Anno accademico",
  chapter: "Capitolo",
  appendix: "Appendice",
  introduction: "Introduzione",
  abstract: "Sommario",
)

#let unimib(
  title: [A long enough thesis title for it to wrap on a newline and show how the title wraps],
  authors: (),
  helpers: (),
  keywords: (),
  // University area of education
  area: [School of Sciences],
  // University department
  department: [Department of Informatics, Systems and Communications],
  // University course
  course: [Degree course in Computer Science],
  // The latter academic year in which you are writing this thesis for
  scholar-year: datetime.today().year(),
  abstract: none,
  bibliography: none,
  paper-size: "us-letter",
  single-page: true,
  // Sets the language, could be either "it", "en" or none, in that case it defaults to "en"
  lang: "en",
  flipped: false,
  dark: true,
  body,
) = {
  set text(lang: lang)
  let langStrings = if lang == "en" {
    _enStrings
  } else if lang == "it" {
    _itStrings
  }

  let academic-year = [#langStrings.academic-year #(scholar-year - 1)-#scholar-year]

  // Set the document's basic properties.
  set document(title: title, author: authors, date: datetime.today(), keywords: keywords)
  set page(
    paper: paper-size,
    flipped: flipped,
    fill: if dark { black } else { white },
    // The margins depend on the paper size.
    // margin: if paper-size != "a4" {
    //   (
    //     top: (3cm / 279mm) * 100%,
    //     inside: (if single-page {2.5cm} else {3.5cm} / 216mm) * 100%,
    //     outside: (2.5cm / 216mm) * 100%,
    //     bottom: (2.5cm / 279mm) * 100%,
    //   )
    // } else {
    //   (
    //     top: 3cm,
    //     inside: if single-page {2.5cm} else {3.5cm},
    //     outside: 2.5cm,
    //     bottom: 2.5cm,
    //   )
    // },
    margin: if paper-size != "a4" {
      (
        top: (1.5in / 279mm) * 100%,
        inside: (if single-page { 1in } else { 1.75in } / 216mm) * 100%,
        outside: (1in / 216mm) * 100%,
        bottom: (1in / 279mm) * 100%,
      )
    } else {
      (
        top: 1.5in,
        inside: if single-page { 1in } else { 1.75in },
        outside: 1in,
        bottom: 1in,
      )
    },
  )
  set text(font: "New Computer Modern", fill: if dark { white } else { black })

  set outline(depth: 3, indent: 1em)

  // Style paragraph
  set par(spacing: 1.25em, leading: 0.58em, justify: false)

  // Style headings
  set heading(numbering: "1.1.1.")
  // Set run-in subheadings, starting at level 3.
  show heading: it => {
    if it.level > 4 {
      parbreak()
      text(11pt, style: "italic", weight: "regular", it.body + ".")
    } else {
      it
    }
  }
  // Configure appearance of first level headings
  show heading.where(level: 1): it => {
    let isIntro = it.body == [#langStrings.introduction]

    if it.numbering == none and not isIntro {
      return it
    }

    pagebreak(weak: true)
    block(breakable: false, {
      v(3em)
      if isIntro {
        text(
          size: 1.5em,
          it.body,
        )
        v(1.25em)
      } else {
        let firstNumbering = it.numbering.first()

        let supplement = if firstNumbering == "1" { langStrings.chapter } else if firstNumbering == "A" {
          langStrings.appendix
        } else {
          return it
        }

        set heading(supplement: supplement, numbering: it.numbering.first())
        text(size: 1.5em)[#supplement #context counter(heading).display()]
        v(.25em)
        it.body
      }
      v(.5em)
    })
  }

  // Style lists
  set enum(indent: 1em, body-indent: 0.9em)
  set list(indent: 1em, body-indent: 0.9em)

  // Style bibliography
  show _std-bibliography: set text(8pt)
  set _std-bibliography(style: "ieee")
  set cite(form: "prose")

  // Style math equation
  set math.equation(numbering: "(1)")
  show math.equation: set block(spacing: 1em)

  // Configure appearance of references
  show ref: it => {
    if it.element != none and it.element.func() == math.equation {
      // Override equation references.
      link(it.element.location(), numbering(it.element.numbering, ..counter(math.equation).at(it.element.location())))
    } else if it.element != none and it.element.func() == heading and it.element.level == 1 {
      // Override heading aka section references.
      link(it.element.location(), [
        #langStrings.chapter #numbering("1", ..counter(heading).at(it.element.location()))
      ])
    } else {
      // Other references as usual.
      it
    }
  }

  // Content start

  grid(
    columns: (auto, 1fr),
    column-gutter: 0.4em,
    image("bicocca-logo.png", height: 6em),
    stack(
      spacing: 1em,
      smallcaps(text(weight: "thin", langStrings.university)),
      strong(area),
      strong(department),
      strong(course),
    ),
  )
  v(4fr)
  align(center, text(2em, weight: "medium", title))
  v(5fr)

  [
    *#langStrings.reportOf:* \
    #for author in authors {
      [#author \ ]
    }
  ]


  if helpers.len() > 0 {
    v(1.5fr)
    align(right)[
      *#langStrings.helper:* \
      #for helper in helpers {
        [#helper \ ]
      }
    ]
  }

  v(2fr, weak: true)

  align(center)[#academic-year]
  pagebreak()

  // Set non content pages to roman numerals
  set page(numbering: "i")
  counter(page).update(1)

  if abstract != none {
    set text(size: 11pt)
    set par(justify: true)
    smallcaps(align(center, text(weight: "extralight", size: 1.2em, [#langStrings.abstract])))
    align(center, abstract)
    pagebreak()
  }

  set page(columns: if flipped { 2 } else { 1 }) // Here because initial page should be on 1 column
  outline()
  pagebreak()

  // Reset counter for normal pages
  set page(numbering: "1")
  counter(page).update(1)

  set page(
    header: text(style: "italic", grid(
      columns: (1fr, auto),
      align: top,
      [#title -- #context counter(page).display()],
      context authors.at(calc.rem(counter(page).at(here()).first(), authors.len())),
    )),
    footer: text(style: "italic", [
      #langStrings.university
      #h(1fr)
      #academic-year
    ]),
  )

  body

  if bibliography != none {
    pagebreak()
    bibliography
  }
}
