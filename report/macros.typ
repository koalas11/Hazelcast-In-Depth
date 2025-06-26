// (op: (size: time_ns))
#let mockData1 = (
  map_put_time_ns: (
    "1": 1,
    "10": 10,
    "100": 100,
    "1000": 1000,
    "10000": 10000,
  ),
  map_get_time_ns: (
    "1": 1 * 10,
    "10": 10 * 10,
    "100": 100 * 10,
    "1000": 1000 * 10,
    "10000": 10000 * 10,
  ),
  map_put_all_time_ns: (
    "1": 1 * 100,
    "10": 10 * 100,
    "100": 100 * 100,
    "1000": 1000 * 100,
    "10000": 10000 * 100,
  ),
  map_get_all_time_ns: (
    "1": 1 * 1000,
    "10": 10 * 1000,
    "100": 100 * 1000,
    "1000": 1000 * 1000,
    "10000": 10000 * 1000,
  ),
)


// (nthread: (size: ops/sec))
#let mockData2 = (
  "2": (
    "1000": 1000 * 2,
    "5000": 5000 * 2,
    "10000": 10000 * 2,
    "20000": 20000 * 2,
    "50000": 50000 * 2,
    "100000": 100000 * 2,
    "200000": 200000 * 2,
    "500000": 500000 * 2,
    "1000000": 1000000 * 2,
  ),
  "4": (
    "1000": 1000 * 4,
    "5000": 5000 * 4,
    "10000": 10000 * 4,
    "20000": 20000 * 4,
    "50000": 50000 * 4,
    "100000": 100000 * 4,
    "200000": 200000 * 4,
    "500000": 500000 * 4,
    "1000000": 1000000 * 4,
  ),
  "8": (
    "1000": 1000 * 8,
    "5000": 5000 * 8,
    "10000": 10000 * 8,
    "20000": 20000 * 8,
    "50000": 50000 * 8,
    "100000": 100000 * 8,
    "200000": 200000 * 8,
    "500000": 500000 * 8,
    "1000000": 1000000 * 8,
  ),
  "16": (
    "1000": 1000 * 16,
    "5000": 5000 * 16,
    "10000": 10000 * 16,
    "20000": 20000 * 16,
    "50000": 50000 * 16,
    "100000": 100000 * 16,
    "200000": 200000 * 16,
    "500000": 500000 * 16,
    "1000000": 1000000 * 16,
  ),
)

// (timing: (size: value))
#let mockData3 = (
  "rejoin_time_ns": (
    "1000": 1000,
    "5000": 5000,
    "10000": 10000,
    "20000": 20000,
    "50000": 50000,
    "100000": 100000,
    "200000": 200000,
    "500000": 500000,
    "1000000": 1000000,
  ),
  "failover_rejoin_time_ns": (
    "1000": 1000 * 10,
    "5000": 5000 * 10,
    "10000": 10000 * 10,
    "20000": 20000 * 10,
    "50000": 50000 * 10,
    "100000": 100000 * 10,
    "200000": 200000 * 10,
    "500000": 500000 * 10,
    "1000000": 1000000 * 10,
  ),
)


#let set-global-style() = {
  import "packages.typ": cetz.draw.set-style
  set-style(
    legend: (fill: white.transparentize(15%)),
    axes: (
      x: (
        tick: (label: (angle: 45deg, offset: 1.25em, anchor: "east")),
        label: (offset: 1.5em),
      ),
      y: (
        label: (angle: 90deg, anchor: "east", offset: 3.5em),
      ),
    ),
  )
}

#let plot1 = {
  import "packages.typ": cetz, cetz-plot
  import cetz-plot: plot

  cetz.canvas({
    import cetz.draw: set-style

    let colors = color.map.inferno.chunks(3).map(it => it.first())
    let palette = cetz.palette.new(colors: colors)

    set-global-style()

    plot.plot(
      size: (12, 6),
      plot-style: palette.with(stroke: true),
      mark-style: palette.with(stroke: true, fill: true),
      legend: "inner-south-east",
      x-label: [Numero elementi],
      y-label: [Tempo di esecuzione \[log_10(ns)\]],
      y-max: 8,
      y-min: -1,
      y-tick-step: 1,
      y-format: val => $10^#val$,
      x-min: -0.5,
      x-max: 4.5,
      x-tick-step: 1,
      x-format: val => $10^#val$,
      axis-style: "left",
      x-grid: true,
      y-grid: true,
      {
        for (operation, timings) in mockData1 {
          let data = timings.pairs().map(i => (calc.log(int(i.at(0))), calc.log(i.at(1))))

          let operationLabel = operation.slice(4, -8).split("_").map(i => upper(i.at(0)) + i.slice(1)).join(" ")

          plot.add(data, mark: "o", mark-size: .125, label: operationLabel, line: "spline")
        }
      },
    )
  })
}

#let plot2 = {
  import "packages.typ": cetz, cetz-plot
  import cetz-plot: plot

  cetz.canvas({
    import cetz.draw: set-style

    let colors = color.map.inferno.chunks(3).map(it => it.first())
    let palette = cetz.palette.new(colors: colors)

    set-global-style()

    plot.plot(
      size: (12, 6),
      plot-style: palette.with(stroke: true),
      mark-style: palette.with(stroke: true, fill: true),
      legend: "inner-south-east",
      x-label: [Numero elementi],
      y-label: [Tempo di esecuzione \[log_10(ns)\]],
      y-max: 7.5,
      y-min: 2.5,
      y-tick-step: 1,
      y-format: val => $10^#val$,
      x-min: 2.5,
      x-max: 6.5,
      x-tick-step: none,
      // x-format: val => $10^#calc.round(val, digits: 2)$,
      x-ticks: mockData2
        .at("2")
        .keys()
        .map(key => calc.round(calc.log(float(key)), digits: 2))
        .map(key => (key, $10^#key$)),
      axis-style: "left",
      x-grid: true,
      y-grid: true,
      {
        for (nThreads, timings) in mockData2 {
          let data = timings.pairs().map(i => (calc.log(int(i.at(0))), calc.log(i.at(1))))

          plot.add(data, mark: "o", mark-size: .125, label: nThreads, line: "spline")
        }
      },
    )
  })
}

#let plot3 = {
  import "packages.typ": cetz, cetz-plot
  import cetz-plot: plot

  cetz.canvas({
    import cetz.draw: set-style

    let colors = color.map.inferno.chunks(6).map(it => it.first())
    let palette = cetz.palette.new(colors: colors)

    set-global-style()

    plot.plot(
      size: (12, 6),
      plot-style: palette.with(stroke: true),
      mark-style: palette.with(stroke: true, fill: true),
      legend: "inner-south-east",
      x-label: [Numero elementi],
      y-label: [Tempo di esecuzione \[log_10(ns)\]],
      y-max: 7.5,
      y-min: 2.5,
      y-tick-step: 1,
      y-format: val => $10^#val$,
      x-min: 2.5,
      x-max: 6.5,
      x-tick-step: none,
      // x-format: val => $10^#calc.round(val, digits: 2)$,
      x-ticks: mockData3
        .failover_rejoin_time_ns
        .keys()
        .map(key => calc.round(calc.log(float(key)), digits: 2))
        .map(key => (key, $10^#key$)),
      axis-style: "left",
      x-grid: true,
      y-grid: true,
      {
        for (metric, timings) in mockData3 {
          let data = timings.pairs().map(i => (calc.log(int(i.at(0))), calc.log(i.at(1))))

          let metricLabel = metric.slice(0, -3).split("_").map(i => upper(i.at(0)) + i.slice(1)).join(" ")

          plot.add(data, mark: "o", mark-size: .125, label: metricLabel, line: "spline")
        }
      },
    )
  })
}
