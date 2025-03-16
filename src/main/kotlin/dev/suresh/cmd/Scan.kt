package dev.suresh.cmd

import BuildConfig
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import dev.suresh.security.Jdk
import dev.suresh.security.certEntries
import dev.suresh.security.findKeyStore
import dev.suresh.security.tryKeyStore
import kotlin.io.path.Path
import nl.altindag.ssl.util.CertificateUtils

class Scan : CliktCommand() {
  val ca: String? by option().help("Filter given RootCAs")
  val password: String? by option().help("Custom TrustStore Password")
  val format: String by option().default("").help("Output Format (JSON)")

  override fun run() {
    println("TrustStore Scan: ${BuildConfig.version}")

    println("####### System TrustStore ######")
    runCatching { CertificateUtils.getSystemTrustedCertificates() }
        .onFailure { println("Warning: Unable to load system certificates: ${it.message}") }
        .getOrElse { emptyList() }
        .filter {
          ca == null || it.subjectX500Principal.toString().contains(ca.orEmpty(), ignoreCase = true)
        }
        .forEachIndexed { idx, cert -> println("$idx : ${cert.subjectX500Principal.name}") }

    println("####### Java TrustStore ######")
    Jdk.jdkCaCert?.let {
      println(it)
      it.tryKeyStore()
          ?.certEntries
          ?.filter { cert ->
            ca == null ||
                cert.subjectX500Principal.toString().contains(ca.orEmpty(), ignoreCase = true)
          }
          ?.forEachIndexed { idx, cert -> println("$idx : ${cert.subjectX500Principal.name}") }
    }

    println("####### Java Process (Custom TrustStore) ######")
    Jdk.javaProcessTrustStores.forEach { trustStore, password ->
      println("Found trustStore $trustStore")
      println("----------------------------")
      Path(trustStore)
          .tryKeyStore(storePasswd = password?.toCharArray())
          ?.certEntries
          ?.filter { cert ->
            ca == null ||
                cert.subjectX500Principal.toString().contains(ca.orEmpty(), ignoreCase = true)
          }
          ?.forEachIndexed { idx, cert -> println("$idx : ${cert.subjectX500Principal.name}") }
    }

    println("####### Custom Paths ######")
    listOf(Path("/secrets/"), Path("/var/lib/certs/"))
        .flatMap { it.findKeyStore(password?.toCharArray()).toList() }
        .forEach { pair ->
          println("Found trustStore ${pair.first}")
          println("----------------------------")
          pair.second
              ?.certEntries
              ?.filter { cert ->
                ca == null ||
                    cert.subjectX500Principal.toString().contains(ca.orEmpty(), ignoreCase = true)
              }
              ?.forEachIndexed { idx, cert -> println("$idx : ${cert.subjectX500Principal.name}") }
        }
  }
}
