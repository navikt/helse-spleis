package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.aktive
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class Utbetalingkladd(
    private val periode: Periode,
    private val arbeidsgiveroppdrag: Oppdrag,
    private val personoppdrag: Oppdrag
) {
    internal fun overlapperMed(dato: LocalDate)  = dato in periode
    fun lagUtbetaling(
        type: Utbetaling.Utbetalingtype,
        korrelerendeUtbetaling: Utbetaling?,
        beregningId: UUID,
        utbetalingstidslinje: Utbetalingstidslinje,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int
    ): Utbetaling {
        return Utbetaling(
            beregningId = beregningId,
            korrelerendeUtbetaling = korrelerendeUtbetaling,
            periode = periode,
            utbetalingstidslinje = utbetalingstidslinje.kutt(periode.endInclusive),
            arbeidsgiverOppdrag = arbeidsgiveroppdrag,
            personOppdrag = personoppdrag,
            type = type,
            maksdato = maksdato,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager
        )
    }

    internal fun forrigeUtbetalte(utbetalinger: List<Utbetaling>): List<Utbetaling> {
        // fordi arbeidsgiverperioder kan slås sammen eller splittes opp så kan det hende vi finner
        // mer enn én utbetaling som dekker perioden vår
        return utbetalinger.aktive(periode)
    }

    internal fun opphører(other: Periode) = this.periode.endInclusive < other.endInclusive
    internal fun arbeidsgiveroppdrag(other: Oppdrag, aktivitetslogg: IAktivitetslogg) =
        this.arbeidsgiveroppdrag.minus(other, aktivitetslogg)

    internal fun personoppdrag(other: Oppdrag, aktivitetslogg: IAktivitetslogg) =
        this.personoppdrag.minus(other, aktivitetslogg)

    internal fun begrensTil(sisteDato: LocalDate): Utbetalingkladd {
        return Utbetalingkladd(
            periode = periode.subset(LocalDate.MIN til sisteDato),
            arbeidsgiveroppdrag = this.arbeidsgiveroppdrag.begrensTil(sisteDato),
            personoppdrag = this.personoppdrag.begrensTil(sisteDato)
        )
    }

    // hensyntar bare endringer frem til og med angitt dato
    internal fun begrensTil(aktivitetslogg: IAktivitetslogg, sisteDato: LocalDate, tidligereArbeidsgiveroppdrag: Oppdrag, tidligerePersonoppdrag: Oppdrag): Utbetalingkladd {
        return begrensTil(sisteDato)
            .diffMotTidligere(aktivitetslogg, tidligereArbeidsgiveroppdrag, tidligerePersonoppdrag)
    }

    // hensyntar bare endringer frem til og med angitt dato,
    // og kopierer evt. senere linjer fra tidligere oppdrag
    internal fun begrensTilOgKopier(
        aktivitetslogg: IAktivitetslogg,
        sisteDato: LocalDate,
        tidligereArbeidsgiveroppdrag: Oppdrag,
        tidligerePersonoppdrag: Oppdrag
    ): Utbetalingkladd {
        return begrensTil(sisteDato)
            .kopierArbeidsgiveroppdrag(tidligereArbeidsgiveroppdrag)
            .kopierPersonoppdrag(tidligerePersonoppdrag)
            .diffMotTidligere(aktivitetslogg, tidligereArbeidsgiveroppdrag, tidligerePersonoppdrag)
    }

    private fun diffMotTidligere(aktivitetslogg: IAktivitetslogg, tidligereArbeidsgiveroppdrag: Oppdrag, tidligerePersonoppdrag: Oppdrag) =
        Utbetalingkladd(
            periode = this.periode,
            arbeidsgiveroppdrag = this.arbeidsgiveroppdrag.minus(tidligereArbeidsgiveroppdrag, aktivitetslogg),
            personoppdrag = this.personoppdrag.minus(tidligerePersonoppdrag, aktivitetslogg)
        )

    private fun kopierArbeidsgiveroppdrag(tidligereArbeidsgiveroppdrag: Oppdrag) =
        Utbetalingkladd(
            periode = this.periode,
            arbeidsgiveroppdrag = this.arbeidsgiveroppdrag + tidligereArbeidsgiveroppdrag.begrensFra(this.periode.endInclusive.nesteDag),
            personoppdrag = this.personoppdrag
        )

    private fun kopierPersonoppdrag(tidligerePersonoppdrag: Oppdrag) =
        Utbetalingkladd(
            periode = this.periode,
            arbeidsgiveroppdrag = this.arbeidsgiveroppdrag,
            personoppdrag = this.personoppdrag + tidligerePersonoppdrag.begrensFra(this.periode.endInclusive.nesteDag)
        )
}