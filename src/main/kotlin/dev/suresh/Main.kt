package dev.suresh

import BuildConfig
import java.security.Security
import nl.altindag.ssl.util.CertificateUtils

fun main(args: Array<String>) {
  System.setProperty("slf4j.internal.verbosity", "WARN")
  // https://github.com/openjdk/jdk/blob/master/src/java.base/share/conf/security/java.security#L303-L311
  Security.setProperty("keystore.type.compat", "true")
  println("TrustStore Scan: ${BuildConfig.version}")

  // /var/lib/certs/*.jks
  // -Djavax.net.ssl.trustStore=repleo.jks -Djavax.net.ssl.trustStorePassword=changeit

  val filter = args.firstOrNull()
  // KeyStoreUtils.loadSystemKeyStores()
  // KeyManagerUtils.createKeyManager()
  // TrustManagerUtils.createTrustManager()

  println("####### TrustStore Providers ######")
  TrustStore.providers().forEach { println(it) }
  println("####### System TrustStore ######")
  CertificateUtils.getSystemTrustedCertificates()
      ?.filter {
        filter == null || it.subjectX500Principal.toString().contains(filter, ignoreCase = true)
      }
      ?.forEach { println(it.subjectX500Principal) }

  println("####### Java Home ######")
  println(Jdk.javaHome())
  println("####### Java TrustStore ######")
  Jdk.jdkCaCert
      ?.toKeyStore()
      ?.certEntries
      ?.filter {
        filter == null || it.subjectX500Principal.toString().contains(filter, ignoreCase = true)
      }
      ?.onEach { println(it.subjectX500Principal) }

  // ProcessHandle.allProcesses().forEach { println(it.info().commandLine()) }
}
