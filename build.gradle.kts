import java.io.PrintWriter

plugins {
    kotlin("jvm") version "2.0.0"
}

val junitJupiterVersion = "5.10.2"
val jvmTargetVersion = 21
val gradleVersjon = "8.6"

allprojects {
    group = "no.nav.helse"
    version = properties["version"] ?: "local-build"

    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        val githubPassword: String? by project
        mavenCentral()
        /* ihht. https://github.com/navikt/utvikling/blob/main/docs/teknisk/Konsumere%20biblioteker%20fra%20Github%20Package%20Registry.md
            så plasseres github-maven-repo (med autentisering) før nav-mirror slik at github actions kan anvende førstnevnte.
            Det er fordi nav-mirroret kjører i Google Cloud og da ville man ellers fått unødvendige utgifter til datatrafikk mellom Google Cloud og GitHub
         */
        maven {
            url = uri("https://maven.pkg.github.com/navikt/maven-release")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }

    /*
        avhengigheter man legger til her blir lagt på -alle- prosjekter.
        med mindre alle submodulene (modellen, apiet, jobs, osv) har behov for samme avhengighet,
        bør det heller legges til de enkelte som har behov.
        Dersom det er flere som har behov så kan det være lurt å legge avhengigheten til
         dependencyResolutionManagement i settings.gradle.kts
     */
    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks {
        java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(jvmTargetVersion)
            }
        }

        withType<Wrapper> {
            gradleVersion = gradleVersjon
        }
        withType<Jar> {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin="java-library")
    apply(plugin="java-test-fixtures")

    tasks {
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

