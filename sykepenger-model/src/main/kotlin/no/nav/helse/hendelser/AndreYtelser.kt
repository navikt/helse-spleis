package no.nav.helse.hendelser

import no.nav.helse.økonomi.Prosentdel

data class AndreYtelser(val perioder: List<PeriodeMedAnnenYtelse>) {
    data class PeriodeMedAnnenYtelse(val ytelse: String, val periode: Periode, val prosent: Prosentdel)
}
