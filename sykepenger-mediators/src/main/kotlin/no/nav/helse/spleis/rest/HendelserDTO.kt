package no.nav.helse.spleis.rest

import no.nav.helse.spleis.db.Meldingstype
import java.time.LocalDate
import java.time.LocalDateTime

internal sealed class HendelseDTO {
    abstract val type: Meldingstype

    data class NySøknadDTO(
        val rapportertdato: LocalDateTime,
        val fom: LocalDate,
        val tom: LocalDate
    ): HendelseDTO() {
        override val type = Meldingstype.NY_SØKNAD
    }

    data class SendtSøknadDTO(
        val rapportertdato: LocalDateTime,
        val sendtNav: LocalDateTime,
        val fom: LocalDate,
        val tom: LocalDate
    ): HendelseDTO() {
        override val type = Meldingstype.SENDT_SØKNAD
    }

    data class InntektsmeldingDTO(
        val beregnetInntekt: Number,
        val førsteFraværsdag: LocalDate
    ): HendelseDTO() {
        override val type = Meldingstype.INNTEKTSMELDING
    }
}
