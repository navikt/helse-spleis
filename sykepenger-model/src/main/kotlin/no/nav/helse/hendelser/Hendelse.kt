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
    data object IngenYrkesaktivitet : Behandlingsporing

    sealed interface Yrkesaktivitet : Behandlingsporing {
        data class Arbeidstaker(val organisasjonsnummer: String) : Yrkesaktivitet
        data object Selvstendig : Yrkesaktivitet
        data object Frilans : Yrkesaktivitet
        data object Arbeidsledig : Yrkesaktivitet

        val Yrkesaktivitet.somOrganisasjonsnummer
            get() = when (this) {
                Arbeidsledig -> "ARBEIDSLEDIG"
                is Arbeidstaker -> organisasjonsnummer
                Frilans -> "FRILANS"
                Selvstendig -> "SELVSTENDIG"
            }

    }
}

fun Behandlingsporing.erLik(other: Behandlingsporing) = when (this) {
    Behandlingsporing.IngenYrkesaktivitet -> other is Behandlingsporing.IngenYrkesaktivitet
    Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> other is Behandlingsporing.Yrkesaktivitet.Arbeidsledig
    is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> other is Behandlingsporing.Yrkesaktivitet.Arbeidstaker && this.organisasjonsnummer == other.organisasjonsnummer
    Behandlingsporing.Yrkesaktivitet.Frilans -> other is Behandlingsporing.Yrkesaktivitet.Frilans
    Behandlingsporing.Yrkesaktivitet.Selvstendig -> other is Behandlingsporing.Yrkesaktivitet.Selvstendig
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
