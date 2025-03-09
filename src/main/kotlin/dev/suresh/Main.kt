package dev.suresh

import java.security.KeyStore
import java.security.cert.X509Certificate
import nl.altindag.ssl.util.CertificateUtils

fun main(args: Array<String>) {
  System.setProperty("slf4j.internal.verbosity", "WARN")
  println("TrustStore Scan: ${BuildConfig.version}")

  // /var/lib/certs/*.jks
  // -Djavax.net.ssl.trustStore=repleo.jks -Djavax.net.ssl.trustStorePassword=changeit

  val path = args.firstOrNull() ?: "/etc/ssl/certs"
  // KeyStoreUtils.getCertificates()
  CertificateUtils.getSystemTrustedCertificates().forEach {
    println(
        """
        |Subject: ${it.subjectX500Principal}
        |Serial Number: ${it.serialNumber}
        |--------------------------------
        """
            .trimMargin())
  }
  // TrustManagerUtils.getTrustManager<>()
  // println("Supported Truststores")
  // TrustStore.allTrustStores().forEach { println(it) }
}

val KeyStore.certEntries
  get() =
      aliases()
          .toList()
          .filter { isCertificateEntry(it) }
          .mapNotNull { getCertificate(it) }
          .filterIsInstance<X509Certificate>()

val KeyStore.certChainEntries
  get() =
      aliases()
          .toList()
          .filter { isCertificateEntry(it) }
          .flatMap { getCertificateChain(it)?.toList().orEmpty() }
          .filterIsInstance<X509Certificate>()

fun KeyStore.keyEntries(password: CharArray) =
    aliases().toList().filter { isKeyEntry(it) }.mapNotNull { getKey(it, password) }
