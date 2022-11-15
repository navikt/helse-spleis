rootProject.name = "sykepenger-spleis"
include("jobs", "sykepenger-api", "sykepenger-model", "sykepenger-mediators", "sykepenger-opprydding-dev")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("testcontainers", "1.17.4")
            version("rapids-and-rivers", "2022111011111668075098.65e508dcde8b")
            version("postgres", "42.5.0")
            version("hikari", "5.0.1")
            version("kotliquery", "1.9.0")
            version("cloudsql", "1.7.2")
            version("flyway", "9.7.0")
            version("logback", "1.4.4")
            version("logstash", "7.2")
            version("jackson", "2.14.0")

            library("rapids-and-rivers", "com.github.navikt", "rapids-and-rivers").versionRef("rapids-and-rivers")

            library("jackson-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").versionRef("jackson")
            library("jackson-datatype", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310").versionRef("jackson")

            library("logback", "ch.qos.logback", "logback-classic").versionRef("logback")
            library("logstash", "net.logstash.logback", "logstash-logback-encoder").versionRef("logstash")

            library("postgresql", "org.postgresql", "postgresql").versionRef("postgres")
            library("hikari", "com.zaxxer", "HikariCP").versionRef("hikari")
            library("kotliquery", "com.github.seratch", "kotliquery").versionRef("kotliquery")

            library("cloudsql", "com.google.cloud.sql", "postgres-socket-factory").versionRef("cloudsql")
            library("flyway", "org.flywaydb", "flyway-core").versionRef("flyway")

            library("testcontainers", "org.testcontainers", "postgresql").versionRef("testcontainers")

            bundle("database", listOf("postgresql", "hikari", "kotliquery"))

            bundle("jackson", listOf("jackson-kotlin", "jackson-datatype"))
            bundle("logging", listOf("logback", "logstash"))
        }
    }
}