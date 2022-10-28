package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.person.Refusjonshistorikk.Refusjon.Companion.leggTilRefusjon
import no.nav.helse.person.Refusjonshistorikk.Refusjon.Companion.somOverlapperMedArbeidsgiverperiode
import no.nav.helse.person.Refusjonshistorikk.Refusjon.Companion.somTilstøterArbeidsgiverperiode
import no.nav.helse.person.Refusjonshistorikk.Refusjon.Companion.somTrefferFørsteFraværsdag
import no.nav.helse.person.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.beløp
import no.nav.helse.person.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN

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

    internal fun finnRefusjon(periode: Periode, aktivitetslogg: IAktivitetslogg): Refusjon? {
        return refusjoner.somTrefferFørsteFraværsdag(periode, aktivitetslogg)
            ?: refusjoner.somTilstøterArbeidsgiverperiode(periode, aktivitetslogg)
            ?: refusjoner.somOverlapperMedArbeidsgiverperiode(periode, aktivitetslogg)
    }

    internal class Refusjon(
        internal val meldingsreferanseId: UUID,
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

            private fun Refusjon.utledetFørsteFraværsdag() = førsteFraværsdag ?: arbeidsgiverperioder.maxOf { it.start }

            private fun Iterable<Refusjon>.nyesteMedFørsteFraværsdagFørFørsteUtbetalingsdag(førsteUtbetalingsdag: LocalDate) =
                filter { it.utledetFørsteFraværsdag() < førsteUtbetalingsdag }.maxByOrNull { it.tidsstempel }

            internal fun Iterable<Refusjon>.somOverlapperMedArbeidsgiverperiode(periode: Periode, aktivitetslogg: IAktivitetslogg): Refusjon? {
                val utvidetPeriode = periode.start.minusDays(16) til periode.endInclusive
                return filter { refusjon ->
                    refusjon.arbeidsgiverperioder.any { it.overlapperMed(utvidetPeriode) }
                }.nyesteMedFørsteFraværsdagFørFørsteUtbetalingsdag(periode.start)
                    ?.also { aktivitetslogg.info("Fant refusjon ved å gå 16 dager tilbake fra første utbetalingsdag i sammenhengende utbetaling") }
            }

            internal fun Iterable<Refusjon>.somTrefferFørsteFraværsdag(periode: Periode, aktivitetslogg: IAktivitetslogg) = filter { refusjon ->
                refusjon.utledetFørsteFraværsdag() in periode
            }.maxByOrNull { it.tidsstempel }?.also { aktivitetslogg.info("Fant refusjon ved å sjekke om første fraværsdag er i sammenhengende utbetaling") }

            internal fun Iterable<Refusjon>.somTilstøterArbeidsgiverperiode(periode: Periode, aktivitetslogg: IAktivitetslogg) = filter { refusjon ->
                refusjon.arbeidsgiverperioder.maxByOrNull { it.endInclusive }?.erRettFør(periode) ?: false
            }.nyesteMedFørsteFraværsdagFørFørsteUtbetalingsdag(periode.start)
                ?.also { aktivitetslogg.info("Fant refusjon ved å finne tilstøtende arbeidsgiverperiode for første utbetalingsdag i sammenhengende utbetaling") }
        }

        internal fun beløp(dag: LocalDate): Inntekt {
            if (sisteRefusjonsdag != null && dag > sisteRefusjonsdag) return INGEN
            return endringerIRefusjon.beløp(dag) ?: beløp ?: INGEN
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
        ) {
            internal companion object {
                internal fun List<EndringIRefusjon>.beløp(dag: LocalDate) = sortedBy { it.endringsdato }.lastOrNull { dag >= it.endringsdato }?.beløp

                private fun Refusjon.startskuddet(): LocalDate {
                    if (førsteFraværsdag == null) return arbeidsgiverperioder.maxOf { it.start }
                    return arbeidsgiverperioder.map { it.start }.plus(førsteFraværsdag).max()
                }
                internal fun Refusjonshistorikk.refusjonsopplysninger(skjæringstidspunkt: LocalDate): Refusjonsopplysninger {
                    val refusjonsopplysninger =
                        this.refusjoner.filter { it.startskuddet() >= skjæringstidspunkt }.sortedBy { it.startskuddet() }
                            .tilRefusjonsopplysninger()

                    return Refusjonsopplysninger(refusjonsopplysninger)
                }

                private fun List<Refusjon>.tilRefusjonsopplysninger() =
                    flatMap { it.tilRefusjoneropplysninger() }

                private fun Refusjon.tilRefusjoneropplysninger(): List<Refusjonsopplysning> {
                    val hovedRefusjonsopplysning = EndringIRefusjon(
                        beløp ?: INGEN, startskuddet()
                    )
                    val alleRefusjonsopplysninger = (endringerIRefusjon + hovedRefusjonsopplysning)
                        .sortedBy { it.endringsdato }
                        .mapNotNull { endring ->
                            if (sisteRefusjonsdag != null && endring.endringsdato > sisteRefusjonsdag) null
                            else if (endring.endringsdato < startskuddet()) null
                            else Refusjonsopplysning(meldingsreferanseId, endring.endringsdato, sisteRefusjonsdag, endring.beløp)
                        }

                    return when(sisteRefusjonsdag) {
                        null -> alleRefusjonsopplysninger
                        else -> alleRefusjonsopplysninger + Refusjonsopplysning(meldingsreferanseId, sisteRefusjonsdag.nesteDag, null, INGEN)
                    }
                }
            }

            internal fun accept(visitor: RefusjonshistorikkVisitor) {
                visitor.visitEndringIRefusjon(beløp, endringsdato)
            }

        }
    }
}

