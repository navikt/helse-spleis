dependencies {
    implementation("commons-codec:commons-codec:1.11")
    api("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2019.11.08-09-49-c3234")
    testImplementation("no.nav.syfo.kafka:sykepengesoknad:0b2a259676f7a78da70d65838851b05925d6de6f")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
