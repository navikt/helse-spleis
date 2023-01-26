package no.nav.helse.person.aktivitetslogg

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Varselkode

interface AktivitetsloggObserver {
    fun aktivitet(id: UUID, label: Char, melding: String, kontekster: List<SpesifikkKontekst>, tidsstempel: LocalDateTime)
    fun varsel(id: UUID, label: Char, kode: Varselkode?, melding: String, kontekster: List<SpesifikkKontekst>, tidsstempel: LocalDateTime)
}