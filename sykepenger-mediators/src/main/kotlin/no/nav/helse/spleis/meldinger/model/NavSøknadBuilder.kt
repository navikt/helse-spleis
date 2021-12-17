package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Søknad
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate
import java.time.LocalDateTime

internal class NavSøknadBuilder : SøknadBuilder() {
    private val perioder = mutableListOf<Søknad.Søknadsperiode>()
    private val merkander = mutableListOf<Søknad.Merknad>()
    private val inntektskilder = mutableListOf<Søknad.Inntektskilde>()
    private var permittert = false
    private lateinit var innsendt: LocalDateTime

    internal fun sendt(tidspunkt: LocalDateTime) = apply {
        this.innsendt = tidspunkt
    }

    internal fun inntektskilde(sykmeldt: Boolean, type: String) {
        inntektskilder.add(Søknad.Inntektskilde(sykmeldt = sykmeldt, type = type))
    }

    internal fun permittert(permittert: Boolean) = apply {
        this.permittert = permittert
    }

    internal fun fravær(type: String, fom: LocalDate, tom: LocalDate?) {
        when (type) {
            "UTDANNING_FULLTID", "UTDANNING_DELTID" -> utdanning(fom)
            "PERMISJON" -> permisjon(fom, tom!!)
            "FERIE" -> ferie(fom, tom!!)
            "UTLANDSOPPHOLD" -> utlandsopphold(fom, tom!!)
        }
    }

    private fun utdanning(fom: LocalDate) = apply {
        perioder.add(Søknad.Søknadsperiode.Utdanning(fom, this.tom))
    }

    private fun permisjon(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Søknad.Søknadsperiode.Permisjon(fom, tom))
    }

    private fun ferie(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Søknad.Søknadsperiode.Ferie(fom, tom))
    }

    private fun utlandsopphold(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Søknad.Søknadsperiode.Utlandsopphold(fom, tom))
    }

    internal fun merknader(type: String, beskrivelse: String) = apply {
        merkander.add(Søknad.Merknad(type, beskrivelse))
    }

    internal fun papirsykmelding(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Søknad.Søknadsperiode.Papirsykmelding(fom = fom, tom = tom))
    }

    internal fun egenmelding(fom: LocalDate, tom: LocalDate) {
        perioder.add(Søknad.Søknadsperiode.Egenmelding(fom = fom, tom = tom))
    }

    internal fun arbeidsgjennopptatt(fom: LocalDate?) = apply {
        if (fom == null) return@apply
        perioder.add(Søknad.Søknadsperiode.Arbeid(fom, this.tom))
    }

    override fun periode(fom: LocalDate, tom: LocalDate, grad: Int, arbeidshelse: Int?) = apply {
        perioder.add(Søknad.Søknadsperiode.Sykdom(
            fom = fom,
            tom = tom,
            sykmeldingsgrad = grad.prosent,
            arbeidshelse = arbeidshelse?.prosent
        ))
    }

    internal fun build() = Søknad(
        meldingsreferanseId = meldingsreferanseId,
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = organisasjonsnummer,
        perioder = perioder,
        andreInntektskilder = inntektskilder,
        sendtTilNAV = innsendt,
        permittert = permittert,
        merknaderFraSykmelding = merkander,
        sykmeldingSkrevet = sykmeldingSkrevet
    )
}
