package dev.suresh

import java.io.File
import java.net.URI
import java.security.KeyStore
import java.security.cert.CertStore
import java.security.cert.CertificateFactory
import java.security.cert.X509CRL
import java.security.cert.X509CRLSelector
import java.security.cert.X509Certificate

/** Loads CRLs from a [File] source */
fun File.loadCRLs() = CertificateFactory.getInstance("X509").generateCRLs(inputStream()).toList()

/** Loads CRLs from a [URI] source */
fun URI.loadCRLs() =
    when (scheme) {
      "ldap" -> {
        val ldapCertStore = CertStore.getInstance("LDAP", { this })
        ldapCertStore.getCRLs(X509CRLSelector()).toList()
      }
      else -> {
        // Read the full stream before feeding to X509Factory.
        val bytes = toURL().readBytes()
        CertificateFactory.getInstance("X509").generateCRLs(bytes.inputStream()).toList()
      }
    }

/**
 * Verify the keystore public/private key certificate against the CRL.
 * [Keytool](https://github.com/openjdk/jdk/tree/master/src/java.base/share/classes/sun/security/tools/keytool)
 *
 * @return alias of the verified cert, else returns null.
 */
fun KeyStore.verifyCRL(crl: X509CRL) =
    aliases()
        .toList()
        .map { getCertificate(it) }
        .filterIsInstance<X509Certificate>()
        .filter { it.subjectX500Principal == crl.issuerX500Principal }
        .firstOrNull {
          runCatching {
                crl.verify(it.publicKey)
                true
              }
              .getOrDefault(false)
        }
        ?.let { getCertificateAlias(it) }
