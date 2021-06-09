package no.nav.helse.serde.mapping

enum class JsonDagType {
    ARBEIDSDAG,
    ARBEIDSGIVERDAG,
    ARBEIDSGIVER_HELGEDAG,
    FERIEDAG,
    FRISK_HELGEDAG,
    FORELDET_SYKEDAG,
    PERMISJONSDAG,
    PROBLEMDAG,
    STUDIEDAG,
    SYKEDAG,
    SYK_HELGEDAG,
    UTENLANDSDAG,
    UKJENT_DAG,
}

enum class SpeilDagtype {
    ARBEIDSDAG,
    ARBEIDSGIVERDAG,
    FERIEDAG,
    FORELDET_SYKEDAG,
    FRISK_HELGEDAG,
    IMPLISITT_DAG,
    PERMISJONSDAG,
    SYKEDAG,
    SYK_HELGEDAG,
    UBESTEMTDAG,
    AVSLÅTT
}

enum class SpeilKildetype {
    Inntektsmelding,
    Søknad,
    Sykmelding,
    Saksbehandler,
    Ukjent
}
