package dev.suresh

import java.security.KeyStore
import java.security.cert.X509Certificate
import kotlin.io.path.Path

fun main(args: Array<String>) {
  println("TrustStore Scan: ${BuildConfig.version}")

  // /var/lib/certs/*.jks
  // -Djavax.net.ssl.trustStore=repleo.jks -Djavax.net.ssl.trustStorePassword=changeit
  // CentOS/RHEL ->  /etc/pki/tls/certs
  // Ubuntu -> /etc/ssl/certs (/usr/local/share/ca-certificates/)

  println("All CA from /etc/ssl/certs")
  val path = args.firstOrNull() ?: "/etc/ssl/certs"
  val keystore = TrustStore.systemTrustStore(TrustStoreType.Directory(Path(path)))
  keystore.certEntries.forEach {
    println(
        """
        |Subject: ${it.subjectX500Principal}
        |Serial Number: ${it.serialNumber}
        |--------------------------------
        """
            .trimMargin())
  }

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

fun KeyStore.keyEntries(password: CharArray) =
    aliases().toList().filter { isKeyEntry(it) }.mapNotNull { getKey(it, password) }
