package no.nav.helse.person.builders

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.OppdragVisitor
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse.*
import no.nav.helse.person.PersonObserver.UtbetaltEvent.IkkeUtbetaltDag.Type
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class UtbetaltEventBuilder(
    private val hendelseIder: Set<UUID>,
    private val periode: Periode,
    vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
) {
    private val sykepengegrunnlag = vilkårsgrunnlag.sykepengegrunnlag()
    private val inntekt = vilkårsgrunnlag.grunnlagForSykepengegrunnlag()
    private val dagsats = sykepengegrunnlag.reflection { _, _, _, daglig -> daglig }

    private var forbrukteSykedager: Int = -1
    private var gjenståendeSykedager: Int = -1
    private var automatiskBehandling: Boolean = false
    private lateinit var godkjentAv: String

    private lateinit var maksdato: LocalDate
    private lateinit var utbetalingId: UUID

    private lateinit var opprettet: LocalDateTime
    private val oppdragListe = mutableListOf<PersonObserver.UtbetaltEvent.Utbetalt>()
    private val ikkeUtbetalteDager = mutableListOf<PersonObserver.UtbetaltEvent.IkkeUtbetaltDag>()

    internal fun utbetalingId(id: UUID) = apply { this.utbetalingId = id }
    internal fun utbetalingOpprettet(tidspunkt: LocalDateTime) = apply { this.opprettet = tidspunkt }
    internal fun oppdrag(vararg oppdrag: Oppdrag) = apply {
        oppdrag.forEach { oppdragListe.add(OppdragUtbetaltBuilder(dagsats, it).result()) }
    }
    internal fun utbetalingstidslinje(utbetalingstidslinje: Utbetalingstidslinje) = apply {
        utbetalingstidslinje.avgrensSisteArbeidsgiverperiode(periode).also {
            ikkeUtbetalteDager.addAll(IkkeUtbetalteDagerBuilder(it).result())
        }
    }

    internal fun forbrukteSykedager(antall: Int) = apply { this.forbrukteSykedager = antall }
    internal fun gjenståendeSykedager(antall: Int) = apply { this.gjenståendeSykedager = antall }
    internal fun maksdato(dato: LocalDate) = apply { this.maksdato = dato }
    internal fun godkjentAv(ident: String) = apply { this.godkjentAv = ident }
    internal fun automatiskBehandling(svar: Boolean) = apply { this.automatiskBehandling = svar }

    fun result(): PersonObserver.UtbetaltEvent {
        check(forbrukteSykedager != -1 && gjenståendeSykedager != -1) { "builderen er ikke komplett" }
        return PersonObserver.UtbetaltEvent(
            hendelser = hendelseIder,
            utbetalingId = utbetalingId,
            oppdrag = oppdragListe.toList(),
            ikkeUtbetalteDager = ikkeUtbetalteDager,
            fom = periode.start,
            tom = periode.endInclusive,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            godkjentAv = godkjentAv,
            automatiskBehandling = automatiskBehandling,
            opprettet = opprettet,
            sykepengegrunnlag = sykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
            månedsinntekt = inntekt.reflection { _, månedlig, _, _ -> månedlig },
            maksdato = maksdato
        )
    }

    private class OppdragUtbetaltBuilder(private val dagsats: Int, oppdrag: Oppdrag) : OppdragVisitor {
        private val utbetalingslinjer = mutableListOf<PersonObserver.UtbetaltEvent.Utbetalt.Utbetalingslinje>()
        private lateinit var result: PersonObserver.UtbetaltEvent.Utbetalt

        init {
            oppdrag.accept(this)
        }

        internal fun result() = result

        override fun postVisitOppdrag(
            oppdrag: Oppdrag,
            fagområde: Fagområde,
            fagsystemId: String,
            mottaker: String,
            førstedato: LocalDate,
            sistedato: LocalDate,
            sisteArbeidsgiverdag: LocalDate?,
            stønadsdager: Int,
            totalBeløp: Int,
            nettoBeløp: Int,
            tidsstempel: LocalDateTime,
            endringskode: Endringskode,
            avstemmingsnøkkel: Long?,
            status: Oppdragstatus?,
            overføringstidspunkt: LocalDateTime?,
            erSimulert: Boolean,
            simuleringsResultat: Simulering.SimuleringResultat?
        ) {
            result = PersonObserver.UtbetaltEvent.Utbetalt(
                mottaker = mottaker,
                fagområde = fagområde.verdi,
                fagsystemId = fagsystemId,
                totalbeløp = totalBeløp,
                utbetalingslinjer = utbetalingslinjer.toList()
            )
        }

        override fun visitUtbetalingslinje(
            linje: Utbetalingslinje,
            fom: LocalDate,
            tom: LocalDate,
            stønadsdager: Int,
            totalbeløp: Int,
            satstype: Satstype,
            beløp: Int?,
            aktuellDagsinntekt: Int?,
            grad: Int?,
            delytelseId: Int,
            refDelytelseId: Int?,
            refFagsystemId: String?,
            endringskode: Endringskode,
            datoStatusFom: LocalDate?,
            statuskode: String?,
            klassekode: Klassekode
        ) {
            if (linje.erOpphør()) return
            utbetalingslinjer.add(
                PersonObserver.UtbetaltEvent.Utbetalt.Utbetalingslinje(
                    fom = fom,
                    tom = tom,
                    sats = dagsats,
                    beløp = beløp!!,
                    grad = grad!!,
                    sykedager = stønadsdager
                )
            )
        }
    }

    private class IkkeUtbetalteDagerBuilder(utbetalingstidslinje: Utbetalingstidslinje) : UtbetalingsdagVisitor {
        private val førsteSykedag = utbetalingstidslinje.sykepengeperiode()?.start ?: LocalDate.MIN
        private val ikkeUtbetalteDager = mutableListOf<PersonObserver.UtbetaltEvent.IkkeUtbetaltDag>()

        init {
            utbetalingstidslinje.accept(this)
        }

        fun result() = ikkeUtbetalteDager

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag, dato: LocalDate, økonomi: Økonomi) {
            ikkeUtbetalteDager.add(PersonObserver.UtbetaltEvent.IkkeUtbetaltDag(dato, Type.AvvistDag, mapBegrunnelser(dag.begrunnelser)))
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag, dato: LocalDate, økonomi: Økonomi) {
            ikkeUtbetalteDager.add(PersonObserver.UtbetaltEvent.IkkeUtbetaltDag(dato, Type.Fridag))
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag, dato: LocalDate, økonomi: Økonomi) {
            if (dato < førsteSykedag) return
            ikkeUtbetalteDager.add(PersonObserver.UtbetaltEvent.IkkeUtbetaltDag(dato, Type.Arbeidsdag))
        }

        private fun mapBegrunnelser(begrunnelser: List<Begrunnelse>): List<PersonObserver.UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse> = begrunnelser.map {
            when (it) {
                Begrunnelse.SykepengedagerOppbrukt -> SykepengedagerOppbrukt
                Begrunnelse.SykepengedagerOppbruktOver67 -> SykepengedagerOppbruktOver67
                Begrunnelse.MinimumInntekt -> MinimumInntekt
                Begrunnelse.MinimumInntektOver67 -> MinimumInntektOver67
                Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode -> EgenmeldingUtenforArbeidsgiverperiode
                Begrunnelse.MinimumSykdomsgrad -> MinimumSykdomsgrad
                Begrunnelse.EtterDødsdato -> EtterDødsdato
                Begrunnelse.ManglerMedlemskap -> ManglerMedlemskap
                Begrunnelse.ManglerOpptjening -> ManglerOpptjening
                Begrunnelse.Over70 -> Over70
                Begrunnelse.NyVilkårsprøvingNødvendig -> SykepengedagerOppbrukt // TODO: Map til NyVilkårsprøvingNødvendig
            }
        }
    }
}
