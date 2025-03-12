package dev.suresh.security

import com.github.marschall.directorykeystore.*
import java.nio.file.Path
import java.security.KeyStore
import java.security.Security
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * JVM can be switched to use a different truststore using **-Djavax.net.ssl.trustStoreType=xxx**
 */
object TrustStore {

  /** Retrieves a list of distinct KeyStore provider names */
  fun providers(): List<String> =
      Security.getProviders()
          .flatMap { it.entries }
          .map { it.key.toString() }
          .filter { it.startsWith("KeyStore.") && it.endsWith("ImplementedIn").not() }
          .map { it.substringAfter("KeyStore.").trim() }
          .distinct()

  /** Creates a KeyStore instance of the specified trust store type */
  fun ofType(type: TrustStoreType): KeyStore =
      when (type) {
        is TrustStoreType.Directory -> {
          if (Security.getProvider(DirectoryKeystoreProvider.NAME) == null) {
            Security.addProvider(DirectoryKeystoreProvider())
          }
          KeyStore.getInstance(type.name).apply { load(DirectoryLoadStoreParameter(type.path)) }
        }
        else -> KeyStore.getInstance(type.name).apply { load(null, null) }
      }

  val caCertsTrustManager by lazy {
    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).run {
      init(null as KeyStore?)
      trustManagers.filterIsInstance<X509TrustManager>()
    }
  }

  val caCerts by lazy { caCertsTrustManager.flatMap { it.acceptedIssuers.toList() } }
}

sealed class TrustStoreType(val name: String) {

  data object WIN_USER : TrustStoreType("Windows-MY")

  data object WIN_SYSTEM : TrustStoreType("Windows-ROOT")

  data object MACOS_USER : TrustStoreType("KeychainStore")

  data object MACOS_SYSTEM : TrustStoreType("KeychainStore-ROOT")

  data class Directory(val path: Path) : TrustStoreType("Directory")
}
