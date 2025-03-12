package dev.suresh

import com.github.ajalt.clikt.core.main
import dev.suresh.cmd.Scan

fun main(args: Array<String>) {
  System.setProperty("slf4j.internal.verbosity", "WARN")
  Scan().main(args)
}
