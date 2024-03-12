package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.EndringIRefusjonDto
import no.nav.helse.dto.RefusjonDto
import no.nav.helse.dto.RefusjonshistorikkDto
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.person.RefusjonshistorikkVisitor
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.Companion.leggTilRefusjon
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.Companion.somOverlapperMedArbeidsgiverperiode
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.Companion.somTilstøterArbeidsgiverperiode
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.Companion.somTrefferFørsteFraværsdag
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.beløp
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.RefusjonsopplysningerBuilder
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
        private val meldingsreferanseId: UUID,
        private val førsteFraværsdag: LocalDate?,
        private val arbeidsgiverperioder: List<Periode>,
        private val beløp: Inntekt?,
        private val sisteRefusjonsdag: LocalDate?,
        private val endringerIRefusjon: List<EndringIRefusjon>,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) {
        private fun muligDuplikat(other: Refusjon) =
            this.meldingsreferanseId == other.meldingsreferanseId && this.utledetFørsteFraværsdag() == other.utledetFørsteFraværsdag()

        internal companion object {
            internal fun MutableList<Refusjon>.leggTilRefusjon(refusjon: Refusjon) {
                if (any { eksisterende -> eksisterende.muligDuplikat(refusjon) }) return
                add(refusjon)
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

            internal fun gjenopprett(dto: RefusjonDto): Refusjon {
                return Refusjon(
                    meldingsreferanseId = dto.meldingsreferanseId,
                    førsteFraværsdag = dto.førsteFraværsdag,
                    arbeidsgiverperioder = dto.arbeidsgiverperioder.map { Periode.gjenopprett(it) },
                    beløp = dto.beløp?.let { Inntekt.gjenopprett(it) },
                    sisteRefusjonsdag = dto.sisteRefusjonsdag,
                    endringerIRefusjon = dto.endringerIRefusjon.map { EndringIRefusjon.gjenopprett(it) },
                    tidsstempel = dto.tidsstempel
                )
            }
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
                internal fun Refusjonshistorikk.refusjonsopplysninger(skjæringstidspunkt: LocalDate, aktivitetslogg: IAktivitetslogg? = null): Refusjonsopplysninger {
                    val refusjonsopplysningBuilder = RefusjonsopplysningerBuilder()
                    val aktuelle = refusjoner.filter { it.startskuddet() >= skjæringstidspunkt }
                    val første = aktuelle.minByOrNull { it.startskuddet() }
                    if (første != null && første.startskuddet() != skjæringstidspunkt) {
                        refusjonsopplysningBuilder.leggTil(Refusjonsopplysning(
                            meldingsreferanseId = første.meldingsreferanseId,
                            fom = skjæringstidspunkt,
                            tom = første.startskuddet().forrigeDag,
                            beløp = første.beløp ?: INGEN,
                        ), første.tidsstempel)
                    }
                    aktuelle.leggTilRefusjonsopplysninger(refusjonsopplysningBuilder)
                    return refusjonsopplysningBuilder.build()
                }

                internal fun Refusjonshistorikk.erTom() = refusjoner.isEmpty()

                internal fun Refusjon.refusjonsopplysninger(): Refusjonsopplysninger {
                    val refusjonsopplysningBuilder = RefusjonsopplysningerBuilder()
                    leggTilRefusjoneropplysninger(refusjonsopplysningBuilder)
                    return refusjonsopplysningBuilder.build()
                }

                private fun List<Refusjon>.leggTilRefusjonsopplysninger(refusjonsopplysningerBuilder: RefusjonsopplysningerBuilder) =
                    forEach { it.leggTilRefusjoneropplysninger(refusjonsopplysningerBuilder) }

                private fun Refusjon.leggTilRefusjoneropplysninger(refusjonsopplysningerBuilder: RefusjonsopplysningerBuilder) {
                    val hovedRefusjonsopplysning = EndringIRefusjon(beløp ?: INGEN, startskuddet())

                    (endringerIRefusjon + hovedRefusjonsopplysning)
                        .forEach { endring ->
                            if (sisteRefusjonsdag != null && endring.endringsdato > sisteRefusjonsdag) return@forEach
                            else if (endring.endringsdato < startskuddet()) return@forEach
                            else refusjonsopplysningerBuilder.leggTil(Refusjonsopplysning(meldingsreferanseId, endring.endringsdato, sisteRefusjonsdag, endring.beløp), tidsstempel)
                        }

                    if (sisteRefusjonsdag == null) return
                    refusjonsopplysningerBuilder.leggTil(Refusjonsopplysning(meldingsreferanseId, sisteRefusjonsdag.nesteDag, null, INGEN), tidsstempel)
                }

                internal fun gjenopprett(dto: EndringIRefusjonDto): EndringIRefusjon {
                    return EndringIRefusjon(
                        beløp = Inntekt.gjenopprett(dto.beløp),
                        endringsdato = dto.endringsdato
                    )
                }
            }

            internal fun accept(visitor: RefusjonshistorikkVisitor) {
                visitor.visitEndringIRefusjon(beløp, endringsdato)
            }

            internal fun dto() = EndringIRefusjonDto(beløp.dtoMånedligDouble(), endringsdato)
        }

        internal fun dto() = RefusjonDto(
            meldingsreferanseId = meldingsreferanseId,
            førsteFraværsdag = førsteFraværsdag,
            arbeidsgiverperioder = arbeidsgiverperioder.map { it.dto() },
            beløp = beløp?.dtoMånedligDouble(),
            sisteRefusjonsdag = sisteRefusjonsdag,
            endringerIRefusjon = endringerIRefusjon.map { it.dto() },
            tidsstempel = tidsstempel
        )
    }

    internal fun dto() = RefusjonshistorikkDto(
        refusjoner = refusjoner.map { it.dto() }
    )

    internal companion object {
        fun gjenopprett(dto: RefusjonshistorikkDto): Refusjonshistorikk {
            return Refusjonshistorikk().apply {
                dto.refusjoner.forEach {
                    leggTilRefusjon(Refusjonshistorikk.Refusjon.gjenopprett(it))
                }
            }
        }
    }
}

