package dev.suresh

import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

object Jdk {
  fun javaHome(): Path? {
    val javaHome: String? = System.getProperty("java.home", System.getenv("JAVA_HOME"))
    return when {
      // dirname $(dirname $(readlink -f $(which javac)))
      javaHome.isNullOrBlank() -> which("java")?.toRealPath()?.parent?.parent
      else -> Path(javaHome)
    }
  }

  val jdkCaCert: Path?
    get() =
        javaHome()?.let { home ->
          val cacerts =
              listOf(
                  home.resolve("jre", "lib", "security", "cacerts"),
                  home.resolve("lib", "security", "cacerts"))
          cacerts.firstOrNull { it.exists() }
        }

  private fun which(execName: String) =
      System.getenv("PATH")
          ?.split(File.pathSeparator)
          ?.map { Path(it).resolve(execName) }
          ?.firstOrNull { it.exists() }
}
