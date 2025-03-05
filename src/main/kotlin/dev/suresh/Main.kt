package dev.suresh

import java.security.cert.X509Certificate
import kotlin.io.path.Path

fun main(args: Array<String>) {
  println("Hello World! ${BuildConfig.version}")

  println("Supported Truststores")
  TrustStore.allTrustStores().forEach { println(it) }

  // /var/lib/certs/*.jks
  // -Djavax.net.ssl.trustStore=repleo.jks -Djavax.net.ssl.trustStorePassword=changeit
  // CentOS/RHEL ->  /etc/pki/tls/certs
  // Ubuntu -> /etc/ssl/certs (/usr/local/share/ca-certificates/)

  println("All CA from /etc/ssl/certs")
  val path = args.firstOrNull() ?: "/etc/ssl/certs"
  val keystore = TrustStore.systemTrustStore(TrustStoreType.Directory(Path(path)))
  val aliases = keystore.aliases()
  while (aliases.hasMoreElements()) {
    val alias = aliases.nextElement()

    if (keystore.isCertificateEntry(alias)) {
      val cert = keystore.getCertificate(alias) as X509Certificate
      println(
          """
                    |Certificate Alias: $alias
                    |Subject: ${cert.subjectX500Principal}
                    |Issuer: ${cert.issuerX500Principal}
                    |Valid From: ${cert.notBefore}
                    |Valid Until: ${cert.notAfter}
                    |Serial Number: ${cert.serialNumber}
                    |--------------------------------
                """
              .trimMargin())
    }
  }
}
