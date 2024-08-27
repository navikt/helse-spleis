rootProject.name = "sykepenger-spleis"
include("jobs", "sykepenger-serde", "sykepenger-api", "sykepenger-api-dto", "sykepenger-model", "sykepenger-model-dto", "sykepenger-mediators", "sykepenger-opprydding-dev", "sykepenger-primitiver", "sykepenger-primitiver-dto", "sykepenger-utbetaling", "sykepenger-utbetaling-dto", "sykepenger-aktivitetslogg", "sykepenger-aktivitetslogg-dto", "sykepenger-etterlevelse", "sykepenger-etterlevelse-api")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("testcontainers", "1.20.1")
            version("rapids-and-rivers", "2024082712401724755232.610a5f59323d")
            version("postgres", "42.7.3")
            version("hikari", "5.1.0")
            version("kotliquery", "1.9.0")
            version("cloudsql", "1.20.0")
            version("flyway", "10.17.1")
            version("logback", "1.5.7")
            version("logstash", "8.0")
            version("jackson", "2.17.2")

            library("rapids-and-rivers", "com.github.navikt", "rapids-and-rivers").versionRef("rapids-and-rivers")

            library("jackson-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").versionRef("jackson")
            library("jackson-datatype", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310").versionRef("jackson")

            library("logback", "ch.qos.logback", "logback-classic").versionRef("logback")
            library("logstash", "net.logstash.logback", "logstash-logback-encoder").versionRef("logstash")

            library("postgresql", "org.postgresql", "postgresql").versionRef("postgres")
            library("hikari", "com.zaxxer", "HikariCP").versionRef("hikari")
            library("kotliquery", "com.github.seratch", "kotliquery").versionRef("kotliquery")

            library("cloudsql", "com.google.cloud.sql", "postgres-socket-factory").versionRef("cloudsql")
            library("flyway-core", "org.flywaydb", "flyway-core").versionRef("flyway")
            library("flyway-postgres", "org.flywaydb", "flyway-database-postgresql").versionRef("flyway")

            library("testcontainers", "org.testcontainers", "postgresql").versionRef("testcontainers")

            bundle("flyway", listOf("flyway-core", "flyway-postgres"))
            bundle("database", listOf("postgresql", "hikari", "kotliquery"))

            bundle("jackson", listOf("jackson-kotlin", "jackson-datatype"))
            bundle("logging", listOf("logback", "logstash"))
        }
    }
}