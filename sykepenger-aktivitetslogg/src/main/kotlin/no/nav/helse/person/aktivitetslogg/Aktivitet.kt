package no.nav.helse.person.aktivitetslogg

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

sealed class Aktivitet(
    val id: UUID,
    private val alvorlighetsgrad: Int,
    val label: Char,
    val melding: String,
    val tidsstempel: LocalDateTime,
    val kontekster: List<SpesifikkKontekst>
) : Comparable<Aktivitet> {
    private companion object {
        private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }

    override fun compareTo(other: Aktivitet) = this.tidsstempel.compareTo(other.tidsstempel)
        .let { if (it == 0) other.alvorlighetsgrad.compareTo(this.alvorlighetsgrad) else it }

    override fun toString() = label + "  \t" + tidsstempel.format(tidsstempelformat) + "  \t" + melding + meldingerString()

    private fun meldingerString(): String {
        return kontekster.joinToString(separator = "") { " (${it.melding()})" }
    }

    operator fun contains(kontekst: Aktivitetskontekst) = kontekst.toSpesifikkKontekst() in kontekster
    class Info private constructor(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        melding: String,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Aktivitet(id, 0, 'I', melding, tidsstempel, kontekster) {
        companion object {
            internal fun opprett(kontekster: List<SpesifikkKontekst>, melding: String) =
                Info(UUID.randomUUID(), kontekster, melding)
        }
    }

    class Varsel private constructor(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        val kode: Varselkode,
        melding: String,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Aktivitet(id, 25, 'W', melding, tidsstempel, kontekster) {
        companion object {
            internal fun opprett(kontekster: List<SpesifikkKontekst>, kode: Varselkode, melding: String) =
                Varsel(UUID.randomUUID(), kontekster, kode, melding = melding)
        }
    }

    class FunksjonellFeil private constructor(
        id: UUID,
        kontekster: List<SpesifikkKontekst>,
        val kode: Varselkode,
        melding: String,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Aktivitet(id, 75, 'E', melding, tidsstempel, kontekster) {
        companion object {
            internal fun opprett(kontekster: List<SpesifikkKontekst>, kode: Varselkode, melding: String) =
                FunksjonellFeil(UUID.randomUUID(), kontekster, kode, melding)
        }
    }
}
