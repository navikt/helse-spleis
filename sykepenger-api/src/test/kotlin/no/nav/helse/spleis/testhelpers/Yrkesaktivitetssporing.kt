package no.nav.helse.spleis.testhelpers

import no.nav.helse.hendelser.Behandlingsporing

internal val Behandlingsporing.Yrkesaktivitet.somOrganisasjonsnummer
    get() = when (this) {
        Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> "ARBEIDSLEDIG"
        is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> organisasjonsnummer
        Behandlingsporing.Yrkesaktivitet.Frilans -> "FRILANS"
        Behandlingsporing.Yrkesaktivitet.Selvstendig -> "SELVSTENDIG"
        Behandlingsporing.Yrkesaktivitet.SelvstendigJordbruker -> "SELVSTENDIG_JORDBRUKER"
        Behandlingsporing.Yrkesaktivitet.SelvstendigFisker -> "SELVSTENDIG_FISKER"
        Behandlingsporing.Yrkesaktivitet.SelvstendigBarnepasser -> "SELVSTENDIG_BARNEPASSER"
    }
