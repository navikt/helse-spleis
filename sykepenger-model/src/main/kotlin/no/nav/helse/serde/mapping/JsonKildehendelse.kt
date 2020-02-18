package no.nav.helse.serde.mapping

@Deprecated("Bare brukt til parsing av JSON: må bli erstattet med migrering av JSON")
enum class JsonKildehendelse {
    Sykmelding,
    Søknad,
    Inntektsmelding
}
