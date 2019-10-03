package no.nav.helse.person.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class PersonJson(
        val aktorId: String,
        val arbeidsgivere: List<ArbeidsgiverJson>
)

data class ArbeidsgiverJson(
        val organisasjonsnummer: String,
        val sakskompleks: List<Sakskompleks>
)

data class SakskompleksJson(
        val tilstand: Sakskompleks.TilstandType
)

data class SoknadJson(
        val id: String,
        val sykmeldingId: String,
        val status: String,
        val aktorId: String,
        val fom: LocalDate,
        val tom: LocalDate,
        val opprettet: LocalDateTime,
        val egenmeldinger: List<Egenmeldingsperiode>,
        val soknadsperioder: List<Soknadsperiode>,
        val fravar: List<Fravarsperiode>,
        val arbeidGjenopptatt: LocalDate?,
        val korrigerer: String
)

data class Egenmeldingsperiode(
        val fom: LocalDate,
        val tom: LocalDate
)

data class Soknadsperiode(
        val fom: LocalDate,
        val tom: LocalDate
)

data class Fravarsperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val type: Fravarstype
)

enum class Soknadstatus {
    NY,
    SENDT
}

enum class Fravarstype {
    FERIE,
    PERMISJON,
    UTLANDSOPPHOLD,
    UTDANNING_FULLTID,
    UTDANNING_DELTID
}
