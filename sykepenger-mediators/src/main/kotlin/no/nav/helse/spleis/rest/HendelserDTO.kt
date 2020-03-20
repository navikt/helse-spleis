package no.nav.helse.spleis.rest

import no.nav.helse.spleis.db.Meldingstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal sealed class HendelseDTO {
    abstract val type: Meldingstype

    data class NySøknadDTO(
        val hendelseId: UUID,
        val rapportertdato: LocalDateTime,
        val fom: LocalDate,
        val tom: LocalDate
    ): HendelseDTO() {
        override val type = Meldingstype.NY_SØKNAD
    }

    data class SendtSøknadDTO(
        val hendelseId: UUID,
        val rapportertdato: LocalDateTime,
        val sendtNav: LocalDateTime,
        val fom: LocalDate,
        val tom: LocalDate
    ): HendelseDTO() {
        override val type = Meldingstype.SENDT_SØKNAD
    }

    data class InntektsmeldingDTO(
        val hendelseId: UUID,
        val beregnetInntekt: Number,
        val førsteFraværsdag: LocalDate,
        val mottattDato: LocalDateTime
    ): HendelseDTO() {
        override val type = Meldingstype.INNTEKTSMELDING
    }

    class UtbetalingshistorikkDTO() : HendelseDTO() {
        override val type = Meldingstype.YTELSER
    }
}
