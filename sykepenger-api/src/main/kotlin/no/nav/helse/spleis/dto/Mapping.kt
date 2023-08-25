package no.nav.helse.spleis.dto

import no.nav.helse.person.Person
import no.nav.helse.serde.api.dto.InntektsmeldingDTO
import no.nav.helse.serde.api.dto.PersonDTO
import no.nav.helse.serde.api.dto.SykmeldingDTO
import no.nav.helse.serde.api.dto.SykmeldingFrilansDTO
import no.nav.helse.serde.api.dto.SøknadArbeidsgiverDTO
import no.nav.helse.serde.api.dto.SøknadFrilansDTO
import no.nav.helse.serde.api.dto.SøknadNavDTO
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.spleis.HendelseDTO
import no.nav.helse.spleis.dao.HendelseDao


internal fun håndterPerson(fødselsnummer: Long, person: Person, hendelseDao: HendelseDao): PersonDTO {
    val hendelser = hendelseDao.hentHendelser(fødselsnummer).mapHendelseDTO()
    return serializePersonForSpeil(person, hendelser)
}

internal fun List<HendelseDTO>.mapHendelseDTO() = map {
    when (it) {
        is HendelseDTO.NySøknadDTO -> mapNySøknad(it)
        is HendelseDTO.NyFrilansSøknadDTO -> mapNyFrilansSøknad(it)
        is HendelseDTO.SendtSøknadNavDTO -> mapSendtSøknad(it)
        is HendelseDTO.SendtSøknadFrilansDTO -> mapSendtSøknadFrilans(it)
        is HendelseDTO.SendtSøknadArbeidsgiverDTO -> mapSendtSøknad(it)
        is HendelseDTO.InntektsmeldingDTO -> mapInntektsmelding(it)
    }
}

internal fun mapSendtSøknad(sendtSøknadNavDTO: HendelseDTO.SendtSøknadNavDTO) = SøknadNavDTO(
    sendtSøknadNavDTO.hendelseId,
    sendtSøknadNavDTO.fom,
    sendtSøknadNavDTO.tom,
    sendtSøknadNavDTO.rapportertdato,
    sendtSøknadNavDTO.sendtNav
)
internal fun mapSendtSøknadFrilans(sendtSøknadNavDTO: HendelseDTO.SendtSøknadFrilansDTO) = SøknadFrilansDTO(
    sendtSøknadNavDTO.hendelseId,
    sendtSøknadNavDTO.fom,
    sendtSøknadNavDTO.tom,
    sendtSøknadNavDTO.rapportertdato,
    sendtSøknadNavDTO.sendtNav
)

internal fun mapSendtSøknad(sendtSøknadArbeidsgiverDTO: HendelseDTO.SendtSøknadArbeidsgiverDTO) = SøknadArbeidsgiverDTO(
    sendtSøknadArbeidsgiverDTO.hendelseId,
    sendtSøknadArbeidsgiverDTO.fom,
    sendtSøknadArbeidsgiverDTO.tom,
    sendtSøknadArbeidsgiverDTO.rapportertdato,
    sendtSøknadArbeidsgiverDTO.sendtArbeidsgiver
)

internal fun mapNySøknad(nySøknadDTO: HendelseDTO.NySøknadDTO) = SykmeldingDTO(
    nySøknadDTO.hendelseId,
    nySøknadDTO.fom,
    nySøknadDTO.tom,
    nySøknadDTO.rapportertdato
)
internal fun mapNyFrilansSøknad(nySøknadDTO: HendelseDTO.NyFrilansSøknadDTO) = SykmeldingFrilansDTO(
    nySøknadDTO.hendelseId,
    nySøknadDTO.fom,
    nySøknadDTO.tom,
    nySøknadDTO.rapportertdato
)

internal fun mapInntektsmelding(inntektsmeldingDTO: HendelseDTO.InntektsmeldingDTO) = InntektsmeldingDTO(
    inntektsmeldingDTO.hendelseId,
    inntektsmeldingDTO.mottattDato,
    inntektsmeldingDTO.beregnetInntekt.toDouble()
)
