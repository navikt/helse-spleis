package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.AvsenderDto
import no.nav.helse.dto.MeldingsreferanseDto

enum class Avsender {
    SYKMELDT, ARBEIDSGIVER, SAKSBEHANDLER, SYSTEM;

    fun dto() = when (this) {
        SYKMELDT -> AvsenderDto.SYKMELDT
        ARBEIDSGIVER -> AvsenderDto.ARBEIDSGIVER
        SAKSBEHANDLER -> AvsenderDto.SAKSBEHANDLER
        SYSTEM -> AvsenderDto.SYSTEM
    }

    companion object {
        fun gjenopprett(dto: AvsenderDto): Avsender {
            return when (dto) {
                AvsenderDto.ARBEIDSGIVER -> ARBEIDSGIVER
                AvsenderDto.SAKSBEHANDLER -> SAKSBEHANDLER
                AvsenderDto.SYKMELDT -> SYKMELDT
                AvsenderDto.SYSTEM -> SYSTEM
            }
        }
    }
}

sealed interface Hendelse {
    val behandlingsporing: Behandlingsporing
    val metadata: HendelseMetadata
}

sealed interface Behandlingsporing {
    data object IngenArbeidsgiver : Behandlingsporing
    data class Arbeidsgiver(val organisasjonsnummer: String) : Behandlingsporing
}

// en value-class for uuid-er som representerer @id til en melding fra kafka
@JvmInline
value class MeldingsreferanseId(val id: UUID) {
    fun dto() = MeldingsreferanseDto(id)

    companion object {
        fun gjenopprett(dto: MeldingsreferanseDto) = MeldingsreferanseId(dto.id)
    }
}

data class HendelseMetadata(
    val meldingsreferanseId: MeldingsreferanseId,
    val avsender: Avsender,

    // tidspunktet meldingen ble registrert (lest inn) av fagsystemet
    val registrert: LocalDateTime,

    // tidspunktet for når meldingen ble sendt inn av avsender.
    // kan være når bruker sendte søknaden sin, eller arbeidsgiver sendte inntektsmelding.
    val innsendt: LocalDateTime,

    // sann hvis et system har sendt meldingen på eget initiativ
    val automatiskBehandling: Boolean
)
