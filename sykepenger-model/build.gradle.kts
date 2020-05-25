val jacksonVersion = "2.10.0"
val slf4jVersion = "1.7.30"
val logbackVersion = "1.2.3"

dependencies {
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("commons-codec:commons-codec:1.11")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    testImplementation("ch.qos.logback:logback-classic:$logbackVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
