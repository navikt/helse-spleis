package no.nav.helse.serde.migration

import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til

internal class V209SpissetKildeSykmeldingTilSøknad: KildeSykmeldingTilSøknad(209) {
    override fun perioderSomSkalEndres() = perioderSomSkalEndres

    private companion object {
        private val perioderSomSkalEndres = mapOf(
            "2020-08-26".dato.somPeriode() to "a9c78fe9-150e-4bfb-b9e2-fe1c65eb1ee8".uuid, // Søknad rett før
            "2020-03-07".dato til "2020-03-22".dato to "d908e9b1-2f40-484a-8456-9d54f62278f5".uuid, // Søknad rett før med type "FORELDET_SYKEDAG"
            "2020-03-07".dato til "2020-03-13".dato to "57765497-eb3d-4c18-8c6b-817265c0334b".uuid, // Søknad rett før med type ""FORELDET_SYKEDAG"
            // Periode splittet med type "FORELDET_SYKEDAG" som har søknadId, men sykmelding før og etter
            "2020-03-13".dato til "2020-03-18".dato to "a65c7f5e-5520-4b98-85c0-3b90129ea5d7".uuid,
            "2020-03-20".dato til "2020-03-27".dato to "a65c7f5e-5520-4b98-85c0-3b90129ea5d7".uuid,
            // Periode splittet med type "SYKEDAG" som har søknadId, men sykmelding før og etter
            "2020-04-21".dato til "2020-04-22".dato to "a131222d-6857-4ab4-9b26-966bdfc3e03a".uuid,
            "2020-04-24".dato til "2020-04-28".dato to "a131222d-6857-4ab4-9b26-966bdfc3e03a".uuid,
            "2020-03-19".dato til "2020-03-27".dato to "08225f36-1c76-46c5-a6d7-333228ea9e35".uuid // Søknad rett før med type ""FORELDET_SYKEDAG"
        )
    }
}