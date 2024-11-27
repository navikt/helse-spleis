package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.spleis.Meldingsporing
import java.time.LocalDate

internal class NySøknadBuilder : SøknadBuilder() {
    private val sykemeldingsperioder = mutableListOf<Sykmeldingsperiode>()
    private var fremtidigSøknad = false

    override fun periode(
        fom: LocalDate,
        tom: LocalDate,
        grad: Int,
        arbeidshelse: Int?,
    ) = apply {
        sykemeldingsperioder.add(
            Sykmeldingsperiode(
                fom = fom,
                tom = tom,
            ),
        )
    }

    internal fun fremtidigSøknad(erFremtidig: Boolean) {
        fremtidigSøknad = erFremtidig
    }

    internal fun build(meldingsporing: Meldingsporing) =
        Sykmelding(
            meldingsreferanseId = meldingsporing.id,
            orgnummer = organisasjonsnummer,
            sykeperioder = sykemeldingsperioder,
        )
}
