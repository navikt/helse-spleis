package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate

internal class NySøknadBuilder : SøknadBuilder() {
    private val sykemeldingsperioder = mutableListOf<Sykmeldingsperiode>()
    private var fremtidigSøknad = false

    override fun periode(fom: LocalDate, tom: LocalDate, grad: Int, arbeidshelse: Int?) = apply {
        sykemeldingsperioder.add(Sykmeldingsperiode(
            fom = fom,
            tom = tom,
            grad = grad.prosent
        ))
    }

    internal fun fremtidigSøknad(erFremtidig: Boolean) {
        fremtidigSøknad = erFremtidig
    }

    internal fun build() = Sykmelding(
        meldingsreferanseId = meldingsreferanseId,
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = organisasjonsnummer,
        sykeperioder = sykemeldingsperioder,
        sykmeldingSkrevet = sykmeldingSkrevet,
        mottatt = opprettet,
        erFremtidig = fremtidigSøknad
    )
}
