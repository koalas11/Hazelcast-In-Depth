#let report_folder = "../tests/python/reports"

#let map_get_time_s = (:)
#let map_put_time_s = (:)
#let map_put_all_time_s = (:)
#let map_get_all_time_s = (:)

#let big_failover_time_s = (:)
#let big_rejoin_time_s = (:)
#let small_failover_time_s = (:)
#let small_rejoin_time_s = (:)

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

#let test_configs = (
  "single_node_tests",
  "two_nodes_tests",
  "three_nodes_tests",
  "four_nodes_tests",
  "five_nodes_tests",
)

// Funzione per caricare dati da una configurazione
#let load_config_data(config) = {
  csv("../tests/python/reports/" + config + "/ram_cpu_test_metrics.csv")
}

// Funzione per estrarre l'ID del nodo dal nome della metrica
#let extract_node_id(metric_name) = {
  let parts = metric_name.split("_")
  if parts.len() > 2 {
    return parts.at(2)
  }
  return ""
}

// Funzione per estrarre la dimensione dal nome della metrica
#let extract_size(metric_name) = {
  let parts = metric_name.split("_")
  return parts.at(-2)
}

// Funzione per estrarre il tipo di metrica
#let extract_metric_type(metric_name) = {
  if metric_name.contains("cpu_usage") {
    return "cpu"
  } else if metric_name.contains("mem_usage") and not metric_name.contains("perc") {
    return "mem_bytes"
  } else if metric_name.contains("network_rx") {
    return "network_rx"
  } else if metric_name.contains("network_tx") {
    return "network_tx"
  }
  return ""
}

// Funzione per processare i dati di una configurazione
#let process_config_data(data) = {
  let nodes = (:)

  for row in data.slice(1) {
    let metric_name = row.at(0)
    let value = row.at(1)

    if metric_name.contains("idle_container") {
      let node_id = extract_node_id(metric_name)
      let size = extract_size(metric_name)
      let metric_type = extract_metric_type(metric_name)

      if node_id != "" and size != "" and metric_type != "" {
        if node_id not in nodes {
          nodes.insert(node_id, (:))
        }
        if size not in nodes.at(node_id) {
          nodes.at(node_id).insert(size, (:))
        }
        nodes.at(node_id).at(size).insert(metric_type, value)
      }
    }
  }

  return nodes
}

// Carica tutti i dati
#let all_configs = (:)
#for config in test_configs {
  let data = load_config_data(config)
  all_configs.insert(config, process_config_data(data))
}

#let usage_test_sizes = ("0", "10000", "100000", "1000000")


#let plot1a = {
  import "packages.typ": cetz, cetz-plot, colorMap

  cetz.canvas({
    import cetz-plot: plot
    import cetz.draw: set-style

    let colors = colorMap
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
  import "packages.typ": cetz, cetz-plot, colorMap

  cetz.canvas({
    import cetz-plot: plot
    import cetz.draw: set-style

    let colors = colorMap.slice(2)
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
  import "packages.typ": cetz, cetz-plot, colorMap

  cetz.canvas({
    import cetz-plot: plot
    import cetz.draw: set-style

    let colors = colorMap.slice(2)
    let palette = cetz.palette.new(colors: colors)

    set-global-style()

    plot.plot(
      size: (12, 6),
      plot-style: palette.with(stroke: true),
      mark-style: palette.with(stroke: true, fill: true),
      legend: "inner-south-east",
      x-label: [Numero elementi],
      y-label: [Tempo \[log_10(s)\]],
      y-max: 1,
      y-min: -4.5,
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
  import "packages.typ": cetz, cetz-plot, colorMap

  cetz.canvas({
    import cetz-plot: plot
    import cetz.draw: set-style

    let colors = colorMap.slice(2)
    let palette = cetz.palette.new(colors: colors)

    set-global-style()

    plot.plot(
      size: (12, 6),
      plot-style: palette.with(stroke: true),
      mark-style: palette.with(stroke: true, fill: true),
      legend: "inner-south-east",
      x-label: [Numero elementi],
      y-label: [Tempi \[log_10(s)\]],
      y-max: 0,
      y-min: -4.5,
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
