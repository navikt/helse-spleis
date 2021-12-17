package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Søknad

internal class NavSøknadBuilder : SendtSøknadBuilder() {

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
