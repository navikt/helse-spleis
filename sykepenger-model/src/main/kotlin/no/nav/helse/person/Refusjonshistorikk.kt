package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Refusjonshistorikk.Refusjon.Companion.leggTilRefusjon
import no.nav.helse.person.Refusjonshistorikk.Refusjon.Companion.overlapperMedArbeidsgiverperiode
import no.nav.helse.person.Refusjonshistorikk.Refusjon.Companion.refusjonSomTrefferFørsteFraværsdag
import no.nav.helse.person.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.beløp
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Refusjonshistorikk {
    private val refusjoner = mutableListOf<Refusjon>()

    internal fun leggTilRefusjon(refusjon: Refusjon) {
        refusjoner.leggTilRefusjon(refusjon)
    }

    internal fun accept(visitor: RefusjonshistorikkVisitor) {
        visitor.preVisitRefusjonshistorikk(this)
        refusjoner.forEach { it.accept(visitor) }
        visitor.postVisitRefusjonshistorikk(this)
    }

    internal fun finnRefusjon(periode: Periode): Refusjon? {
        return refusjoner.refusjonSomTrefferFørsteFraværsdag(periode) ?: refusjoner.overlapperMedArbeidsgiverperiode(periode)
    }

    internal class Refusjon(
        private val meldingsreferanseId: UUID,
        private val førsteFraværsdag: LocalDate?,
        private val arbeidsgiverperioder: List<Periode>,
        private val beløp: Inntekt?,
        private val sisteRefusjonsdag: LocalDate?,
        private val endringerIRefusjon: List<EndringIRefusjon>,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) {
        internal companion object {
            internal fun MutableList<Refusjon>.leggTilRefusjon(refusjon: Refusjon) {
                if (refusjon.meldingsreferanseId !in map { it.meldingsreferanseId }) add(refusjon)
            }
            internal fun Iterable<Refusjon>.overlapperMedArbeidsgiverperiode(periode: Periode) : Refusjon? {
                val utvidetPeriode = periode.start.minusDays(16) til periode.endInclusive
                return firstOrNull { refusjon ->
                    refusjon.arbeidsgiverperioder.any { it.overlapperMed(utvidetPeriode) }
                }
            }

            private fun Refusjon.utledetFørsteFraværsdag() = førsteFraværsdag ?: arbeidsgiverperioder.maxOf { it.start }

            internal fun Iterable<Refusjon>.refusjonSomTrefferFørsteFraværsdag(periode: Periode) = firstOrNull { refusjon ->
                refusjon.utledetFørsteFraværsdag() in periode
            }

        }

        internal fun beløp(dag: LocalDate, aktivitetslogg: IAktivitetslogg) : Inntekt {
            if (dag < utledetFørsteFraværsdag()) {
                aktivitetslogg.severe("Har ikke opplysninger om refusjon på den aktuelle dagen")
            }
            if (sisteRefusjonsdag != null && dag > sisteRefusjonsdag) return Inntekt.INGEN
            return endringerIRefusjon.beløp(dag) ?: beløp ?: Inntekt.INGEN
        }

        internal fun accept(visitor: RefusjonshistorikkVisitor) {
            visitor.preVisitRefusjon(
                meldingsreferanseId,
                førsteFraværsdag,
                arbeidsgiverperioder,
                beløp,
                sisteRefusjonsdag,
                endringerIRefusjon,
                tidsstempel
            )
            endringerIRefusjon.forEach { it.accept(visitor) }
            visitor.postVisitRefusjon(
                meldingsreferanseId,
                førsteFraværsdag,
                arbeidsgiverperioder,
                beløp,
                sisteRefusjonsdag,
                endringerIRefusjon,
                tidsstempel
            )
        }

        internal class EndringIRefusjon(
            private val beløp: Inntekt,
            private val endringsdato: LocalDate
        ){
            internal companion object {
                internal fun List<EndringIRefusjon>.beløp(dag: LocalDate) = sortedBy { it.endringsdato }.lastOrNull { dag >= it.endringsdato }?.beløp
            }

            internal fun accept(visitor: RefusjonshistorikkVisitor){
                visitor.visitEndringIRefusjon(beløp, endringsdato)
            }
        }
    }
}
