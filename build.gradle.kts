import com.javiersc.semver.project.gradle.plugin.SemverExtension
import common.*
import kotlin.collections.first
import org.gradle.kotlin.dsl.*

plugins {
  id("dev.suresh.plugin.root")
  id("dev.suresh.plugin.kotlin.jvm")
  id("dev.suresh.plugin.graalvm")
  application
  alias(libs.plugins.shadow)
}

description = "TrustStore-Scan"

buildConfig {
  enabled = true
  projectName = rootProject.name
  projectVersion = project.version.toString()
  projectDesc = rootProject.description
  gitCommit = semver.commits.get().first()
  catalogVersions = project.versionCatalogMapOf()
}

application {
  mainClass = libs.versions.app.mainclass
  applicationDefaultJvmArgs += jvmArguments()
}

val quickBuildEnabled = project.hasProperty("quick")
val muslEnabled = project.hasProperty("musl")
val reportsEnabled = project.hasProperty("reports")
val agentEnabled = project.hasProperty("agent")
val semverExtn = extensions.getByType<SemverExtension>()

graalvmNative {
  binaries.all {
    imageName = project.name
    useFatJar = false
    sharedLibrary = false
    fallback = false
    verbose = debugEnabled
    quickBuild = quickBuildEnabled
    richOutput = true
    buildArgs = buildList {
      add("--enable-preview")
      add("--enable-native-access=ALL-UNNAMED")
      add("--native-image-info")
      add("--enable-https")
      add("--install-exit-handlers")
      add("-R:MaxHeapSize=64m")
      add("-EBUILD_NUMBER=${project.version}")
      add("-ECOMMIT_HASH=${semverExtn.commits.get().first().hash}")

      add("-H:+UnlockExperimentalVMOptions")
      add("-H:+ReportExceptionStackTraces")
      add("-O3")
      // add("-Os")
      // add("-H:+ForeignAPISupport")
      // add("--features=graal.aot.RuntimeFeature")
      // add("-H:+AddAllCharsets")
      // add("-H:+IncludeAllLocales")
      // add("-H:+IncludeAllTimeZones")
      // add("-H:IncludeResources=.*(message\\.txt|\\app.properties)\$")
      // add("--enable-url-protocols=http,https,jar,unix")
      // add("--initialize-at-build-time=kotlinx,kotlin,org.slf4j")

      val monOpts = buildString { append("heapdump,jfr,jvmstat,threaddump,nmt") }
      add("--enable-monitoring=$monOpts")

      if (common.Platform.isLinux) {
        when {
          muslEnabled -> {
            add("--static")
            add("--libc=musl")
            // add("-H:CCompilerOption=-Wl,-z,stack-size=2097152")
          }
          else -> add("--static-nolibc")
        }
        add("-H:+StripDebugInfo")
      }

      // Use the compatibility mode when build image on GitHub Actions.
      when (GithubAction.isEnabled) {
        true -> add("-march=compatibility")
        else -> add("-march=native")
      }

      if (debugEnabled) {
        add("-H:+TraceNativeToolUsage")
        add("-H:+TraceSecurityServices")
        add("--trace-class-initialization=kotlin.annotation.AnnotationRetention")
        // add("--debug-attach")
      }
      // https://www.graalvm.org/latest/reference-manual/native-image/overview/Options/
    }

    // resources {
    //   autodetection {
    //     enabled = false
    //     restrictToProjectDependencies = true
    //   }
    // }

    jvmArgs = jvmArguments()
    systemProperties =
        mapOf("java.awt.headless" to "false", "jdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK" to "0")
    javaLauncher = javaToolchains.launcherFor { configureJvmToolchain(project) }
  }

  agent {
    defaultMode = "standard"
    enabled = agentEnabled
    metadataCopy {
      inputTaskNames.add("run") // Tasks previously executed with the agent attached (test).
      outputDirectories.add("src/main/resources/META-INF/native-image")
      mergeWithExisting = true
    }
  }

  metadataRepository { enabled = true }
  toolchainDetection = false
}
