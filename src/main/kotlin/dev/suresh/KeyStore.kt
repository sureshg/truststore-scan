package dev.suresh

import java.nio.file.Path
import java.security.KeyStore
import java.security.cert.PKIXParameters
import java.security.cert.X509Certificate
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile

/**
 * Converts the current KeyStore to a `PKCS#12` format [KeyStore] by copying all matching entries.
 * If the current [KeyStore] is already PKCS#12, returns it unchanged.
 *
 * @param keyPassword The password used to protect key entries in the new KeyStore.
 * @param aliasFilter Optional regex pattern to filter which aliases should be included in the new
 *   KeyStore.
 * @return A new KeyStore in PKCS#12 format containing the filtered entries from the original
 *   KeyStore.
 */
fun KeyStore.toPKCS12(keyPassword: CharArray? = null, aliasFilter: Regex? = null): KeyStore =
    when (type.uppercase()) {
      "PKCS12" -> this
      else ->
          KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            val protParam = KeyStore.PasswordProtection(keyPassword)
            aliases()
                .toList()
                .filter { aliasFilter == null || it.matches(aliasFilter) }
                .forEach { alias ->
                  val entry = getEntry(alias, if (isKeyEntry(alias)) protParam else null)
                  setEntry(alias, entry, protParam)
                }
          }
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

val KeyStore.trustAnchors
  get() = PKIXParameters(this).trustAnchors.mapNotNull { it.trustedCert }

/**
 * Creates a [KeyStore] instance from a file path.
 *
 * @param type The type of KeyStore to create. Defaults to Java Keystore (JKS) type for maximum
 *   compatibility.
 * @param storePasswd The password to unlock the KeyStore. Default is null. For system cacerts, the
 *   default password is typically "changeit".
 * @return The loaded [KeyStore] instance, or null if the path is not a regular file.
 */
fun Path.toKeyStore(type: String = "JKS", storePasswd: CharArray? = null) =
    if (isRegularFile()) {
      inputStream().use { fis -> KeyStore.getInstance(type).apply { load(fis, storePasswd) } }
    } else null
