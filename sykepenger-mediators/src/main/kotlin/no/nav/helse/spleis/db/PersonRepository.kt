package no.nav.helse.spleis.db

import no.nav.helse.person.Person
import java.util.*

internal interface PersonRepository {
    fun hentPerson(fødselsnummer: String): Person?
    fun hentVedtaksperiodeIderMedTilstand(personId: Long): List<VedtaksperiodeIdTilstand>
    fun hentNyestePersonId(fødselsnummer: String): Long?
    fun hentPerson(id: Long): Person
    fun markerSomTilbakerullet(fødselsnummer: String, tilbakeTilId: Long): Int
}

data class VedtaksperiodeIdTilstand(val id: UUID, val tilstand: String)
