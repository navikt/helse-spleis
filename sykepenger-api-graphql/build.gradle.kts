val jacksonVersion = "2.18.1"
val ktorVersion = "2.2.2"
val kGraphQLVersion = "0.19.0"
val jsonassertVersion = "1.5.1"

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")

    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("com.apurebase:kgraphql:$kGraphQLVersion")
    testImplementation("com.apurebase:kgraphql-ktor:$kGraphQLVersion")
}
