package no.nav.helse

import no.nav.helse.hendelser.Behandlingsporing

internal val Behandlingsporing.Yrkesaktivitet.somOrganisasjonsnummer
    get() = when (this) {
        Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> "ARBEIDSLEDIG"
        is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> organisasjonsnummer
        Behandlingsporing.Yrkesaktivitet.Frilans -> "FRILANS"
        Behandlingsporing.Yrkesaktivitet.Selvstendig -> "SELVSTENDIG"
    }
