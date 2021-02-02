package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal interface PersonVisitor : ArbeidsgiverVisitor, AktivitetsloggVisitor {
    fun preVisitPerson(person: Person, opprettet: LocalDateTime, aktørId: String, fødselsnummer: String) {}
    fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
    fun preVisitArbeidsgivere() {}
    fun postVisitArbeidsgivere() {}
    fun postVisitPerson(person: Person, opprettet: LocalDateTime, aktørId: String, fødselsnummer: String) {}
}

internal interface ArbeidsgiverVisitor : InntekthistorikkVisitor, SykdomshistorikkVisitor, VedtaksperiodeVisitor, UtbetalingVisitor {
    fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
    }

    fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {}
    fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {}
    fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {}
    fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {}
    fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {}
    fun preVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak){}
    fun postVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak){}
    fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {}
    fun postVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
    }

}

internal interface VedtaksperiodeVisitor : UtbetalingVisitor, SykdomstidslinjeVisitor, UtbetalingsdagVisitor {
    fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        hendelseIder: List<UUID>
    ) {
    }

    fun visitSkjæringstidspunkt(skjæringstidspunkt: LocalDate) {}
    fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?) {}
    fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {}
    fun visitForlengelseFraInfotrygd(forlengelseFraInfotrygd: ForlengelseFraInfotrygd) {}
    fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode
    ) {
    }
}

internal interface UtbetalingsdagVisitor {
    fun preVisit(tidslinje: Utbetalingstidslinje) {}
    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.Fridag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun visit(
        dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
    }

    fun postVisit(tidslinje: Utbetalingstidslinje) {}
}

internal interface SykdomshistorikkVisitor : SykdomstidslinjeVisitor {
    fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {}
    fun preVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        hendelseId: UUID?,
        tidsstempel: LocalDateTime
    ) {
    }

    fun preVisitHendelseSykdomstidslinje(
        tidslinje: Sykdomstidslinje,
        hendelseId: UUID?,
        tidsstempel: LocalDateTime
    ) {
    }

    fun postVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    fun postVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
    fun postVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        hendelseId: UUID?,
        tidsstempel: LocalDateTime
    ) {
    }

    fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {}
}

internal interface SykdomstidslinjeVisitor {
    fun preVisitSykdomstidslinje(
        tidslinje: Sykdomstidslinje,
        låstePerioder: List<Periode>
    ) {
    }

    fun visitDag(dag: UkjentDag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(
        dag: Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(dag: Feriedag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(
        dag: ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(
        dag: Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(
        dag: ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(
        dag: SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
    }

    fun visitDag(dag: Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: Studiedag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: Utenlandsdag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) {}
    fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) {}
}

internal interface InntekthistorikkVisitor {
    fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {}
    fun visitInntekt(inntektsendring: Inntektshistorikk.Inntektsendring, hendelseId: UUID) {}
    fun postVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {}

    fun preVisitInntekthistorikkVol2(inntektshistorikk: InntektshistorikkVol2) {}
    fun preVisitInnslag(innslag: InntektshistorikkVol2.Innslag, id: UUID) {}
    fun visitInntektVol2(
        inntektsopplysning: InntektshistorikkVol2.Inntektsopplysning,
        id: UUID,
        fom: LocalDate,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitInntektSkattVol2(
        id: UUID,
        fom: LocalDate,
        måned: YearMonth,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitInntektSaksbehandlerVol2(id: UUID, fom: LocalDate, tidsstempel: LocalDateTime) {}
    fun postVisitInnslag(innslag: InntektshistorikkVol2.Innslag, id: UUID) {}
    fun postVisitInntekthistorikkVol2(inntektshistorikk: InntektshistorikkVol2) {}
    fun visitSaksbehandler(
        saksbehandler: InntektshistorikkVol2.Saksbehandler,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitInntektsmelding(
        inntektsmelding: InntektshistorikkVol2.Inntektsmelding,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
    }

    fun preVisitInntektsopplysningKopi(
        inntektsopplysning: InntektshistorikkVol2.InntektsopplysningReferanse,
        dato: LocalDate,
        hendelseId: UUID,
        tidsstempel: LocalDateTime
    ) {
    }

    fun postVisitInntektsopplysningKopi(
        inntektsopplysning: InntektshistorikkVol2.InntektsopplysningReferanse,
        dato: LocalDate,
        hendelseId: UUID,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitInfotrygd(
        infotrygd: InntektshistorikkVol2.Infotrygd,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
    }

    fun preVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite, id: UUID) {}
    fun visitSkattSykepengegrunnlag(
        sykepengegrunnlag: InntektshistorikkVol2.Skatt.Sykepengegrunnlag,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: InntektshistorikkVol2.Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitSkattSammenligningsgrunnlag(
        sammenligningsgrunnlag: InntektshistorikkVol2.Skatt.Sammenligningsgrunnlag,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: InntektshistorikkVol2.Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
    }

    fun postVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite, id: UUID) {}
}

internal interface UtbetalingVisitor : UtbetalingsdagVisitor, OppdragVisitor {
    fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        beregningId: UUID,
        type: Utbetaling.Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?
    ) {
    }

    fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {}
    fun postVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {}
    fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {}
    fun postVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {}
    fun preVisitPersonOppdrag(oppdrag: Oppdrag) {}
    fun postVisitPersonOppdrag(oppdrag: Oppdrag) {}
    fun visitVurdering(vurdering: Utbetaling.Vurdering, ident: String, epost: String, tidspunkt: LocalDateTime, automatiskBehandling: Boolean) {}
    fun postVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        beregningId: UUID,
        type: Utbetaling.Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?
    ) {
    }
}

internal interface OppdragVisitor {
    fun preVisitOppdrag(
        oppdrag: Oppdrag,
        totalBeløp: Int,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        beløp: Int?,
        aktuellDagsinntekt: Int,
        grad: Double,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?
    ) {
    }

    fun postVisitOppdrag(
        oppdrag: Oppdrag,
        totalBeløp: Int,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime
    ) {
    }
}
