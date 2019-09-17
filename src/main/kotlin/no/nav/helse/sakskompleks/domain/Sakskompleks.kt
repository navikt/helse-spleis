package no.nav.helse.sakskompleks.domain

import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.sykmelding.domain.gjelderTil
import no.nav.helse.søknad.domain.Sykepengesøknad
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import java.lang.RuntimeException
import java.time.LocalDate
import java.util.UUID

data class Sakskompleks(
    val id: UUID,
    val aktørId: String,
    val sykmeldinger: List<Sykmelding> = emptyList(),
    val søknader: List<Sykepengesøknad> = emptyList(),
    val inntektsmeldinger: List<Inntektsmelding> = emptyList()
)

fun Sakskompleks.fom() : LocalDate? = run {
    val syketilfelleStart = sykmeldinger.mapNotNull { sykmelding -> sykmelding.syketilfelleStartDato }.min()
    val tidligsteFOM: LocalDate? =
        sykmeldinger.flatMap { sykmelding -> sykmelding.perioder }.map { periode -> periode.fom }.min()
    val søknadEgenmelding =
        søknader.flatMap { søknad -> søknad.egenmeldinger }.map { egenmelding -> egenmelding.fom }.min()

    return listOfNotNull(syketilfelleStart, tidligsteFOM, søknadEgenmelding).min()
}

fun Sakskompleks.tom(): LocalDate = run {
    val arbeidGjenopptatt = søknader.somIkkeErKorrigerte().maxBy { søknad -> søknad.tom }?.arbeidGjenopptatt
    val sisteTOMSøknad = søknader.maxBy { søknad -> søknad.tom }?.tom
    val sisteTOMSykmelding = sykmeldinger.maxBy { sykmelding ->  sykmelding.gjelderTil() }?.gjelderTil()

    return arbeidGjenopptatt
        ?: listOfNotNull(sisteTOMSøknad, sisteTOMSykmelding).max() ?: throw RuntimeException("Et sakskompleks må ha en sluttdato!")
}

fun List<Sykepengesøknad>.somIkkeErKorrigerte(): List<Sykepengesøknad> {
    val korrigerteIder = mapNotNull { it.korrigerer }
    return filter { it.id !in korrigerteIder }
}
