import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `maven-publish`
    id("io.papermc.paperweight.patcher") version "2.0.0-beta.18"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"
val leafMavenPublicUrl = "https://maven.leafmc.one/snapshots/"

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
        maven(leafMavenPublicUrl)
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
        options.isFork = true
        options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-removal"))
    }
    tasks.withType<Javadoc>().configureEach {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = Charsets.UTF_8.name()
    }
    tasks.withType<Test>().configureEach {
        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STANDARD_OUT)
        }
    }

    extensions.configure<PublishingExtension> {
        repositories {
            maven(leafMavenPublicUrl) {
                name = "leaf"

                credentials.username = System.getenv("REPO_USER")
                credentials.password = System.getenv("REPO_PASSWORD")
            }
        }
    }
}

paperweight {
    upstreams.register("leaf") {
        repo = github("Winds-Studio", "Leaf")
        ref = providers.gradleProperty("leafCommit")

        patchFile {
            path = "leaf-server/build.gradle.kts"
            outputFile = file("sapling-server/build.gradle.kts")
            patchFile = file("sapling-server/build.gradle.kts.patch")
        }
        patchFile {
            path = "leaf-api/build.gradle.kts"
            outputFile = file("sapling-api/build.gradle.kts")
            patchFile = file("sapling-api/build.gradle.kts.patch")
        }
        patchRepo("paperApi") {
            upstreamPath = "paper-api"
            patchesDir = file("sapling-api/paper-patches")
            outputDir = file("paper-api")
        }
        patchDir("leafApi") {
            upstreamPath = "leaf-api"
            excludes = listOf("build.gradle.kts", "build.gradle.kts.patch", "paper-patches")
            patchesDir = file("sapling-api/leaf-patches")
            outputDir = file("leaf-api")
        }
    }
}
