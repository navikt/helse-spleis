package no.nav.helse.spleis.dto

import no.nav.helse.person.Person
import no.nav.helse.serde.api.PersonDTO
import no.nav.helse.serde.api.hendelseReferanserForPerson
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.serde.api.v2.InntektsmeldingDTO
import no.nav.helse.serde.api.v2.SykmeldingDTO
import no.nav.helse.serde.api.v2.SøknadArbeidsgiverDTO
import no.nav.helse.serde.api.v2.SøknadNavDTO
import no.nav.helse.spleis.HendelseDTO
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.objectMapper


internal fun håndterPerson(person: Person, hendelseDao: HendelseDao): PersonDTO {
    val hendelseReferanser = hendelseReferanserForPerson(person)
    val hendelser = hendelseDao.hentHendelser(hendelseReferanser).map { (type, hendelseJson) ->
        when (type) {
            HendelseDao.Meldingstype.NY_SØKNAD -> HendelseDTO.NySøknadDTO(objectMapper.readTree(hendelseJson))
            HendelseDao.Meldingstype.SENDT_SØKNAD_NAV -> HendelseDTO.SendtSøknadNavDTO(objectMapper.readTree(hendelseJson))
            HendelseDao.Meldingstype.SENDT_SØKNAD_ARBEIDSGIVER -> HendelseDTO.SendtSøknadArbeidsgiverDTO(objectMapper.readTree(hendelseJson))
            HendelseDao.Meldingstype.INNTEKTSMELDING -> HendelseDTO.InntektsmeldingDTO(objectMapper.readTree(hendelseJson))
        }
    }.mapHendelseDTO()
    return serializePersonForSpeil(person, hendelser)
}

internal fun List<HendelseDTO>.mapHendelseDTO() = map {
    when (it) {
        is HendelseDTO.NySøknadDTO -> mapNySøknad(it)
        is HendelseDTO.SendtSøknadNavDTO -> mapSendtSøknad(it)
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

internal fun mapInntektsmelding(inntektsmeldingDTO: HendelseDTO.InntektsmeldingDTO) = InntektsmeldingDTO(
    inntektsmeldingDTO.hendelseId,
    inntektsmeldingDTO.mottattDato,
    inntektsmeldingDTO.beregnetInntekt.toDouble()
)
