package no.nav.helse.sakskompleks.domain

import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.sakskompleks.*
import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.sykmelding.domain.gjelderTil
import no.nav.helse.søknad.domain.Sykepengesøknad
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.time.LocalDate
import java.util.UUID

class Sakskompleks(
        val id: UUID,
        val aktørId: String,
        private val sykmeldinger: MutableList<Sykmelding> = mutableListOf(),
        private val søknader: MutableList<Sykepengesøknad> = mutableListOf(),
        private val inntektsmeldinger: MutableList<Inntektsmelding> = mutableListOf()
) {
    var tilstand: Sakskomplekstilstand = SykmeldingMottattTilstand()

    fun leggerTil(søknad: Sykepengesøknad) {
        tilstand.søknadMottatt()
        søknader.add(søknad)
    }

    fun leggerTil(inntektsmelding: Inntektsmelding) {
        inntektsmeldinger.add(inntektsmelding)
    }

    fun leggerTil(sykmelding: Sykmelding) {
        sykmeldinger.add(sykmelding)
    }

    internal inner class SykmeldingMottattTilstand: Sakskomplekstilstand {
        override fun søknadMottatt() {
            tilstand = SøknadMottattTilstand()
        }
    }

    internal inner class SøknadMottattTilstand: Sakskomplekstilstand {

    }
    fun fom() : LocalDate? = run {
        val syketilfelleStart = sykmeldinger.mapNotNull { sykmelding -> sykmelding.syketilfelleStartDato }.min()
        val tidligsteFOM: LocalDate? =
                sykmeldinger.flatMap { sykmelding -> sykmelding.perioder }.map { periode -> periode.fom }.min()
        val søknadEgenmelding =
                søknader.flatMap { søknad -> søknad.egenmeldinger }.map { egenmelding -> egenmelding.fom }.min()

        return listOfNotNull(syketilfelleStart, tidligsteFOM, søknadEgenmelding).min()
    }

    fun tom(): LocalDate = run {
        val arbeidGjenopptatt = søknader.somIkkeErKorrigerte().maxBy { søknad -> søknad.tom }?.arbeidGjenopptatt
        val sisteTOMSøknad = søknader.maxBy { søknad -> søknad.tom }?.tom
        val sisteTOMSykmelding = sykmeldinger.maxBy { sykmelding ->  sykmelding.gjelderTil() }?.gjelderTil()

        return arbeidGjenopptatt
                ?: listOfNotNull(sisteTOMSøknad, sisteTOMSykmelding).max() ?: throw RuntimeException("Et sakskompleks må ha en sluttdato!")
    }

    fun hørerSammenMed(sykepengesøknad: Sykepengesøknad) =
            sykmeldinger.any { sykmelding ->
                sykmelding.id == sykepengesøknad.sykmeldingId
            }

    fun har(sykmelding: Sykmelding) =
            sykmeldinger.any { enSykmelding ->
                sykmelding == enSykmelding
            }

    fun har(sykepengesøknad: Sykepengesøknad) =
            søknader.any { enSøknad ->
                sykepengesøknad == enSøknad
            }

    fun har(inntektsmelding: Inntektsmelding) =
            inntektsmeldinger.any { enInntektsmelding ->
                inntektsmelding == enInntektsmelding
            }
}

interface Sakskomplekstilstand {

    fun søknadMottatt() {
        throw IllegalStateException()
    }
    fun inntektsmeldingMottatt() {
        throw IllegalStateException()
    }
}

fun List<Sykepengesøknad>.somIkkeErKorrigerte(): List<Sykepengesøknad> {
    val korrigerteIder = mapNotNull { it.korrigerer }
    return filter { it.id !in korrigerteIder }
}
