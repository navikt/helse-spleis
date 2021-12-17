package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.SendtSøknad
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate

internal abstract class SendtSøknadBuilder : SøknadBuilder() {
    protected val perioder = mutableListOf<SendtSøknad.Søknadsperiode>()
    protected val merkander = mutableListOf<SendtSøknad.Merknad>()
    protected val inntektskilder = mutableListOf<SendtSøknad.Inntektskilde>()

    override fun inntektskilde(sykmeldt: Boolean, type: String) = apply {
        inntektskilder.add(SendtSøknad.Inntektskilde(sykmeldt = sykmeldt, type = type))
    }

    override fun utdanning(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(SendtSendtSøknad.Søknadsperiode.Utdanning(fom, tom))
    }

    override fun permisjon(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(SendtSendtSøknad.Søknadsperiode.Permisjon(fom, tom))
    }

    override fun ferie(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(SendtSendtSøknad.Søknadsperiode.Ferie(fom, tom))
    }

    override fun utlandsopphold(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(SendtSendtSøknad.Søknadsperiode.Utlandsopphold(fom, tom))
    }

    override fun merknader(type: String, beskrivelse: String) = apply {
        merkander.add(SendtSøknad.Merknad(type, beskrivelse))
    }

    override fun papirsykmelding(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(SendtSendtSøknad.Søknadsperiode.Papirsykmelding(fom = fom, tom = tom))
    }

    override fun egenmelding(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(SendtSendtSøknad.Søknadsperiode.Egenmelding(fom = fom, tom = tom))
    }

    override fun arbeidsgjennopptatt(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(SendtSendtSøknad.Søknadsperiode.Arbeid(fom, tom))
    }

    override fun periode(fom: LocalDate, tom: LocalDate, grad: Int, arbeidshelse: Int?) = apply {
        perioder.add(SendtSendtSøknad.Søknadsperiode.Sykdom(
            fom = fom,
            tom = tom,
            sykmeldingsgrad = grad.prosent,
            arbeidshelse = arbeidshelse?.prosent
        ))
    }
}
