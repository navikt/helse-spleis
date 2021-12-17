package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate

internal class ArbeidsgiverSøknadBuilder : SøknadBuilder() {
    private val sykdomsperioder = mutableListOf<SøknadArbeidsgiver.Sykdom>()
    private val arbeidsperiode = mutableListOf<SøknadArbeidsgiver.Arbeid>()

    override fun utdanning(fom: LocalDate) = apply {}

    override fun permisjon(fom: LocalDate, tom: LocalDate) = apply {}

    override fun ferie(fom: LocalDate, tom: LocalDate) = apply {}

    override fun utlandsopphold(fom: LocalDate, tom: LocalDate) = apply {}

    override fun merknader(type: String, beskrivelse: String) = apply {}

    override fun papirsykmelding(fom: LocalDate, tom: LocalDate) = apply {}

    override fun egenmelding(fom: LocalDate, tom: LocalDate) = apply {}

    override fun arbeidsgjennopptatt(fom: LocalDate, tom: LocalDate) = apply {
        arbeidsperiode.add(SøknadArbeidsgiver.Arbeid(fom, tom))
    }

    override fun periode(fom: LocalDate, tom: LocalDate, grad: Int, arbeidshelse: Int?) = apply {
        sykdomsperioder.add(SøknadArbeidsgiver.Sykdom(fom = fom, tom = tom, sykmeldingsgrad = grad.prosent, arbeidshelse = arbeidshelse?.prosent))
    }

    internal fun build() = SøknadArbeidsgiver(
        meldingsreferanseId = meldingsreferanseId,
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = organisasjonsnummer,
        sykdomsperioder = sykdomsperioder,
        arbeidsperiode = arbeidsperiode,
        sykmeldingSkrevet = sykmeldingSkrevet
    )
}
