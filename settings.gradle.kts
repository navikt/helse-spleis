rootProject.name = "sykepenger-spleis"
include(
    "jobs", "sykepenger-serde", "sykepenger-api", "sykepenger-api-graphql", "sykepenger-api-dto", "sykepenger-model", "sykepenger-model-dto",
    "sykepenger-mediators", "sykepenger-opprydding-dev", "sykepenger-primitiver", "sykepenger-primitiver-dto", "sykepenger-utbetaling",
    "sykepenger-utbetaling-dto", "sykepenger-aktivitetslogg", "sykepenger-aktivitetslogg-dto", "sykepenger-etterlevelse-api"
)

val rapidsAndRiversVersion = "2025012712551737978926.de930d8e0feb"
val tbdLibsVersion = "2025.02.25-15.45-523b4479"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("testcontainers", "1.20.3")
            version("rapids-and-rivers", rapidsAndRiversVersion)
            version("postgres", "42.7.4")
            version("hikari", "6.1.0")
            version("cloudsql", "1.20.0")
            version("flyway", "10.21.0")
            version("logback", "1.5.12")
            version("logstash", "8.0")
            version("jackson", "2.18.1")

            library("rapids-and-rivers", "com.github.navikt", "rapids-and-rivers").versionRef("rapids-and-rivers")

            library("jackson-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").versionRef("jackson")
            library("jackson-datatype", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310").versionRef("jackson")

            library("logback", "ch.qos.logback", "logback-classic").versionRef("logback")
            library("logstash", "net.logstash.logback", "logstash-logback-encoder").versionRef("logstash")

            library("postgresql", "org.postgresql", "postgresql").versionRef("postgres")
            library("hikari", "com.zaxxer", "HikariCP").versionRef("hikari")
            library("tbd-sql", "com.github.navikt.tbd-libs", "sql-dsl").version(tbdLibsVersion)

            library("cloudsql", "com.google.cloud.sql", "postgres-socket-factory").versionRef("cloudsql")
            library("flyway-postgres", "org.flywaydb", "flyway-database-postgresql").versionRef("flyway")

            library("testcontainers", "org.testcontainers", "postgresql").versionRef("testcontainers")

            bundle("flyway", listOf("flyway-postgres"))
            bundle("database", listOf("postgresql", "hikari", "tbd-sql"))

            bundle("jackson", listOf("jackson-kotlin", "jackson-datatype"))
            bundle("logging", listOf("logback", "logstash"))
        }
    }
}
