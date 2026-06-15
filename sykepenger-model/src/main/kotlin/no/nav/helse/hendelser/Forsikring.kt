package no.nav.helse.hendelser

import no.nav.helse.økonomi.Prosentdel

sealed interface Forsikring {
    fun dekningsgrad(): Prosentdel
    fun navOvertarAnsvarForVentetid(): Boolean
}
