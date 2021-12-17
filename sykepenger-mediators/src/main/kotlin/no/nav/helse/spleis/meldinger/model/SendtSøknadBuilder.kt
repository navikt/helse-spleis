package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.SendtSøknad.*
import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate

internal abstract class SendtSøknadBuilder : SøknadBuilder() {
    protected val perioder = mutableListOf<Søknadsperiode>()
    protected val merkander = mutableListOf<Merknad>()
    protected val inntektskilder = mutableListOf<Inntektskilde>()

    override fun inntektskilde(sykmeldt: Boolean, type: String) = apply {
        inntektskilder.add(Inntektskilde(sykmeldt = sykmeldt, type = type))
    }

    override fun utdanning(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Utdanning(fom, tom))
    }

    override fun permisjon(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Permisjon(fom, tom))
    }

    override fun ferie(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Ferie(fom, tom))
    }

    override fun utlandsopphold(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Utlandsopphold(fom, tom))
    }

    override fun merknader(type: String, beskrivelse: String?) = apply {
        merkander.add(Merknad(type, beskrivelse))
    }

    override fun papirsykmelding(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Papirsykmelding(fom = fom, tom = tom))
    }

    override fun egenmelding(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Egenmelding(fom = fom, tom = tom))
    }

    override fun arbeidsgjennopptatt(fom: LocalDate, tom: LocalDate) = apply {
        perioder.add(Arbeid(fom, tom))
    }

    override fun periode(fom: LocalDate, tom: LocalDate, grad: Int, arbeidshelse: Int?) = apply {
        perioder.add(
            Sykdom(
                fom = fom,
                tom = tom,
                sykmeldingsgrad = grad.prosent,
                arbeidshelse = arbeidshelse?.prosent
            )
        )
    }
}
