import common.*

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
  applicationDefaultJvmArgs += runJvmArgs
}

dependencies {
  implementation(libs.slf4j.simple)
  implementation(libs.bundles.ajalt)
}
