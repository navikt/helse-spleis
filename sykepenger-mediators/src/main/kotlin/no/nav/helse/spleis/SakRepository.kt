package no.nav.helse.spleis

import no.nav.helse.sak.Sak

internal interface SakRepository {

    fun hentSak(aktørId: String): Sak?

}
