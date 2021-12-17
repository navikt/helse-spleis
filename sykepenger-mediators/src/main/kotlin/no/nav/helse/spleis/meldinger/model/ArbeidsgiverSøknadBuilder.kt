package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.SøknadArbeidsgiver

internal class ArbeidsgiverSøknadBuilder : SendtSøknadBuilder() {
    internal fun build() = SøknadArbeidsgiver(
        meldingsreferanseId = meldingsreferanseId,
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = organisasjonsnummer,
        perioder = perioder,
        andreInntektskilder = inntektskilder,
        sendtTilArbeidsgiver = innsendt!!,
        permittert = permittert,
        merknaderFraSykmelding = merkander,
        sykmeldingSkrevet = sykmeldingSkrevet
    )
}
