package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Søknad
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate

internal class NavSøknadBuilder : SøknadBuilder() {
    private val perioder = mutableListOf<Søknad.Søknadsperiode>()
    private val merkander = mutableListOf<Søknad.Merknad>()
    private val inntektskilder = mutableListOf<Søknad.Inntektskilde>()

    override fun inntektskilde(sykmeldt: Boolean, type: String) = apply {
        inntektskilder.add(Søknad.Inntektskilde(sykmeldt = sykmeldt, type = type))
    }

    override fun utdanning(fom: LocalDate) = apply {
        perioder.add(Søknad.Søknadsperiode.Utdanning(fom, this.tom))
    }

    override fun permisjon(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Søknad.Søknadsperiode.Permisjon(fom, tom))
    }

    override fun ferie(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Søknad.Søknadsperiode.Ferie(fom, tom))
    }

    override fun utlandsopphold(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Søknad.Søknadsperiode.Utlandsopphold(fom, tom))
    }

    override fun merknader(type: String, beskrivelse: String) = apply {
        merkander.add(Søknad.Merknad(type, beskrivelse))
    }

    override fun papirsykmelding(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Søknad.Søknadsperiode.Papirsykmelding(fom = fom, tom = tom))
    }

    override fun egenmelding(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Søknad.Søknadsperiode.Egenmelding(fom = fom, tom = tom))
    }

    override fun arbeidsgjennopptatt(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Søknad.Søknadsperiode.Arbeid(fom, tom))
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
        sendtTilNAV = innsendt!!,
        permittert = permittert,
        merknaderFraSykmelding = merkander,
        sykmeldingSkrevet = sykmeldingSkrevet
    )
}
