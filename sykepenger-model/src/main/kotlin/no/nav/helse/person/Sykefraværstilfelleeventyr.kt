package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykefraværstilfelleeventyrObserver.SykefraværstilfelleeventyrObserverEvent
import no.nav.helse.person.SykefraværstilfelleeventyrObserver.SykefraværstilfelleeventyrObserverEvent.SykefraværstilfelleperiodeObserverEvent

internal class Sykefraværstilfelleeventyr(private val dato: LocalDate, private val perioder: List<Triple<UUID, String, Periode>>) {
    constructor(dato: LocalDate) : this(dato, emptyList())

    internal fun bliMed(id: UUID, orgnr: String, periode: Periode): Sykefraværstilfelleeventyr {
        return Sykefraværstilfelleeventyr(this.dato, this.perioder.plus(Triple(id, orgnr, periode)))
    }

    private fun tilObserverEvent() =
        SykefraværstilfelleeventyrObserverEvent(
            this.dato,
            this.perioder.map { (id, orgnr, periode) ->
                SykefraværstilfelleperiodeObserverEvent(id, orgnr, periode.start, periode.endInclusive)
            }
        )

    internal companion object {
        fun List<Sykefraværstilfelleeventyr>.bliMed(vedtaksperiodeId: UUID, organisasjonsnummer: String, periode: Periode): List<Sykefraværstilfelleeventyr> = with(sortedBy { it.dato }) {
            val riktig = this.indexOfLast { eventyr -> eventyr.dato <= periode.endInclusive }
            if (riktig == -1) return@with listOf(Sykefraværstilfelleeventyr(periode.start, listOf(Triple(vedtaksperiodeId, organisasjonsnummer, periode)))) + this
            val snute = this.take(riktig)
            val hale = this.drop(riktig + 1)
            return snute + this[riktig].bliMed(vedtaksperiodeId, organisasjonsnummer, periode) + hale
        }

        internal fun List<Sykefraværstilfelleeventyr>.varsleObservers(observers: List<SykefraværstilfelleeventyrObserver>) {
            val event = sortedBy { it.dato }.map { it.tilObserverEvent() }
            observers.forEach { it.sykefraværstilfelle(event) }
        }
    }
}

interface SykefraværstilfelleeventyrObserver {
    fun sykefraværstilfelle(sykefraværstilfeller: List<SykefraværstilfelleeventyrObserverEvent>) {}

    data class SykefraværstilfelleeventyrObserverEvent(
        val dato: LocalDate,
        val perioder: List<SykefraværstilfelleperiodeObserverEvent>
    ) {

        data class SykefraværstilfelleperiodeObserverEvent(
            val id: UUID,
            val organisasjonsnummer: String,
            val fom: LocalDate,
            val tom: LocalDate
        )
    }
}