package no.nav.helse.serde.api.v2

import java.time.LocalDate
import java.time.LocalDateTime

interface HendelseDTO {
    val id: String
    val type: String

    companion object {
        internal inline fun <reified T : HendelseDTO> List<HendelseDTO>.finn(): T? {
            return filterIsInstance<T>().firstOrNull()
        }
    }
}

data class InntektsmeldingDTO(
    override val id: String,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double
) : HendelseDTO {
    override val type = "INNTEKTSMELDING"
}

data class SøknadNavDTO(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertdato: LocalDateTime,
    val sendtNav: LocalDateTime
) : HendelseDTO {
    override val type = "SENDT_SØKNAD_NAV"

    internal fun søknadsfristOppfylt(): Boolean {
        val søknadSendtMåned = sendtNav.toLocalDate().withDayOfMonth(1)
        val senesteMuligeSykedag = fom.plusMonths(3)
        return søknadSendtMåned < senesteMuligeSykedag.plusDays(1)
    }
}

data class SøknadArbeidsgiverDTO(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertdato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime
) : HendelseDTO {
    override val type = "SENDT_SØKNAD_ARBEIDSGIVER"
}

data class SykmeldingDTO(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertdato: LocalDateTime
) : HendelseDTO {
    override val type = "NY_SØKNAD"
}
