package no.nav.helse.person

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.UgyldigPeriode
import no.nav.helse.person.infotrygdhistorikk.UkjentInfotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjeberegning
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.*

internal interface PersonVisitor : ArbeidsgiverVisitor, AktivitetsloggVisitor, VilkårsgrunnlagHistorikkVisitor, InfotrygdhistorikkVisitor {
    fun preVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        fødselsnummer: Fødselsnummer,
        dødsdato: LocalDate?,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {}
    fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
    fun preVisitArbeidsgivere() {}
    fun postVisitArbeidsgivere() {}
    fun postVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        fødselsnummer: Fødselsnummer,
        dødsdato: LocalDate?,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {}
}

internal interface InfotrygdhistorikkVisitor {
    fun preVisitInfotrygdhistorikk() {}
    fun preVisitInfotrygdhistorikkElement(
        id: UUID,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        hendelseId: UUID?,
        lagretInntekter: Boolean,
        lagretVilkårsgrunnlag: Boolean,
        harStatslønn: Boolean
    ) {
    }

    fun preVisitInfotrygdhistorikkPerioder() {}
    fun visitInfotrygdhistorikkFerieperiode(periode: Friperiode) {}
    fun visitInfotrygdhistorikkPersonUtbetalingsperiode(orgnr: String, periode: Utbetalingsperiode, grad: Prosentdel, inntekt: Inntekt) {}
    fun visitInfotrygdhistorikkArbeidsgiverUtbetalingsperiode(orgnr: String, periode: Utbetalingsperiode, grad: Prosentdel, inntekt: Inntekt) {}
    fun visitInfotrygdhistorikkUkjentPeriode(periode: UkjentInfotrygdperiode) {}
    fun postVisitInfotrygdhistorikkPerioder() {}
    fun preVisitInfotrygdhistorikkInntektsopplysninger() {}
    fun visitInfotrygdhistorikkInntektsopplysning(
        orgnr: String,
        sykepengerFom: LocalDate,
        inntekt: Inntekt,
        refusjonTilArbeidsgiver: Boolean,
        refusjonTom: LocalDate?,
        lagret: LocalDateTime?
    ) {
    }

    fun postVisitInfotrygdhistorikkInntektsopplysninger() {}
    fun visitUgyldigePerioder(ugyldigePerioder: List<UgyldigPeriode>) {}
    fun visitInfotrygdhistorikkArbeidskategorikoder(arbeidskategorikoder: Map<String, LocalDate>) {}
    fun postVisitInfotrygdhistorikkElement(
        id: UUID,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        hendelseId: UUID?,
        lagretInntekter: Boolean,
        lagretVilkårsgrunnlag: Boolean,
        harStatslønn: Boolean
    ) {
    }

    fun postVisitInfotrygdhistorikk() {}
}

internal interface FeriepengeutbetalingsperiodeVisitor {
    fun visitPersonutbetalingsperiode(orgnr: String, periode: Periode, beløp: Int, utbetalt: LocalDate) {}
    fun visitArbeidsgiverutbetalingsperiode(orgnr: String, periode: Periode, beløp: Int, utbetalt: LocalDate) {}
}

internal interface VilkårsgrunnlagHistorikkVisitor : InntekthistorikkVisitor {
    fun preVisitVilkårsgrunnlagHistorikk() {}
    fun preVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {}
    fun postVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {}
    fun postVisitVilkårsgrunnlagHistorikk() {}
    fun preVisitGrunnlagsdata(
        skjæringstidspunkt: LocalDate,
        grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
        sykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Inntekt,
        avviksprosent: Prosent?,
        antallOpptjeningsdagerErMinst: Int,
        harOpptjening: Boolean,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        harMinimumInntekt: Boolean?,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?,
        vilkårsgrunnlagId: UUID
    ) {}
    fun postVisitGrunnlagsdata(
        skjæringstidspunkt: LocalDate,
        grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
        sykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Inntekt,
        avviksprosent: Prosent?,
        antallOpptjeningsdagerErMinst: Int,
        harOpptjening: Boolean,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        harMinimumInntekt: Boolean?,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?,
        vilkårsgrunnlagId: UUID
    ) {}
    fun preVisitInfotrygdVilkårsgrunnlag(
        infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag,
        vilkårsgrunnlagId: UUID
    ) {
    }

    fun postVisitInfotrygdVilkårsgrunnlag(
        infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag,
        vilkårsgrunnlagId: UUID
    ) {}
    fun preVisitSykepengegrunnlag(
        sykepengegrunnlag1: Sykepengegrunnlag,
        sykepengegrunnlag: Inntekt,
        grunnlagForSykepengegrunnlag: Inntekt,
        begrensning: Sykepengegrunnlag.Begrensning
    ) {}
    fun postVisitSykepengegrunnlag(
        sykepengegrunnlag1: Sykepengegrunnlag,
        sykepengegrunnlag: Inntekt,
        grunnlagForSykepengegrunnlag: Inntekt,
        begrensning: Sykepengegrunnlag.Begrensning
    ) {}
    fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {}
    fun postVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {}
}

internal interface ArbeidsgiverVisitor : InntekthistorikkVisitor, SykdomshistorikkVisitor, VedtaksperiodeVisitor, UtbetalingVisitor,
    UtbetalingstidslinjeberegningVisitor, FeriepengeutbetalingVisitor, RefusjonshistorikkVisitor, ArbeidsforholdhistorikkVisitor {
    fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
    }

    fun preVisitUtbetalingstidslinjeberegninger(beregninger: List<Utbetalingstidslinjeberegning>) {}
    fun postVisitUtbetalingstidslinjeberegninger(beregninger: List<Utbetalingstidslinjeberegning>) {}
    fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {}
    fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {}
    fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {}
    fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {}
    fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {}
    fun preVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak) {}
    fun postVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak) {}
    fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {}
    fun visitRefusjonOpphører(refusjonOpphører: List<LocalDate?>) {}
    fun postVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
    }
}

internal interface FeriepengeutbetalingVisitor : OppdragVisitor {
    fun preVisitFeriepengeutbetalinger(feriepengeutbetalinger: List<Feriepengeutbetaling>) {}
    fun preVisitFeriepengeutbetaling(
        feriepengeutbetaling: Feriepengeutbetaling,
        infotrygdFeriepengebeløpPerson: Double,
        infotrygdFeriepengebeløpArbeidsgiver: Double,
        spleisFeriepengebeløpArbeidsgiver: Double,
        overføringstidspunkt: LocalDateTime?,
        avstemmingsnøkkel: Long?,
        utbetalingId: UUID
    ) {
    }

    fun preVisitFeriepengeberegner(
        feriepengeberegner: Feriepengeberegner,
        feriepengedager: List<Feriepengeberegner.UtbetaltDag>,
        opptjeningsår: Year,
        utbetalteDager: List<Feriepengeberegner.UtbetaltDag>
    ) {
    }

    fun preVisitUtbetaleDager() {}
    fun postVisitUtbetaleDager() {}
    fun preVisitFeriepengedager() {}
    fun postVisitFeriepengedager() {}

    fun visitInfotrygdPersonDag(infotrygdPerson: Feriepengeberegner.UtbetaltDag.InfotrygdPerson, orgnummer: String, dato: LocalDate, beløp: Int) {}
    fun visitInfotrygdArbeidsgiverDag(
        infotrygdArbeidsgiver: Feriepengeberegner.UtbetaltDag.InfotrygdArbeidsgiver,
        orgnummer: String,
        dato: LocalDate,
        beløp: Int
    ) {
    }

    fun visitSpleisArbeidsgiverDag(spleisArbeidsgiver: Feriepengeberegner.UtbetaltDag.SpleisArbeidsgiver, orgnummer: String, dato: LocalDate, beløp: Int) {}
    fun postVisitFeriepengeberegner(
        feriepengeberegner: Feriepengeberegner,
        feriepengedager: List<Feriepengeberegner.UtbetaltDag>,
        opptjeningsår: Year,
        utbetalteDager: List<Feriepengeberegner.UtbetaltDag>
    ) {}
    fun postVisitFeriepengeutbetaling(
        feriepengeutbetaling: Feriepengeutbetaling,
        infotrygdFeriepengebeløpPerson: Double,
        infotrygdFeriepengebeløpArbeidsgiver: Double,
        spleisFeriepengebeløpArbeidsgiver: Double,
        overføringstidspunkt: LocalDateTime?,
        avstemmingsnøkkel: Long?,
        utbetalingId: UUID
    ) {
    }

    fun postVisitFeriepengeutbetalinger(feriepengeutbetalinger: List<Feriepengeutbetaling>) {}
}

internal interface UtbetalingstidslinjeberegningVisitor : UtbetalingsdagVisitor {
    fun preVisitUtbetalingstidslinjeberegning(
        id: UUID,
        tidsstempel: LocalDateTime,
        organisasjonsnummer: String,
        sykdomshistorikkElementId: UUID,
        inntektshistorikkInnslagId: UUID,
        vilkårsgrunnlagHistorikkInnslagId: UUID
    ) {
    }

    fun postVisitUtbetalingstidslinjeberegning(
        id: UUID,
        tidsstempel: LocalDateTime,
        organisasjonsnummer: String,
        sykdomshistorikkElementId: UUID,
        inntektshistorikkInnslagId: UUID,
        vilkårsgrunnlagHistorikkInnslagId: UUID
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
        skjæringstidspunkt: LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        periodetype: Periodetype,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<UUID>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
    ) {
    }

    fun visitInntektsmeldinginfo(id: UUID, arbeidsforholdId: String?) {}
    fun preVisitVedtakserperiodeUtbetalinger(utbetalinger: List<Utbetaling>) {}
    fun postVisitVedtakserperiodeUtbetalinger(utbetalinger: List<Utbetaling>) {}
    fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: VilkårsgrunnlagHistorikk.Grunnlagsdata?) {}
    fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        periodetype: Periodetype,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<UUID>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
    ) {
    }
}

internal interface UtbetalingsdagVisitor {
    fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {}
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

    fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {}
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
    fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: Hendelseskilde, melding: String) {}
    fun visitDag(dag: AvslåttDag, dato: LocalDate, kilde: Hendelseskilde) {}
    fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: MutableList<Periode>) {}
}

internal interface RefusjonshistorikkVisitor {
    fun preVisitRefusjonshistorikk(refusjonshistorikk: Refusjonshistorikk) {}
    fun preVisitRefusjon(
        meldingsreferanseId: UUID,
        førsteFraværsdag: LocalDate?,
        arbeidsgiverperioder: List<Periode>,
        beløp: Inntekt?,
        sisteRefusjonsdag: LocalDate?,
        endringerIRefusjon: List<Refusjonshistorikk.Refusjon.EndringIRefusjon>,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitEndringIRefusjon(beløp: Inntekt, endringsdato: LocalDate) {}
    fun postVisitRefusjon(
        meldingsreferanseId: UUID,
        førsteFraværsdag: LocalDate?,
        arbeidsgiverperioder: List<Periode>,
        beløp: Inntekt?,
        sisteRefusjonsdag: LocalDate?,
        endringerIRefusjon: List<Refusjonshistorikk.Refusjon.EndringIRefusjon>,
        tidsstempel: LocalDateTime
    ) {
    }

    fun postVisitRefusjonshistorikk(refusjonshistorikk: Refusjonshistorikk) {}
}

internal interface ArbeidsforholdhistorikkVisitor {
    fun preVisitArbeidsforholdhistorikk(arbeidsforholdhistorikk: Arbeidsforholdhistorikk) {}
    fun postVisitArbeidsforholdhistorikk(arbeidsforholdhistorikk: Arbeidsforholdhistorikk) {}

    fun visitArbeidsforhold(orgnummer: String, fom: LocalDate, tom: LocalDate?) {}

    fun preVisitArbeidsforholdinnslag(arbeidsforholdinnslag: Arbeidsforholdhistorikk.Innslag, id: UUID, skjæringstidspunkt: LocalDate) {}
    fun postVisitArbeidsforholdinnslag(arbeidsforholdinnslag: Arbeidsforholdhistorikk.Innslag, id: UUID, skjæringstidspunkt: LocalDate) {}
}

internal interface InntekthistorikkVisitor {
    fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {}
    fun preVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) {}
    fun visitInntekt(
        inntektsopplysning: Inntektshistorikk.Inntektsopplysning,
        id: UUID,
        fom: LocalDate,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitInntektSkatt(
        id: UUID,
        fom: LocalDate,
        måned: YearMonth,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitInntektSaksbehandler(id: UUID, fom: LocalDate, tidsstempel: LocalDateTime) {}
    fun postVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) {}
    fun postVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {}
    fun visitSaksbehandler(
        saksbehandler: Inntektshistorikk.Saksbehandler,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitInntektsmelding(
        inntektsmelding: Inntektshistorikk.Inntektsmelding,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitInfotrygd(
        infotrygd: Inntektshistorikk.Infotrygd,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
    }

    fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {}
    fun visitSkattSykepengegrunnlag(
        sykepengegrunnlag: Inntektshistorikk.Skatt.Sykepengegrunnlag,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: Inntektshistorikk.Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
    }

    fun visitSkattSammenligningsgrunnlag(
        sammenligningsgrunnlag: Inntektshistorikk.Skatt.Sammenligningsgrunnlag,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: Inntektshistorikk.Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
    }

    fun postVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {}
}

internal interface UtbetalingVisitor : UtbetalingsdagVisitor, OppdragVisitor {
    fun preVisitUtbetaling(
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
    }

    fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {}
    fun postVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {}
    fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {}
    fun postVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {}
    fun preVisitPersonOppdrag(oppdrag: Oppdrag) {}
    fun postVisitPersonOppdrag(oppdrag: Oppdrag) {}
    fun visitVurdering(
        vurdering: Utbetaling.Vurdering,
        ident: String,
        epost: String,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        godkjent: Boolean
    ) {
    }

    fun postVisitUtbetaling(
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
    }
}

internal interface OppdragVisitor {
    fun preVisitOppdrag(
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
    }

    fun visitUtbetalingslinje(
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
    }

    fun postVisitOppdrag(
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
    }
}
