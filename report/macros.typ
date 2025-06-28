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

#let report_folder = "../tests/python/reports"

#let map_get_time_s = (:)
#let map_put_time_s = (:)
#let map_put_all_time_s = (:)
#let map_get_all_time_s = (:)

#let big_failover_time_s = (:)
#let big_rejoin_time_s = (:)
#let small_failover_time_s = (:)
#let small_rejoin_time_s = (:)

#let ram_usage = (:)
#let cpu_usage = (:)

#{
  let folders = (
    "single_node_tests",
    "two_nodes_tests",
    "three_nodes_tests",
    "four_nodes_tests",
    "five_nodes_tests",
  )

  for folder in folders {
    map_get_time_s.insert(folder, (:))
    map_put_time_s.insert(folder, (:))
    map_put_all_time_s.insert(folder, (:))
    map_get_all_time_s.insert(folder, (:))
    big_failover_time_s.insert(folder, (:))
    big_rejoin_time_s.insert(folder, (:))
    small_failover_time_s.insert(folder, (:))
    small_rejoin_time_s.insert(folder, (:))
    ram_usage.insert(folder, (:))
    cpu_usage.insert(folder, (:))

    let map_metrics_csv = report_folder + "/" + folder + "/map_test_metrics.csv"

    let map_metrics = csv(map_metrics_csv, row-type: dictionary)

    let index = 0

    for (metric_name, value) in map_metrics {
      let split_name = metric_name.split("_")
      let size = split_name.at(split_name.len() - 2)

      let value_float = float(value)

      if index == 0 {
        map_put_time_s.at(folder).insert(size, value_float)
      } else if index == 1 {
        map_get_time_s.at(folder).insert(size, value_float)
      } else if index == 2 {
        map_put_all_time_s.at(folder).insert(size, value_float)
      } else if index == 3 {
        map_get_all_time_s.at(folder).insert(size, value_float)
      }

      index += 1
      if index > 3 {
        index = 0
      }
    }

    if folder == "single_node_tests" {
      continue
    }

    let failover_rejoin_csv = report_folder + "/" + folder + "/failover_test_metrics.csv"
    let failover_rejoin_data = csv(failover_rejoin_csv, row-type: dictionary)

    for (metric_name, value) in failover_rejoin_data {
      let data_type = metric_name.split("_").first()
      let size = metric_name.split("_").last()
      let type = metric_name.split("_").at(3)

      if (data_type == "big") {
        if (type == "rejoin") {
          big_rejoin_time_s.at(folder).insert(size, float(value))
        } else {
          big_failover_time_s.at(folder).insert(size, float(value))
        }
      } else if (data_type == "small") {
        if (type == "rejoin") {
          small_rejoin_time_s.at(folder).insert(size, float(value))
        } else {
          small_failover_time_s.at(folder).insert(size, float(value))
        }
      }
    }
  }

  let ram_cpu_usage_1_node = (
    node1: (
      ("cpu_usage_0_300s", 1.2253706276944505),
      ("mem_usage_0_300s", 355416858.122449),
      ("cpu_usage_1000_300s", 0.8017536226033788),
      ("mem_usage_1000_300s", 298315284.48),
      ("cpu_usage_10000_300s", 0.9337827206217075),
      ("mem_usage_10000_300s", 297893314.56),
      ("cpu_usage_100000_300s", 0.9225761406731777),
      ("mem_usage_100000_300s", 388530503.68),
    ),
  )

  let ram_cpu_usage_2_nodes = (
    node1: (
      ("cpu_usage_0_300s", 1.8511559812622598),
      ("mem_usage_0_300s", 450587486.31578946),
      ("cpu_usage_1000_300s", 1.0455799141595525),
      ("mem_usage_1000_300s", 453615939.3684211),
      ("cpu_usage_10000_300s", 0.9246862314523149),
      ("mem_usage_10000_300s", 465344943.15789473),
      ("cpu_usage_100000_300s", 0.8660591461128713),
      ("mem_usage_100000_300s", 676032188.6315789),
    ),
    node2: (
      ("cpu_usage_0_300s", 0.9343500356119798),
      ("mem_usage_0_300s", 610064384.0),
      ("cpu_usage_1000_300s", 0.974124381001111),
      ("mem_usage_1000_300s", 584461150.3157895),
      ("cpu_usage_10000_300s", 0.8544637290458394),
      ("mem_usage_10000_300s", 566110315.7894737),
      ("cpu_usage_100000_300s", 1.0263037342438646),
      ("mem_usage_100000_300s", 596933685.8947369),
    ),
  )

  let ram_cpu_usage_nodes = (
    node1: (
      ("cpu_usage_0_300s", 0.7697420135984188),
      ("mem_usage_0_300s", 471144994.1333333),
      ("cpu_usage_1000_300s", 1.1085467904769724),
      ("mem_usage_1000_300s", 512581222.4),
      ("cpu_usage_10000_300s", 0.9208157008322337),
      ("mem_usage_10000_300s", 550651357.8666667),
      ("cpu_usage_100000_300s", 0.7820633252814534),
      ("mem_usage_100000_300s", 581359889.0666667),
    ),
    node2: (
      ("cpu_usage_0_300s", 1.7443434120355934),
      ("mem_usage_0_300s", 463516467.2),
      ("cpu_usage_1000_300s", 0.7561048053863701),
      ("mem_usage_1000_300s", 492996881.06666666),
      ("cpu_usage_10000_300s", 0.874942185016564),
      ("mem_usage_10000_300s", 502616473.6),
      ("cpu_usage_100000_300s", 1.306414901227902),
      ("mem_usage_100000_300s", 530130261.3333333),
    ),
    node3: (
      ("cpu_usage_0_300s", 1.2740295991655546),
      ("mem_usage_0_300s", 361252864.0),
      ("cpu_usage_1000_300s", 0.8885969768985237),
      ("mem_usage_1000_300s", 375617536.0),
      ("cpu_usage_10000_300s", 1.6037675492327088),
      ("mem_usage_10000_300s", 399075328.0),
      ("cpu_usage_100000_300s", 0.8576963728961757),
      ("mem_usage_100000_300s", 465859652.26666665),
    ),
  )

  let ram_cpu_usage_nodes = (
    node1: (
      ("cpu_usage_0_300s", 1.0796349510612364),
      ("mem_usage_0_300s", 446458265.6),
      ("cpu_usage_1000_300s", 0.8934218679332827),
      ("mem_usage_1000_300s", 499894517.76),
      ("cpu_usage_10000_300s", 0.862913833392802),
      ("mem_usage_10000_300s", 483747758.08),
      ("cpu_usage_100000_300s", 0.8546367692608023),
      ("mem_usage_100000_300s", 501004697.6),
    ),
    node2: (
      ("cpu_usage_0_300s", 1.5188976354548596),
      ("mem_usage_0_300s", 344396922.88),
      ("cpu_usage_1000_300s", 1.120182391527788),
      ("mem_usage_1000_300s", 351416156.16),
      ("cpu_usage_10000_300s", 1.2329221597896123),
      ("mem_usage_10000_300s", 353877524.48),
      ("cpu_usage_100000_300s", 0.9532210703372637),
      ("mem_usage_100000_300s", 457783214.08),
    ),
    node3: (
      ("cpu_usage_0_300s", 1.1761633714152513),
      ("mem_usage_0_300s", 401951129.6),
      ("cpu_usage_1000_300s", 1.167567277848313),
      ("mem_usage_1000_300s", 403466649.6),
      ("cpu_usage_10000_300s", 1.5411343451233899),
      ("mem_usage_10000_300s", 394116792.32),
      ("cpu_usage_100000_300s", 1.0673277483274766),
      ("mem_usage_100000_300s", 420617912.32),
    ),
    node4: (
      ("cpu_usage_0_300s", 1.2167009592153846),
      ("mem_usage_0_300s", 516678942.72),
      ("cpu_usage_1000_300s", 0.9637048129307207),
      ("mem_usage_1000_300s", 517549260.8),
      ("cpu_usage_10000_300s", 0.9309251201685308),
      ("mem_usage_10000_300s", 489231646.72),
      ("cpu_usage_100000_300s", 0.8343783101528124),
      ("mem_usage_100000_300s", 504752046.08),
    ),
  )

  let ram_cpu_usage_nodes = (
    node1: (
      ("cpu_usage_0_300s", 1.0914348122922162),
      ("mem_usage_0_300s", 491534894.54545456),
      ("cpu_usage_1000_300s", 0.9382342953596641),
      ("mem_usage_1000_300s", 493617152.0),
      ("cpu_usage_10000_300s", 0.9256969265094572),
      ("mem_usage_10000_300s", 496246225.45454544),
      ("cpu_usage_100000_300s", 0.8457178507467176),
      ("mem_usage_100000_300s", 513953605.8181818),
    ),
    node2: (
      ("cpu_usage_0_300s", 1.7394130155481842),
      ("mem_usage_0_300s", 340889786.1818182),
      ("cpu_usage_1000_300s", 1.9416099858968061),
      ("mem_usage_1000_300s", 411886871.27272725),
      ("cpu_usage_10000_300s", 0.9782400830885298),
      ("mem_usage_10000_300s", 440995281.45454544),
      ("cpu_usage_100000_300s", 1.406463961821502),
      ("mem_usage_100000_300s", 524505646.54545456),
    ),
    node3: (
      ("cpu_usage_0_300s", 1.7026977729931054),
      ("mem_usage_0_300s", 380836584.72727275),
      ("cpu_usage_1000_300s", 1.0111913224662756),
      ("mem_usage_1000_300s", 382996666.1818182),
      ("cpu_usage_10000_300s", 0.9095658775421872),
      ("mem_usage_10000_300s", 384191022.54545456),
      ("cpu_usage_100000_300s", 0.9563167984703075),
      ("mem_usage_100000_300s", 428744890.1818182),
    ),
    node4: (
      ("cpu_usage_0_300s", 1.2949410627288047),
      ("mem_usage_0_300s", 364546978.90909094),
      ("cpu_usage_1000_300s", 0.9089368545881024),
      ("mem_usage_1000_300s", 383962205.09090906),
      ("cpu_usage_10000_300s", 0.9574005156409242),
      ("mem_usage_10000_300s", 389234129.45454544),
      ("cpu_usage_100000_300s", 0.9172925378712932),
      ("mem_usage_100000_300s", 419971072.0),
    ),
    node5: (
      ("cpu_usage_0_300s", 1.134231889595602),
      ("mem_usage_0_300s", 344558685.09090906),
      ("cpu_usage_1000_300s", 0.9890553782954004),
      ("mem_usage_1000_300s", 386223383.27272725),
      ("cpu_usage_10000_300s", 0.8337673881571974),
      ("mem_usage_10000_300s", 395306263.27272725),
      ("cpu_usage_100000_300s", 0.8656306770699836),
      ("mem_usage_100000_300s", 403702318.54545456),
    ),
  )
}


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

#let plot1a = {
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
      y-label: [Tempo di esecuzione \[log_10(s)\]],
      y-max: 3,
      y-min: -4,
      y-tick-step: 1,
      y-format: val => $10^#val$,
      x-min: -0.5,
      x-max: 5.6,
      x-tick-step: none,
      x-ticks: map_get_time_s.at("single_node_tests").keys().enumerate().map(((idx, name)) => (idx, name)),
      axis-style: "left",
      x-grid: true,
      y-grid: true,
      {
        let nodes_str = ""
        let index = 0
        for nodes in (
          "single_node_tests",
          "two_nodes_tests",
          "three_nodes_tests",
        ) {
          if index == 0 {
            nodes_str = " (1)"
          } else if index == 1 {
            nodes_str = " (2)"
          } else if index == 2 {
            nodes_str = " (3)"
          }

          let data = map_get_time_s.at(nodes).values().enumerate().map(((idx, value)) => (idx, calc.log(value * 1e-9)))
          plot.add(data, mark: "o", mark-size: .125, label: "get" + nodes_str, line: "spline")

          data = map_put_time_s.at(nodes).values().enumerate().map(((idx, value)) => (idx, calc.log(value * 1e-9)))
          plot.add(data, mark: "o", mark-size: .125, label: "put" + nodes_str, line: "spline")

          data = map_put_all_time_s.at(nodes).values().enumerate().map(((idx, value)) => (idx, calc.log(value * 1e-9)))
          plot.add(data, mark: "o", mark-size: .125, label: "put all" + nodes_str, line: "spline")

          data = map_get_all_time_s.at(nodes).values().enumerate().map(((idx, value)) => (idx, calc.log(value * 1e-9)))
          plot.add(data, mark: "o", mark-size: .125, label: "get all" + nodes_str, line: "spline")
          index += 1
        }
      },
    )
  })
}

#let plot1b = {
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
      y-label: [Tempo di esecuzione \[log_10(s)\]],
      y-max: 3,
      y-min: -4,
      y-tick-step: 1,
      y-format: val => $10^#val$,
      x-min: -0.5,
      x-max: 5.6,
      x-tick-step: none,
      x-ticks: map_get_time_s.at("four_nodes_tests").keys().enumerate().map(((idx, name)) => (idx, name)),
      axis-style: "left",
      x-grid: true,
      y-grid: true,
      {
        let nodes_str = ""
        let index = 0
        for nodes in (
          "four_nodes_tests",
          "five_nodes_tests",
        ) {
          if index == 0 {
            nodes_str = " (4)"
          } else if index == 1 {
            nodes_str = " (5)"
          }

          let data = map_get_time_s.at(nodes).values().enumerate().map(((idx, value)) => (idx, calc.log(value * 1e-9)))
          plot.add(data, mark: "o", mark-size: .125, label: "get" + nodes_str, line: "spline")

          data = map_put_time_s.at(nodes).values().enumerate().map(((idx, value)) => (idx, calc.log(value * 1e-9)))
          plot.add(data, mark: "o", mark-size: .125, label: "put" + nodes_str, line: "spline")

          data = map_put_all_time_s.at(nodes).values().enumerate().map(((idx, value)) => (idx, calc.log(value * 1e-9)))
          plot.add(data, mark: "o", mark-size: .125, label: "put all" + nodes_str, line: "spline")

          data = map_get_all_time_s.at(nodes).values().enumerate().map(((idx, value)) => (idx, calc.log(value * 1e-9)))
          plot.add(data, mark: "o", mark-size: .125, label: "get all" + nodes_str, line: "spline")
          index += 1
        }
      },
    )
  })
}

#let plot2a = {
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
      y-label: [Tempo di esecuzione \[log_10(s)\]],
      y-max: 2,
      y-min: -2,
      y-tick-step: 1,
      y-format: val => $10^#val$,
      x-min: -0.5,
      x-max: 4.5,
      x-tick-step: none,
      x-ticks: big_failover_time_s.at("two_nodes_tests").keys().enumerate().map(((idx, name)) => (idx, name)),
      axis-style: "left",
      x-grid: true,
      y-grid: true,
      {
        let nodes_str = ""
        let index = 0
        for nodes in (
          "two_nodes_tests",
          "three_nodes_tests",
          "four_nodes_tests",
          "five_nodes_tests",
        ) {
          if index == 0 {
            nodes_str = " (2)"
          } else if index == 1 {
            nodes_str = " (3)"
          } else if index == 2 {
            nodes_str = " (4)"
          } else if index == 3 {
            nodes_str = " (5)"
          }

          let data = big_failover_time_s
            .at(nodes)
            .values()
            .enumerate()
            .map(((idx, value)) => (idx, calc.log(value * 1e-9)))
          plot.add(data, mark: "o", mark-size: .125, label: "Failover" + nodes_str, line: "spline")

          let data = big_rejoin_time_s
            .at(nodes)
            .values()
            .enumerate()
            .map(((idx, value)) => (idx, calc.log(value * 1e-9)))
          plot.add(data, mark: "o", mark-size: .125, label: "Rejoin" + nodes_str, line: "spline")
          index += 1
        }
      },
    )
  })
}

#let plot2b = {
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
      y-label: [Tempo di esecuzione \[log_10(s)\]],
      y-max: 2,
      y-min: -2,
      y-tick-step: 1,
      y-format: val => $10^#val$,
      x-min: -0.5,
      x-max: 4.5,
      x-tick-step: none,
      x-ticks: small_failover_time_s.at("two_nodes_tests").keys().enumerate().map(((idx, name)) => (idx, name)),
      axis-style: "left",
      x-grid: true,
      y-grid: true,
      {
        let nodes_str = ""
        let index = 0
        for nodes in (
          "two_nodes_tests",
          "three_nodes_tests",
          "four_nodes_tests",
          "five_nodes_tests",
        ) {
          if index == 0 {
            nodes_str = " (2)"
          } else if index == 1 {
            nodes_str = " (3)"
          } else if index == 2 {
            nodes_str = " (4)"
          } else if index == 3 {
            nodes_str = " (5)"
          }

          let data = small_failover_time_s
            .at(nodes)
            .values()
            .enumerate()
            .map(((idx, value)) => (idx, calc.log(value * 1e-9)))
          plot.add(data, mark: "o", mark-size: .125, label: "Failover" + nodes_str, line: "spline")

          let data = small_rejoin_time_s
            .at(nodes)
            .values()
            .enumerate()
            .map(((idx, value)) => (idx, calc.log(value * 1e-9)))
          plot.add(data, mark: "o", mark-size: .125, label: "Rejoin" + nodes_str, line: "spline")
          index += 1
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
