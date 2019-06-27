package no.nav.helse.sakskompleks

import no.nav.helse.sakskompleks.domain.Sakskompleks

class SakskompleksDao(private val saker: List<Sakskompleks>) {

    fun finnSaker(aktørId: String) =
            saker.filter { sak ->
                sak.aktørId == aktørId
            }
}
