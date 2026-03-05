import java.io.PrintWriter
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "2.3.0" apply false
}

val junitJupiterVersion = "6.0.2"

allprojects {
    group = "no.nav.helse"
    version = properties["version"] ?: "local-build"

    // Sett opp repositories basert på om vi kjører i CI eller ikke
    // Jf. https://github.com/navikt/utvikling/blob/main/docs/teknisk/Konsumere%20biblioteker%20fra%20Github%20Package%20Registry.md
    repositories {
        mavenCentral()
        if (providers.environmentVariable("GITHUB_ACTIONS").orNull == "true") {
            maven {
                url = uri("https://maven.pkg.github.com/navikt/maven-release")
                credentials {
                    username = "token"
                    password = providers.environmentVariable("GITHUB_TOKEN").orNull!!
                }
            }
        } else {
            maven("https://repo.adeo.no/repository/github-package-registry-navikt/")
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin="java-library")
    apply(plugin="java-test-fixtures")

    /*
        avhengigheter man legger til her blir lagt på -alle- prosjekter.
        med mindre alle submodulene (modellen, apiet, jobs, osv) har behov for samme avhengighet,
        bør det heller legges til de enkelte som har behov.
        Dersom det er flere som har behov så kan det være lurt å legge avhengigheten til
         dependencyResolutionManagement i settings.gradle.kts
     */
    val testImplementation by configurations
    val testRuntimeOnly by configurations
    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    configure<KotlinJvmProjectExtension> {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of("21"))
        }
    }

    tasks {
        withType<Jar> {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        withType<Test> {
            maxHeapSize = "6G"
            useJUnitPlatform()
            testLogging {
                events("skipped", "failed")
            }
        }
    }
}

/**
 * kjør gjerne denne slik:
 *
 *  ./gradlew tegn_modul_graf -Putputt=EnGrad.md
 *
 * så får du en fin graf under /doc/EnGrad.md
 */
tasks.create("tegn_modul_graf") {
    doLast {
        val printer: PrintWriter = hentPrinter()
        printer.println("```mermaid\n")
        printer.println("classDiagram\n")
        this.project.allprojects.forEach { it.listUtModulAvhengigheter(printer) }
        printer.println("\n```")
        printer.flush()
        printer.close()
    }
}

fun hentPrinter() = if (ext.properties["utputt"] != null) {
    val paff = "${project.rootDir.absolutePath}/doc/${ext.properties["utputt"]}"
    val fail = File(paff)
    if (!fail.exists()) {
        fail.createNewFile()
    }
    fail.printWriter()
} else PrintWriter(System.out, true)

fun Project.listUtModulAvhengigheter(printer: PrintWriter) {
    val deps = mutableSetOf<String>()
    this.configurations.forEach { configuration ->
        if (configuration.dependencies.size > 0) {
            configuration.dependencies.forEach { dependency ->
                if (dependency.name.startsWith("sykepenger-") && dependency.name != this.name) {
                    deps.add(dependency.name)
                }
            }
        }
    }
    if (deps.isNotEmpty()) {
        deps.forEach { printer.println("\t${this.name}-->$it") }
    }
}

