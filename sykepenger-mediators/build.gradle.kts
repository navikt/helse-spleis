plugins {
    id("no.nav.sykepenger.deployable")
}

sykepengerDeployable {
    mainClass = "no.nav.helse.AppKt"
    imageName = "helse-spleis-spleis"
}

dependencies {
    implementation(project(":sykepenger-model"))
    implementation(project(":sykepenger-serde"))

    implementation(libs.rapids.and.rivers)
    implementation(libs.bundles.database)
    implementation(libs.bundles.flyway)
    implementation(libs.tbd.naisful.postgres)

    testImplementation(testFixtures(project(":sykepenger-primitiver")))
    testImplementation(libs.tbd.rapids.and.rivers.test)
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.spill.av.im.matching)
    testImplementation(libs.syfokafka)
    testImplementation(libs.mockk)
    testImplementation(libs.jsonassert)
}

// GCP Cloud Profiler-agenten lastes inn via JAVA_TOOL_OPTIONS (-agentpath:/opt/cprof/profiler_java_agent.so) i .nais/spleis.*.yaml
val cloudProfilerArkiv = layout.buildDirectory.file("cloud-profiler/profiler_java_agent.tar.gz")
val cloudProfilerRot = layout.buildDirectory.dir("cloud-profiler/root")

val lastNedCloudProfiler =
    tasks.register("lastNedCloudProfiler") {
        val arkiv = cloudProfilerArkiv
        outputs.file(arkiv)
        doLast {
            val fil = arkiv.get().asFile
            fil.parentFile.mkdirs()
            uri("https://storage.googleapis.com/cloud-profiler/java/latest/profiler_java_agent.tar.gz").toURL().openStream().use { inn ->
                fil.outputStream().use { inn.copyTo(it) }
            }
        }
    }

val pakkUtCloudProfiler =
    tasks.register<Sync>("pakkUtCloudProfiler") {
        dependsOn(lastNedCloudProfiler)
        from(tarTree(resources.gzip(cloudProfilerArkiv))) {
            include("profiler_java_agent.so")
        }
        into(cloudProfilerRot.map { it.dir("opt/cprof") })
    }

jib {
    extraDirectories {
        paths {
            path {
                setFrom(cloudProfilerRot)
                into = "/"
            }
        }
    }
}

tasks.matching { it.name in setOf("jib", "jibDockerBuild", "jibBuildTar") }.configureEach {
    dependsOn(pakkUtCloudProfiler)
}
