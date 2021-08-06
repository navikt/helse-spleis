val mainClass = "no.nav.helse.AppKt"

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:20210617121814-3e67e4d")
    implementation(project(":sykepenger-model"))

    testImplementation("com.github.navikt:inntektsmelding-kontrakt:2020.04.06-ab8f786")
    testImplementation("com.github.navikt:syfokafka:2021.02.15-14.09-103a1544")
    testImplementation("io.mockk:mockk:1.12.0")
}

repositories {
    maven("https://jitpack.io")
}

tasks {
    withType<Jar> {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = mainClass
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }
}
