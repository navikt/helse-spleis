package no.nav.helse.serde.api.dto

import java.time.LocalDate
import java.time.LocalDateTime

enum class HendelsetypeDto {
    NY_SØKNAD,
    NY_SØKNAD_FRILANS,
    SENDT_SØKNAD_NAV,
    SENDT_SØKNAD_FRILANS,
    SENDT_SØKNAD_ARBEIDSGIVER,
    INNTEKTSMELDING
}

data class HendelseDTO(
    val type: HendelsetypeDto,
    val id: String,
    val eksternDokumentId: String,

    // Inntektsmelding-spesifikk
    val mottattDato: LocalDateTime? = null,
    val beregnetInntekt: Double? = null,

    // Flex-søknad-spesifikk
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val rapportertdato: LocalDateTime? = null,
    val sendtNav: LocalDateTime? = null,
    val sendtArbeidsgiver: LocalDateTime? = null,
) {

    companion object {
        fun nySøknad(id: String, eksternDokumentId: String, fom: LocalDate, tom: LocalDate, rapportertdato: LocalDateTime) = HendelseDTO(
            type = HendelsetypeDto.NY_SØKNAD,
            id = id,
            eksternDokumentId = eksternDokumentId,
            fom = fom,
            tom = tom,
            rapportertdato = rapportertdato,
        )
        fun nyFrilanssøknad(id: String, eksternDokumentId: String, fom: LocalDate, tom: LocalDate, rapportertdato: LocalDateTime) = HendelseDTO(
            type = HendelsetypeDto.NY_SØKNAD_FRILANS,
            id = id,
            eksternDokumentId = eksternDokumentId,
            fom = fom,
            tom = tom,
            rapportertdato = rapportertdato,
        )
        fun sendtSøknadNav(id: String, eksternDokumentId: String, fom: LocalDate, tom: LocalDate, rapportertdato: LocalDateTime, sendtNav: LocalDateTime) = HendelseDTO(
            type = HendelsetypeDto.SENDT_SØKNAD_NAV,
            id = id,
            eksternDokumentId = eksternDokumentId,
            fom = fom,
            tom = tom,
            rapportertdato = rapportertdato,
            sendtNav = sendtNav,
        )
        fun sendtSøknadFrilans(id: String, eksternDokumentId: String, fom: LocalDate, tom: LocalDate, rapportertdato: LocalDateTime, sendtNav: LocalDateTime) = HendelseDTO(
            type = HendelsetypeDto.SENDT_SØKNAD_FRILANS,
            id = id,
            eksternDokumentId = eksternDokumentId,
            fom = fom,
            tom = tom,
            rapportertdato = rapportertdato,
            sendtNav = sendtNav,
        )
        fun sendtSøknadArbeidsgiver(id: String, eksternDokumentId: String, fom: LocalDate, tom: LocalDate, rapportertdato: LocalDateTime, sendtArbeidsgiver: LocalDateTime) = HendelseDTO(
            type = HendelsetypeDto.SENDT_SØKNAD_ARBEIDSGIVER,
            id = id,
            eksternDokumentId = eksternDokumentId,
            fom = fom,
            tom = tom,
            rapportertdato = rapportertdato,
            sendtArbeidsgiver = sendtArbeidsgiver,
        )
        fun inntektsmelding(id: String, eksternDokumentId: String, mottattDato: LocalDateTime, beregnetInntekt: Double) = HendelseDTO(
            type = HendelsetypeDto.INNTEKTSMELDING,
            id = id,
            eksternDokumentId = eksternDokumentId,
            mottattDato = mottattDato,
            beregnetInntekt = beregnetInntekt
        )
    }
}
