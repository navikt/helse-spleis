package no.nav.helse.spleis.meldinger.model

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.properties.Delegates
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad

internal abstract class SøknadBuilder {
    protected lateinit var sykmeldingSkrevet: LocalDateTime
    protected lateinit var fødselsdato: LocalDate
    protected lateinit var behandlingsporing: Behandlingsporing.Yrkesaktivitet
    protected var registrert: LocalDateTime = LocalDateTime.now()
    protected lateinit var egenmeldinger: List<Periode>
    private lateinit var fom: LocalDate
    private lateinit var tom: LocalDate
    protected var permittert = false
    protected var arbeidUtenforNorge by Delegates.notNull<Boolean>()
    protected var yrkesskade by Delegates.notNull<Boolean>()
    protected var innsendt: LocalDateTime? = null
    protected var pensjonsgivendeInntekter: List<Søknad.PensjonsgivendeInntekt>? = null

    internal fun sykmeldingSkrevet(sykmeldingSkrevet: LocalDateTime) = apply { this.sykmeldingSkrevet = sykmeldingSkrevet }
    internal fun fødselsdato(fødselsdato: LocalDate) = apply { this.fødselsdato = fødselsdato }

    internal fun arbeidstaker(organisasjonsnummer: String) = apply { this.behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer) }
    internal fun selvstendig() = apply { this.behandlingsporing = Behandlingsporing.Yrkesaktivitet.Selvstendig }
    internal fun arbeidsledig() = apply { this.behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidsledig }
    internal fun frilans() = apply { this.behandlingsporing = Behandlingsporing.Yrkesaktivitet.Frilans }

    internal fun fom(fom: LocalDate) = apply { this.fom = fom }
    internal fun tom(tom: LocalDate) = apply { this.tom = tom }
    internal fun sendt(tidspunkt: LocalDateTime) = apply { this.innsendt = tidspunkt }
    internal fun permittert(permittert: Boolean) = apply { this.permittert = permittert }
    internal fun egenmeldinger(egenmeldinger: List<Periode>) = apply { this.egenmeldinger = egenmeldinger }
    internal fun arbeidUtenforNorge(arbeidUtenforNorge: Boolean) = apply { this.arbeidUtenforNorge = arbeidUtenforNorge }
    internal fun yrkesskade(yrkesskade: Boolean) = apply { this.yrkesskade = yrkesskade }
    internal fun fravær(type: String, fom: LocalDate, tom: LocalDate?) {
        when (type) {
            "PERMISJON" -> permisjon(fom, tom!!)
            "FERIE" -> ferie(fom, tom!!)
            "UTLANDSOPPHOLD" -> utlandsopphold(fom, tom!!)
        }
    }
    internal open fun inntektskilde(andreInntektskilder: Boolean) = apply {}

    internal abstract fun periode(fom: LocalDate, tom: LocalDate, grad: Int, arbeidshelse: Int?): SøknadBuilder

    internal open fun permisjon(fom: LocalDate, tom: LocalDate) = apply {}
    internal open fun ferie(fom: LocalDate, tom: LocalDate) = apply {}
    internal open fun utlandsopphold(fom: LocalDate, tom: LocalDate) = apply {}
    internal open fun merknader(type: String, beskrivelse: String?) = apply {}
    internal open fun papirsykmelding(fom: LocalDate, tom: LocalDate) = apply {}
    internal open fun arbeidsgjennopptatt(fom: LocalDate, tom: LocalDate) = apply { }
    internal open fun utenlandskSykmelding(utenlandsk: Boolean) = apply {}
    internal open fun sendTilGosys(tilGosys: Boolean) = apply {}

    internal fun arbeidsgjennopptatt(fom: LocalDate?) = apply {
        if (fom == null) return@apply
        arbeidsgjennopptatt(fom, this.tom)
    }
}
