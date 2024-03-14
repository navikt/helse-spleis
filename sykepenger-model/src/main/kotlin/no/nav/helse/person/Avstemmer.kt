package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.utbetalingslinjer.Utbetalingstatus

internal class Avstemmer(person: Person) {
    private val tilstander = mutableListOf<PersonVisitor>(Initiell())
    private val arbeidsgivere = mutableListOf<Map<String, Any>>()

    init {
        person.accept(DelegatedPersonVisitor { tilstander.first() })
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
        private var vurdering: Map<String, Any>? = null
        override fun preVisitUtbetaling(
            utbetaling: Utbetaling,
            id: UUID,
            korrelasjonsId: UUID,
            type: Utbetalingtype,
            utbetalingstatus: Utbetalingstatus,
            periode: Periode,
            tidsstempel: LocalDateTime,
            oppdatert: LocalDateTime,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            maksdato: LocalDate,
            forbrukteSykedager: Int?,
            gjenståendeSykedager: Int?,
            stønadsdager: Int,
            overføringstidspunkt: LocalDateTime?,
            avsluttet: LocalDateTime?,
            avstemmingsnøkkel: Long?,
            annulleringer: Set<UUID>
        ) {
        }

        override fun visitVurdering(
            vurdering: Utbetaling.Vurdering,
            ident: String,
            epost: String,
            tidspunkt: LocalDateTime,
            automatiskBehandling: Boolean,
            godkjent: Boolean
        ) {
            this.vurdering = mapOf(
                "ident" to ident,
                "tidspunkt" to tidspunkt,
                "automatiskBehandling" to automatiskBehandling,
                "godkjent" to godkjent
            )
        }

        override fun postVisitUtbetaling(
            utbetaling: Utbetaling,
            id: UUID,
            korrelasjonsId: UUID,
            type: Utbetalingtype,
            tilstand: Utbetalingstatus,
            periode: Periode,
            tidsstempel: LocalDateTime,
            oppdatert: LocalDateTime,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            maksdato: LocalDate,
            forbrukteSykedager: Int?,
            gjenståendeSykedager: Int?,
            stønadsdager: Int,
            overføringstidspunkt: LocalDateTime?,
            avsluttet: LocalDateTime?,
            avstemmingsnøkkel: Long?,
            annulleringer: Set<UUID>
        ) {
            utbetalinger.add(
                mutableMapOf<String, Any>(
                    "id" to id,
                    "korrelasjonsId" to korrelasjonsId,
                    "type" to type.name,
                    "status" to tilstand,
                    "opprettet" to tidsstempel,
                    "oppdatert" to oppdatert,
                ).apply {
                    compute("avsluttet") {_, _ -> avsluttet }
                    compute("vurdering") {_, _ -> vurdering }
                }
            )
            vurdering = null
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
            skjæringstidspunkt: () -> LocalDate,
            hendelseIder: Set<Dokumentsporing>
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
            skjæringstidspunkt: () -> LocalDate,
            hendelseIder: Set<Dokumentsporing>
        ) {
            perioder.add(
                mapOf(
                    "id" to id,
                    "tilstand" to tilstand.type,
                    "opprettet" to opprettet,
                    "oppdatert" to oppdatert,
                    "fom" to periode.start,
                    "tom" to periode.endInclusive,
                    "skjæringstidspunkt" to skjæringstidspunkt(),
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
            utbetalingstatus: Utbetalingstatus,
            periode: Periode,
            tidsstempel: LocalDateTime,
            oppdatert: LocalDateTime,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            maksdato: LocalDate,
            forbrukteSykedager: Int?,
            gjenståendeSykedager: Int?,
            stønadsdager: Int,
            overføringstidspunkt: LocalDateTime?,
            avsluttet: LocalDateTime?,
            avstemmingsnøkkel: Long?,
            annulleringer: Set<UUID>
        ) {
            if (utbetalingstatus != Utbetalingstatus.FORKASTET) utbetalinger.getOrPut(vedtaksperiodeId) { mutableListOf() }.add(id)
        }

        override fun postVisitGenerasjoner(generasjoner: List<Generasjoner.Generasjon>) {
            pop()
        }
    }
}
