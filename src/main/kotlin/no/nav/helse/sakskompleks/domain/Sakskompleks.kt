package no.nav.helse.sakskompleks.domain

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.sykmelding.domain.gjelderTil
import no.nav.helse.søknad.domain.Sykepengesøknad
import java.time.LocalDate
import java.util.*

data class Sakskompleks(
    val id: UUID,
    val aktørId: String,
    val sykmeldinger: MutableList<Sykmelding> = mutableListOf(),
    val søknader: MutableList<Sykepengesøknad> = mutableListOf(),
    val inntektsmeldinger: MutableList<Inntektsmelding> = mutableListOf(),
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    var tilstand: Sakskomplekstilstand = StartTilstand()
) {

    fun leggTil(søknad: Sykepengesøknad) {
        with(tilstand) { søknadMottatt() }
        søknader.add(søknad)
    }

    fun leggTil(inntektsmelding: Inntektsmelding) {
        with(tilstand) { inntektsmeldingMottatt() }
        inntektsmeldinger.add(inntektsmelding)
    }

    fun leggTil(sykmelding: Sykmelding) {
        with(tilstand) { sykmeldingMottatt() }
        sykmeldinger.add(sykmelding)
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

class StartTilstand: Sakskomplekstilstand() {
    override fun Sakskompleks.sykmeldingMottatt() {
       tilstand = SykmeldingMottattTilstand()
    }
}

class SykmeldingMottattTilstand: Sakskomplekstilstand() {
    override fun Sakskompleks.søknadMottatt() {
        tilstand = SøknadMottattTilstand()
    }

    override fun Sakskompleks.inntektsmeldingMottatt() {
        tilstand = InntektsmeldingMottattTilstand()
    }
}

class SøknadMottattTilstand: Sakskomplekstilstand() {
    override fun Sakskompleks.inntektsmeldingMottatt() {
        tilstand = KomplettSakTilstand()
    }
}

class InntektsmeldingMottattTilstand: Sakskomplekstilstand() {
    override fun Sakskompleks.søknadMottatt() {
        tilstand = KomplettSakTilstand()
    }
}

class KomplettSakTilstand: Sakskomplekstilstand()

abstract class Sakskomplekstilstand {

    open fun Sakskompleks.sykmeldingMottatt() {
        throw IllegalStateException()
    }

    open fun Sakskompleks.søknadMottatt() {
        throw IllegalStateException()
    }

    open fun Sakskompleks.inntektsmeldingMottatt() {
        throw IllegalStateException()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

}

fun List<Sykepengesøknad>.somIkkeErKorrigerte(): List<Sykepengesøknad> {
    val korrigerteIder = mapNotNull { it.korrigerer }
    return filter { it.id !in korrigerteIder }
}
