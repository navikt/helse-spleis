package no.nav.helse.person

enum class DokumentType(val ekstern: Boolean = false) {
    Søknad(ekstern = true),
    InntektsmeldingInntekt(ekstern = true),
    InntektsmeldingRefusjon(ekstern = true),
    InntektsmeldingDager(ekstern = true),
    /** Intern dokumentsporing som ikke sendes ut på Kafka-events
    men kan sees i Spanner f.eks. **/
    // Du skulle kanskje tro sykmelding var ektern?
    //  - 1) Det lages ingen dokumentsporing mot sykmelding i Spleis i dag, bare skjedd på historiske ting.
    //  - 2) Sporbar fisker ut "sykmeldingId" fra søknad (som den finner basert på dokumentsporing til Søknad som er ekstern)
    Sykmelding,
    InntektFraAOrdningen,
    OverstyrTidslinje,
    OverstyrInntekt,
    OverstyrRefusjon,
    OverstyrArbeidsgiveropplysninger,
    OverstyrArbeidsforhold,
    SkjønnsmessigFastsettelse,
    AndreYtelser
}

