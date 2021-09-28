package no.nav.helse.serde.api.v2

import java.time.LocalDate
import java.time.LocalDateTime

interface Hendelse {
    val id: String
    val type: String

    companion object {
        internal inline fun <reified T : Hendelse> List<Hendelse>.finn(): T? {
            return filterIsInstance<T>().firstOrNull()
        }
    }
}

data class Inntektsmelding(
    override val id: String,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double
) : Hendelse {
    override val type = "INNTEKTSMELDING"
}

data class SøknadNav(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertdato: LocalDateTime,
    val sendtNav: LocalDateTime
) : Hendelse {
    override val type = "SENDT_SØKNAD_NAV"

    internal fun søknadsfristOppfylt(): Boolean {
        val søknadSendtMåned = sendtNav.toLocalDate().withDayOfMonth(1)
        val senesteMuligeSykedag = fom.plusMonths(3)
        return søknadSendtMåned < senesteMuligeSykedag.plusDays(1)
    }
}

data class SøknadArbeidsgiver(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertdato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime
) : Hendelse {
    override val type = "SENDT_SØKNAD_ARBEIDSGIVER"
}

data class Sykmelding(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertdato: LocalDateTime
) : Hendelse {
    override val type = "NY_SØKNAD"
}
