package dev.suresh.security

import dev.suresh.security.Jdk.javaHome
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.jvm.optionals.getOrNull

object Jdk {
  fun javaHome(): Path? {
    val javaHome: String? = System.getProperty("java.home", System.getenv("JAVA_HOME"))
    return when {
      // dirname $(dirname $(readlink -f $(which javac)))
      javaHome.isNullOrBlank() -> which("java")?.toRealPath()?.parent?.parent
      else -> Path(javaHome)
    }
  }

  /**
   * Gets the path to the JDK's cacerts truststore file.
   *
   * Looks for the cacerts file in the following locations relative to [javaHome]:
   * - jre/lib/security/cacerts (for JDK 8 and earlier)
   * - lib/security/cacerts (for JDK 9+)
   *
   * @return Path to the first existing cacerts file found, or null if none exists
   */
  val jdkCaCert: Path?
    get() =
        javaHome()?.let { home ->
          val cacerts =
              listOf(
                  home.resolve("jre", "lib", "security", "cacerts"),
                  home.resolve("lib", "security", "cacerts"))
          cacerts.firstOrNull { it.exists() }
        }

  /**
   * Gets all Java processes currently running on the system
   *
   * @return List of ProcessHandle for Java processes
   */
  val javaProcesses: List<ProcessHandle>
    get() =
        ProcessHandle.allProcesses()
            .filter { it.info().command().orElse("").contains("java", ignoreCase = true) }
            .toList()

  /**
   * Gets the custom truststore configuration for all running Java processes on the system.
   *
   * @return Map of truststore paths to their passwords for Java processes with custom trust-stores
   */
  val javaProcessTrustStores
    get() = javaProcesses.mapNotNull { it.customTrustStore }.toMap()
}

/**
 * Gets the custom truststore configuration for this Java process, if one is specified. Looks for
 * `-Djavax.net.ssl.trustStore` and `-Djavax.net.ssl.trustStorePassword` JVM arguments.
 *
 * @return Pair of (truststore path, password) if custom truststore is configured, `null` otherwise
 */
val ProcessHandle.customTrustStore: Pair<String, String?>?
  get() {
    val args = info().arguments().getOrNull().orEmpty()
    return args
        .find { it.trim().startsWith("-Djavax.net.ssl.trustStore=", ignoreCase = true) }
        ?.substringAfter("=")
        ?.let { trustStore ->
          val password =
              args
                  .find {
                    it.trim().startsWith("-Djavax.net.ssl.trustStorePassword=", ignoreCase = true)
                  }
                  ?.substringAfter("=")
          trustStore to password
        }
  }

/**
 * Finds the full path of an executable on the system PATH. Similar to Unix 'which' command.
 *
 * @param execName Name of the executable to find
 * @return Path to the executable if found on PATH, null otherwise
 */
fun which(execName: String) =
    System.getenv("PATH")
        ?.split(File.pathSeparator)
        ?.map { Path(it).resolve(execName) }
        ?.firstOrNull { it.exists() }
