rootProject.name = "sykepenger-spleis"
include("jobs", "sykepenger-api", "sykepenger-model", "sykepenger-mediators", "sykepenger-opprydding-dev", "sykepenger-primitiver", "sykepenger-utbetaling", "sykepenger-aktivitetslogg", "sykepenger-etterlevelse", "sykepenger-etterlevelse-api")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("testcontainers", "1.19.3")
            version("rapids-and-rivers", "2024020415331707057200.009c5dd149c5")
            version("postgres", "42.7.1")
            version("hikari", "5.1.0")
            version("kotliquery", "1.9.0")
            version("cloudsql", "1.15.1")
            version("flyway", "10.5.0")
            version("logback", "1.4.14")
            version("logstash", "7.4")
            version("jackson", "2.16.1")

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