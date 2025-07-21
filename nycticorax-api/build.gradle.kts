plugins {
    `java-library`
    `maven-publish`
    idea
}

java {
    withSourcesJar()
    withJavadocJar()
}

val annotationsVersion = "26.0.2"
// Keep in sync with paper-server adventure-text-serializer-ansi dep
val adventureVersion = "4.23.0"
val bungeeCordChatVersion = "1.21-R0.2-deprecated+build.21"
// Leaf start - Bump Dependencies
val slf4jVersion = "2.0.17"
val log4jVersion = "2.24.3"
// Leaf end - Bump Dependencies

val apiAndDocs: Configuration by configurations.creating {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}
configurations.api {
    extendsFrom(apiAndDocs)
}

// Configure mockito agent that is needed in newer Java versions
val mockitoAgent = configurations.register("mockitoAgent")
abstract class MockitoAgentProvider : CommandLineArgumentProvider {
    @get:CompileClasspath
    abstract val fileCollection: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> {
        return listOf("-javaagent:" + fileCollection.files.single().absolutePath)
    }
}

dependencies {
    // api dependencies are listed transitively to API consumers
    // Leaf start - Bump Dependencies
    api("com.google.guava:guava:33.4.0-jre")
    // Waiting Paper, Gson has breaking change since 2.12.0
    // See https://github.com/google/gson/commit/6c2e3db7d25ceceabe056aeb8b65477fdd509214
    api("com.google.code.gson:gson:2.11.0")
    api("org.yaml:snakeyaml:2.3") // 2.4 removed `org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder`
    // Leaf end - Bump Dependencies
    api("org.joml:joml:1.10.8") {
        isTransitive = false // https://github.com/JOML-CI/JOML/issues/352
    }
    api("it.unimi.dsi:fastutil:8.5.15")
    api("org.apache.logging.log4j:log4j-api:$log4jVersion")
    api("org.slf4j:slf4j-api:$slf4jVersion")
    api("com.mojang:brigadier:1.3.10")
    api("io.sentry:sentry:8.13.2") // Pufferfish

    // Deprecate bungeecord-chat in favor of adventure
    api("net.md-5:bungeecord-chat:$bungeeCordChatVersion") {
        exclude("com.google.guava", "guava")
    }

    apiAndDocs(platform("net.kyori:adventure-bom:$adventureVersion"))
    apiAndDocs("net.kyori:adventure-api")
    apiAndDocs("net.kyori:adventure-text-minimessage")
    apiAndDocs("net.kyori:adventure-text-serializer-gson")
    apiAndDocs("net.kyori:adventure-text-serializer-legacy")
    apiAndDocs("net.kyori:adventure-text-serializer-plain")
    apiAndDocs("net.kyori:adventure-text-logger-slf4j")

    // Leaf start - Bump Dependencies
    api("org.apache.maven:maven-resolver-provider:3.9.9") // make API dependency for Paper Plugins
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.22") // Dreeam TODO - Update to 2.0.1
    implementation("org.apache.maven.resolver:maven-resolver-transport-http:1.9.22") // Dreeam TODO - Update to 2.0.1
    // Leaf start - Bump Dependencies

    // Annotations - Slowly migrate to jspecify
    val annotations = "org.jetbrains:annotations:$annotationsVersion"
    compileOnly(annotations)
    testCompileOnly(annotations)

    val checkerQual = "org.checkerframework:checker-qual:3.49.2"
    compileOnlyApi(checkerQual)
    testCompileOnly(checkerQual)

    api("org.jspecify:jspecify:1.0.0")

    // Test dependencies
    testImplementation("org.apache.commons:commons-lang3:3.17.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    // Leaf start - Bump Dependencies
    testImplementation("org.hamcrest:hamcrest:3.0")
    testImplementation("org.mockito:mockito-core:5.16.1")
    // Leaf end - Bump Dependencies
    testImplementation("org.ow2.asm:asm-tree:9.8")
    mockitoAgent("org.mockito:mockito-core:5.16.1") { isTransitive = false } // configure mockito agent that is needed in newer java versions // Leaf - Bump Dependencies
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Leaf start - Bump Dependencies
    // commons-lang3 is removed in maven-resolver-provider since 3.9.8
    // Add this because bukkit api still need it.
    compileOnly("org.apache.commons:commons-lang3:3.17.0")
    // Leaf end - Bump Dependencies
}

val generatedDir: java.nio.file.Path = rootProject.layout.projectDirectory.dir("paper-api/src/generated/java").asFile.toPath() // Leaf - project setup
idea {
    module {
        generatedSourceDirs.add(generatedDir.toFile())
    }
}
sourceSets {
    main {
        java {
            srcDir(generatedDir)
            // Leaf start - project setup
            srcDir(file("../paper-api/src/main/java"))
            srcDir(file("../leaf-api/src/main/java")) // Nycticorax - project setup
        }
        resources {
            srcDir(file("../paper-api/src/main/resources"))
            srcDir(file("../leaf-api/src/main/resources")) // Nycticorax - project setup
        }
    }
    test {
        java {
            srcDir(file("../paper-api/src/test/java"))
            srcDir(file("../leaf-api/src/test/java")) // Nycticorax - project setup
        }
        resources {
            srcDir(file("../paper-api/src/test/resources"))
            srcDir(file("../leaf-api/src/test/resources")) // Nycticorax - project setup
            // Leaf end - project setup
        }
    }
}

val outgoingVariants = arrayOf("runtimeElements", "apiElements", "sourcesElements", "javadocElements")
val mainCapability = "${project.group}:${project.name}:${project.version}"
configurations {
    val outgoing = outgoingVariants.map { named(it) }
    for (config in outgoing) {
        config {
            attributes {
                attribute(io.papermc.paperweight.util.mainCapabilityAttribute, mainCapability)
            }
            outgoing {
                capability(mainCapability)
                // Paper-MojangAPI has been merged into Paper-API
                capability("io.papermc.paper:paper-mojangapi:${project.version}")
                capability("com.destroystokyo.paper:paper-mojangapi:${project.version}")
                // Conflict with old coordinates
                capability("com.destroystokyo.paper:paper-api:${project.version}")
                capability("org.spigotmc:spigot-api:${project.version}")
                capability("org.bukkit:bukkit:${project.version}")
            }
        }
    }
}

configure<PublishingExtension> {
    publications.create<MavenPublication>("maven") {
        // For Brigadier API
        outgoingVariants.forEach {
            suppressPomMetadataWarningsFor(it)
        }
        from(components["java"])
    }
}

// Gale start - hide irrelevant compilation warnings
tasks.withType<JavaCompile> {
    val compilerArgs = options.compilerArgs
    compilerArgs.add("-Xlint:-module")
    compilerArgs.add("-Xlint:-removal")
    compilerArgs.add("-Xlint:-dep-ann")
    compilerArgs.add("--add-modules=jdk.incubator.vector") // Gale - Pufferfish - SIMD support
}
// Gale end - hide irrelevant compilation warnings
val generateApiVersioningFile by tasks.registering {
    inputs.property("version", project.version)
    val pomProps = layout.buildDirectory.file("pom.properties")
    outputs.file(pomProps)
    val projectVersion = project.version
    doLast {
        pomProps.get().asFile.writeText("version=$projectVersion")
    }
}

tasks.jar {
    from(generateApiVersioningFile.map { it.outputs.files.singleFile }) {
        into("META-INF/maven/${project.group}/${project.name}")
    }
    manifest {
        attributes(
            "Automatic-Module-Name" to "org.bukkit"
        )
    }

    // Gale start - package license into jar
    from("${project.projectDir}/LICENSE.txt") {
        into("")
    }
    // Gale end - package license into jar
}

abstract class Services {
    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations
}
val services = objects.newInstance<Services>()

tasks.withType<Javadoc>().configureEach {
    val options = options as StandardJavadocDocletOptions
    options.overview = "../paper-api/src/main/javadoc/overview.html" // Leaf - project setup
    options.use()
    options.isDocFilesSubDirs = true
    options.links(
        "https://guava.dev/releases/33.4.0-jre/api/docs/", // Leaf - Bump Dependencies
        "https://javadoc.io/doc/org.yaml/snakeyaml/2.3/", // Leaf - Bump Dependencies
        "https://javadoc.io/doc/org.jetbrains/annotations/$annotationsVersion/",
        "https://javadoc.io/doc/org.joml/joml/1.10.8/",
        "https://www.javadoc.io/doc/com.google.code.gson/gson/2.11.0",
        "https://jspecify.dev/docs/api/",
        "https://jd.advntr.dev/api/$adventureVersion/",
        "https://jd.advntr.dev/key/$adventureVersion/",
        "https://jd.advntr.dev/text-minimessage/$adventureVersion/",
        "https://jd.advntr.dev/text-serializer-gson/$adventureVersion/",
        "https://jd.advntr.dev/text-serializer-legacy/$adventureVersion/",
        "https://jd.advntr.dev/text-serializer-plain/$adventureVersion/",
        "https://jd.advntr.dev/text-logger-slf4j/$adventureVersion/",
        "https://javadoc.io/doc/org.slf4j/slf4j-api/$slf4jVersion/",
        "https://logging.apache.org/log4j/2.x/javadoc/log4j-api/",
        "https://javadoc.io/doc/org.apache.maven.resolver/maven-resolver-api/1.9.22", // Leaf - Bump Dependencies
    )
    options.tags("apiNote:a:API Note:")

    inputs.files(apiAndDocs).ignoreEmptyDirectories().withPropertyName(apiAndDocs.name + "-configuration")
    val apiAndDocsElements = apiAndDocs.elements
    doFirst {
        options.addStringOption(
            "sourcepath",
            apiAndDocsElements.get().map { it.asFile }.joinToString(separator = File.pathSeparator, transform = File::getPath)
        )
    }

    // workaround for https://github.com/gradle/gradle/issues/4046
    inputs.dir("../paper-api/src/main/javadoc").withPropertyName("javadoc-sourceset") // Leaf - project setup
    val fsOps = services.fileSystemOperations
    doLast {
        fsOps.copy {
            from("../paper-api/src/main/javadoc") { // Leaf - project setup
                include("**/doc-files/**")
            }
            into("build/docs/javadoc")
        }
    }

    options.addStringOption("Xdoclint:none", "-quiet") // Gale - hide irrelevant compilation warnings
    options.addStringOption("-add-modules", "jdk.incubator.vector") // Gale - Pufferfish - SIMD support
}

tasks.test {
    useJUnitPlatform()

    // configure mockito agent that is needed in newer java versions
    val provider = objects.newInstance<MockitoAgentProvider>()
    provider.fileCollection.from(mockitoAgent)
    jvmArgumentProviders.add(provider)
}

// Compile tests with -parameters for better junit parameterized test names
tasks.compileTestJava {
    options.compilerArgs.add("-parameters")
}

val scanJarForBadCalls by tasks.registering(io.papermc.paperweight.tasks.ScanJarForBadCalls::class) {
    badAnnotations.add("Lio/papermc/paper/annotation/DoNotUse;")
    jarToScan.set(tasks.jar.flatMap { it.archiveFile })
    classpath.from(configurations.compileClasspath)
}
// Leaf start - Bump Dependencies
repositories {
    mavenCentral()
}
// Leaf end - Bump Dependencies
tasks.check {
    dependsOn(scanJarForBadCalls)
}

if (providers.gradleProperty("updatingMinecraft").getOrElse("false").toBoolean()) {
    val scanJarForOldGeneratedCode by tasks.registering(io.papermc.paperweight.tasks.ScanJarForOldGeneratedCode::class) {
        mcVersion.set(providers.gradleProperty("mcVersion"))
        annotation.set("Lio/papermc/paper/generated/GeneratedFrom;")
        jarToScan.set(tasks.jar.flatMap { it.archiveFile })
        classpath.from(configurations.compileClasspath)
    }
    tasks.check {
        dependsOn(scanJarForOldGeneratedCode)
    }
}
