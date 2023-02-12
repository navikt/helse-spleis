package no.nav.helse.spleis.meldinger.model

import java.time.LocalDate
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode

internal class NySøknadBuilder : SøknadBuilder() {
    private val sykemeldingsperioder = mutableListOf<Sykmeldingsperiode>()
    private var fremtidigSøknad = false

    override fun periode(fom: LocalDate, tom: LocalDate, grad: Int, arbeidshelse: Int?) = apply {
        sykemeldingsperioder.add(Sykmeldingsperiode(
            fom = fom,
            tom = tom
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
        sykeperioder = sykemeldingsperioder
    )
}
