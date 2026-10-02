import java.io.PrintWriter

plugins {
    alias(libs.plugins.sykepenger.root)
    alias(libs.plugins.sykepenger.kotlin) apply false
    alias(libs.plugins.sykepenger.deployable) apply false
}

allprojects {
    group = "no.nav.helse"
    version = properties["version"] ?: "local-build"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "java-test-fixtures")

    tasks {
        withType<Jar> {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        withType<Test> {
            maxHeapSize = "6G"
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

