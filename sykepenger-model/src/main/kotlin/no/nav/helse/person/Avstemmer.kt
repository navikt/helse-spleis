package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype

internal class Avstemmer(person: Person) {
    private val tilstander = mutableListOf<PersonVisitor>(Initiell())
    private val arbeidsgivere = mutableListOf<Map<String, Any>>()

    init {
        person.accept(DelegatedPersonVisitor(tilstander::first))
    }

    fun toMap(): Map<String, Any> = mapOf(
        "arbeidsgivere" to arbeidsgivere
    )

    private fun push(visitor: PersonVisitor) {
        tilstander.add(0, visitor)
    }

    private fun pop() {
        tilstander.removeAt(0)
    }

    inner class Initiell : PersonVisitor {
        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            push(HarArbeidsgiver())
        }
    }

    inner class HarArbeidsgiver : PersonVisitor {
        private val utbetalinger = mutableListOf<Map<String, Any>>()
        private val aktiveVedtaksperioder = mutableListOf<Map<String, Any>>()
        private val forkastedeVedtaksperioder = mutableListOf<Map<String, Any>>()

        override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
            push(HarUtbetalinger(this.utbetalinger))
        }

        override fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
            push(HarPerioder(forkastedeVedtaksperioder))
        }

        override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            push(HarPerioder(aktiveVedtaksperioder))
        }

        override fun postVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            arbeidsgivere.add(mapOf(
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperioder" to aktiveVedtaksperioder,
                "forkastedeVedtaksperioder" to forkastedeVedtaksperioder,
                "utbetalinger" to utbetalinger
            ))
            pop()
        }
    }

    private inner class HarUtbetalinger(private val utbetalinger: MutableList<Map<String, Any>>) : PersonVisitor {
        override fun preVisitUtbetaling(
            utbetaling: Utbetaling,
            id: UUID,
            korrelasjonsId: UUID,
            type: Utbetalingtype,
            tilstand: Utbetaling.Tilstand,
            periode: Periode,
            tidsstempel: LocalDateTime,
            oppdatert: LocalDateTime,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            maksdato: LocalDate,
            forbrukteSykedager: Int?,
            gjenståendeSykedager: Int?,
            stønadsdager: Int,
            beregningId: UUID,
            overføringstidspunkt: LocalDateTime?,
            avsluttet: LocalDateTime?,
            avstemmingsnøkkel: Long?
        ) {
            utbetalinger.add(mapOf(
                "id" to id,
                "type" to type.name,
                "status" to Utbetalingstatus.fraTilstand(tilstand),
                "opprettet" to tidsstempel,
                "oppdatert" to oppdatert
            ))
        }

        override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
            pop()
        }
    }

    private inner class HarPerioder(private val perioder: MutableList<Map<String, Any>>) : PersonVisitor {
        private val utbetalinger: MutableMap<UUID, MutableList<UUID>> = mutableMapOf()
        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            periodetype: () -> Periodetype,
            skjæringstidspunkt: () -> LocalDate,
            skjæringstidspunktFraInfotrygd: LocalDate?,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: Set<Dokumentsporing>,
            inntektsmeldingInfo: InntektsmeldingInfo?,
            inntektskilde: () -> Inntektskilde
        ) {
            push(HarVedtaksperiodeUtbetalinger(utbetalinger, id))
        }

        override fun postVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            periodetype: () -> Periodetype,
            skjæringstidspunkt: () -> LocalDate,
            skjæringstidspunktFraInfotrygd: LocalDate?,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: Set<Dokumentsporing>,
            inntektsmeldingInfo: InntektsmeldingInfo?,
            inntektskilde: () -> Inntektskilde
        ) {
            perioder.add(
                mapOf(
                    "id" to id,
                    "tilstand" to tilstand.type,
                    "opprettet" to opprettet,
                    "oppdatert" to oppdatert,
                    "utbetalinger" to (utbetalinger[id]?.toList() ?: emptyList())
                )
            )
        }

        override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            pop()
        }

        override fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
            pop()
        }
    }

    private inner class HarVedtaksperiodeUtbetalinger(
        private val utbetalinger: MutableMap<UUID, MutableList<UUID>>,
        private val vedtaksperiodeId: UUID
    ): PersonVisitor {

        override fun preVisitUtbetaling(
            utbetaling: Utbetaling,
            id: UUID,
            korrelasjonsId: UUID,
            type: Utbetalingtype,
            tilstand: Utbetaling.Tilstand,
            periode: Periode,
            tidsstempel: LocalDateTime,
            oppdatert: LocalDateTime,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            maksdato: LocalDate,
            forbrukteSykedager: Int?,
            gjenståendeSykedager: Int?,
            stønadsdager: Int,
            beregningId: UUID,
            overføringstidspunkt: LocalDateTime?,
            avsluttet: LocalDateTime?,
            avstemmingsnøkkel: Long?
        ) {
            if (tilstand != Utbetaling.Forkastet) utbetalinger.getOrPut(vedtaksperiodeId) { mutableListOf() }.add(id)
        }

        override fun postVisitVedtakserperiodeUtbetalinger(utbetalinger: List<Pair<VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement?, Utbetaling>>) {
            pop()
        }
    }
}
