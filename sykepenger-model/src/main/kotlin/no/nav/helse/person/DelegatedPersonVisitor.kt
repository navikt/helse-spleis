package no.nav.helse.person

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.UkjentInfotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjeberegning
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.*

internal class DelegatedPersonVisitor(private val delegateeFun: () -> PersonVisitor) : PersonVisitor {
    private val delegatee get() = delegateeFun()

    override fun preVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
        delegatee.preVisitAktivitetslogg(aktivitetslogg)
    }

    override fun visitInfo(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Info, melding: String, tidsstempel: String) {
        delegatee.visitInfo(kontekster, aktivitet, melding, tidsstempel)
    }

    override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
        delegatee.visitWarn(kontekster, aktivitet, melding, tidsstempel)
    }

    override fun preVisitUtbetalingstidslinjeberegninger(beregninger: List<Utbetalingstidslinjeberegning>) {
        delegatee.preVisitUtbetalingstidslinjeberegninger(beregninger)
    }

    override fun postVisitUtbetalingstidslinjeberegninger(beregninger: List<Utbetalingstidslinjeberegning>) {
        delegatee.postVisitUtbetalingstidslinjeberegninger(beregninger)
    }

    override fun postVisitGrunnlagsdata(skjæringstidspunkt: LocalDate, grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
        delegatee.postVisitGrunnlagsdata(skjæringstidspunkt, grunnlagsdata)
    }

    override fun postVisitInfotrygdVilkårsgrunnlag(skjæringstidspunkt: LocalDate, infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag) {
        delegatee.postVisitInfotrygdVilkårsgrunnlag(skjæringstidspunkt, infotrygdVilkårsgrunnlag)
    }

    override fun preVisitSykepengegrunnlag(sykepengegrunnlag1: Sykepengegrunnlag, sykepengegrunnlag: Inntekt, grunnlagForSykepengegrunnlag: Inntekt) {
        delegatee.preVisitSykepengegrunnlag(sykepengegrunnlag1, sykepengegrunnlag, grunnlagForSykepengegrunnlag)
    }

    override fun postVisitSykepengegrunnlag(sykepengegrunnlag1: Sykepengegrunnlag, sykepengegrunnlag: Inntekt, grunnlagForSykepengegrunnlag: Inntekt) {
        delegatee.postVisitSykepengegrunnlag(sykepengegrunnlag1, sykepengegrunnlag, grunnlagForSykepengegrunnlag)
    }

    override fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
        delegatee.preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning, orgnummer)
    }

    override fun postVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
        delegatee.postVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning, orgnummer)
    }

    override fun visitUtbetalingstidslinjeberegning(
        id: UUID,
        tidsstempel: LocalDateTime,
        sykdomshistorikkElementId: UUID,
        inntektshistorikkInnslagId: UUID,
        vilkårsgrunnlagHistorikkInnslagId: UUID
    ) {
        delegatee.visitUtbetalingstidslinjeberegning(id, tidsstempel, sykdomshistorikkElementId, inntektshistorikkInnslagId, vilkårsgrunnlagHistorikkInnslagId)
    }

    override fun visitBehov(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Behov,
        type: Aktivitetslogg.Aktivitet.Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any?>,
        tidsstempel: String
    ) {
        delegatee.visitBehov(kontekster, aktivitet, type, melding, detaljer, tidsstempel)
    }

    override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
        delegatee.visitError(kontekster, aktivitet, melding, tidsstempel)
    }

    override fun visitSevere(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Severe, melding: String, tidsstempel: String) {
        delegatee.visitSevere(kontekster, aktivitet, melding, tidsstempel)
    }

    override fun postVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
        delegatee.postVisitAktivitetslogg(aktivitetslogg)
    }

    override fun preVisitPerson(person: Person, opprettet: LocalDateTime, aktørId: String, fødselsnummer: Fødselsnummer, dødsdato: LocalDate?) {
        delegatee.preVisitPerson(person, opprettet, aktørId, fødselsnummer, dødsdato)
    }

    override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
        delegatee.visitPersonAktivitetslogg(aktivitetslogg)
    }

    override fun preVisitArbeidsgivere() {
        delegatee.preVisitArbeidsgivere()
    }

    override fun postVisitArbeidsgivere() {
        delegatee.postVisitArbeidsgivere()
    }

    override fun preVisitVilkårsgrunnlagHistorikk() {
        delegatee.preVisitVilkårsgrunnlagHistorikk()
    }

    override fun preVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
        delegatee.preVisitInnslag(innslag, id, opprettet)
    }

    override fun preVisitGrunnlagsdata(skjæringstidspunkt: LocalDate, grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
        delegatee.preVisitGrunnlagsdata(skjæringstidspunkt, grunnlagsdata)
    }

    override fun preVisitInfotrygdVilkårsgrunnlag(
        infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag
    ) {
        delegatee.preVisitInfotrygdVilkårsgrunnlag(infotrygdVilkårsgrunnlag, skjæringstidspunkt, sykepengegrunnlag)
    }

    override fun postVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
        delegatee.postVisitInnslag(innslag, id, opprettet)
    }

    override fun postVisitVilkårsgrunnlagHistorikk() {
        delegatee.postVisitVilkårsgrunnlagHistorikk()
    }

    override fun preVisitInfotrygdhistorikk() {
        delegatee.preVisitInfotrygdhistorikk()
    }

    override fun preVisitInfotrygdhistorikkElement(
        id: UUID,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        hendelseId: UUID?,
        lagretInntekter: Boolean,
        lagretVilkårsgrunnlag: Boolean,
        harStatslønn: Boolean
    ) {
        delegatee.preVisitInfotrygdhistorikkElement(id, tidsstempel, oppdatert, hendelseId, lagretInntekter, lagretVilkårsgrunnlag, harStatslønn)
    }

    override fun preVisitInfotrygdhistorikkPerioder() {
        delegatee.preVisitInfotrygdhistorikkPerioder()
    }

    override fun preVisitInfotrygdhistorikkInntektsopplysninger() {
        delegatee.preVisitInfotrygdhistorikkInntektsopplysninger()
    }

    override fun visitInfotrygdhistorikkInntektsopplysning(
        orgnr: String,
        sykepengerFom: LocalDate,
        inntekt: Inntekt,
        refusjonTilArbeidsgiver: Boolean,
        refusjonTom: LocalDate?,
        lagret: LocalDateTime?
    ) {
        delegatee.visitInfotrygdhistorikkInntektsopplysning(orgnr, sykepengerFom, inntekt, refusjonTilArbeidsgiver, refusjonTom, lagret)
    }

    override fun postVisitInfotrygdhistorikkInntektsopplysninger() {
        delegatee.postVisitInfotrygdhistorikkInntektsopplysninger()
    }

    override fun visitUgyldigePerioder(ugyldigePerioder: List<Pair<LocalDate?, LocalDate?>>) {
        delegatee.visitUgyldigePerioder(ugyldigePerioder)
    }

    override fun visitInfotrygdhistorikkArbeidskategorikoder(arbeidskategorikoder: Map<String, LocalDate>) {
        delegatee.visitInfotrygdhistorikkArbeidskategorikoder(arbeidskategorikoder)
    }

    override fun visitInfotrygdhistorikkFerieperiode(periode: Friperiode) {
        delegatee.visitInfotrygdhistorikkFerieperiode(periode)
    }

    override fun visitInfotrygdhistorikkPersonUtbetalingsperiode(orgnr: String, periode: Utbetalingsperiode, grad: Prosentdel, inntekt: Inntekt) {
        delegatee.visitInfotrygdhistorikkPersonUtbetalingsperiode(orgnr, periode, grad, inntekt)
    }

    override fun visitInfotrygdhistorikkArbeidsgiverUtbetalingsperiode(orgnr: String, periode: Utbetalingsperiode, grad: Prosentdel, inntekt: Inntekt) {
        delegatee.visitInfotrygdhistorikkArbeidsgiverUtbetalingsperiode(orgnr, periode, grad, inntekt)
    }

    override fun visitInfotrygdhistorikkUkjentPeriode(periode: UkjentInfotrygdperiode) {
        delegatee.visitInfotrygdhistorikkUkjentPeriode(periode)
    }

    override fun postVisitInfotrygdhistorikkPerioder() {
        delegatee.postVisitInfotrygdhistorikkPerioder()
    }

    override fun postVisitInfotrygdhistorikkElement(
        id: UUID,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        hendelseId: UUID?,
        lagretInntekter: Boolean,
        lagretVilkårsgrunnlag: Boolean,
        harStatslønn: Boolean
    ) {
        delegatee.postVisitInfotrygdhistorikkElement(id, tidsstempel, oppdatert, hendelseId, lagretInntekter, lagretVilkårsgrunnlag, harStatslønn)
    }

    override fun postVisitInfotrygdhistorikk() {
        delegatee.postVisitInfotrygdhistorikk()
    }

    override fun postVisitPerson(person: Person, opprettet: LocalDateTime, aktørId: String, fødselsnummer: Fødselsnummer, dødsdato: LocalDate?) {
        delegatee.postVisitPerson(person, opprettet, aktørId, fødselsnummer, dødsdato)
    }

    override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
        delegatee.preVisitArbeidsgiver(arbeidsgiver, id, organisasjonsnummer)
    }

    override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        delegatee.preVisitUtbetalinger(utbetalinger)
    }

    override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        delegatee.postVisitUtbetalinger(utbetalinger)
    }

    override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        delegatee.preVisitPerioder(vedtaksperioder)
    }

    override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        delegatee.postVisitPerioder(vedtaksperioder)
    }

    override fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        delegatee.preVisitForkastedePerioder(vedtaksperioder)
    }

    override fun preVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak) {
        delegatee.preVisitForkastetPeriode(vedtaksperiode, forkastetÅrsak)
    }

    override fun postVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak) {
        delegatee.postVisitForkastetPeriode(vedtaksperiode, forkastetÅrsak)
    }

    override fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        delegatee.postVisitForkastedePerioder(vedtaksperioder)
    }

    override fun postVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
        delegatee.postVisitArbeidsgiver(arbeidsgiver, id, organisasjonsnummer)
    }

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<UUID>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
    ) {
        delegatee.preVisitVedtaksperiode(
            vedtaksperiode,
            id,
            tilstand,
            opprettet,
            oppdatert,
            periode,
            opprinneligPeriode,
            skjæringstidspunkt,
            periodetype,
            forlengelseFraInfotrygd,
            hendelseIder,
            inntektsmeldingInfo,
            inntektskilde
        )
    }

    override fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: VilkårsgrunnlagHistorikk.Grunnlagsdata?) {
        delegatee.visitDataForVilkårsvurdering(dataForVilkårsvurdering)
    }

    override fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {
        delegatee.visitDataForSimulering(dataForSimuleringResultat)
    }

    override fun preVisitFeriepengeutbetalinger(feriepengeutbetalinger: List<Feriepengeutbetaling>) {
        delegatee.preVisitFeriepengeutbetalinger(feriepengeutbetalinger)
    }

    override fun postVisitFeriepengeutbetalinger(feriepengeutbetalinger: List<Feriepengeutbetaling>) {
        delegatee.postVisitFeriepengeutbetalinger(feriepengeutbetalinger)
    }

    override fun preVisitFeriepengeutbetaling(
        feriepengeutbetaling: Feriepengeutbetaling,
        infotrygdFeriepengebeløpPerson: Double,
        infotrygdFeriepengebeløpArbeidsgiver: Double,
        spleisFeriepengebeløpArbeidsgiver: Double,
        overføringstidspunkt: LocalDateTime?,
        avstemmingsnøkkel: Long?,
        utbetalingId: UUID
    ) {
        delegatee.preVisitFeriepengeutbetaling(
            feriepengeutbetaling,
            infotrygdFeriepengebeløpPerson,
            infotrygdFeriepengebeløpArbeidsgiver,
            spleisFeriepengebeløpArbeidsgiver,
            overføringstidspunkt,
            avstemmingsnøkkel,
            utbetalingId
        )
    }

    override fun postVisitFeriepengeutbetaling(
        feriepengeutbetaling: Feriepengeutbetaling,
        infotrygdFeriepengebeløpPerson: Double,
        infotrygdFeriepengebeløpArbeidsgiver: Double,
        spleisFeriepengebeløpArbeidsgiver: Double,
        overføringstidspunkt: LocalDateTime?,
        avstemmingsnøkkel: Long?
    ) {
        delegatee.postVisitFeriepengeutbetaling(
            feriepengeutbetaling,
            infotrygdFeriepengebeløpPerson,
            infotrygdFeriepengebeløpArbeidsgiver,
            spleisFeriepengebeløpArbeidsgiver,
            overføringstidspunkt,
            avstemmingsnøkkel
        )
    }

    override fun preVisitFeriepengeberegner(
        feriepengeberegner: Feriepengeberegner,
        feriepengedager: List<Feriepengeberegner.UtbetaltDag>,
        opptjeningsår: Year,
        utbetalteDager: List<Feriepengeberegner.UtbetaltDag>
    ) {
        delegatee.preVisitFeriepengeberegner(feriepengeberegner, feriepengedager, opptjeningsår, utbetalteDager)
    }

    override fun postVisitFeriepengeberegner(feriepengeberegner: Feriepengeberegner) {
        delegatee.postVisitFeriepengeberegner(feriepengeberegner)
    }

    override fun preVisitUtbetaleDager() {
        delegatee.preVisitUtbetaleDager()
    }

    override fun postVisitUtbetaleDager() {
        delegatee.postVisitUtbetaleDager()
    }

    override fun preVisitFeriepengedager() {
        delegatee.preVisitFeriepengedager()
    }

    override fun postVisitFeriepengedager() {
        delegatee.postVisitFeriepengedager()
    }

    override fun visitInfotrygdArbeidsgiverDag(
        infotrygdArbeidsgiver: Feriepengeberegner.UtbetaltDag.InfotrygdArbeidsgiver,
        orgnummer: String,
        dato: LocalDate,
        beløp: Int
    ) {
        delegatee.visitInfotrygdArbeidsgiverDag(infotrygdArbeidsgiver, orgnummer, dato, beløp)
    }

    override fun visitInfotrygdPersonDag(infotrygdPerson: Feriepengeberegner.UtbetaltDag.InfotrygdPerson, orgnummer: String, dato: LocalDate, beløp: Int) {
        delegatee.visitInfotrygdPersonDag(infotrygdPerson, orgnummer, dato, beløp)
    }

    override fun visitSpleisArbeidsgiverDag(
        spleisArbeidsgiver: Feriepengeberegner.UtbetaltDag.SpleisArbeidsgiver,
        orgnummer: String,
        dato: LocalDate,
        beløp: Int
    ) {
        delegatee.visitSpleisArbeidsgiverDag(spleisArbeidsgiver, orgnummer, dato, beløp)
    }

    override fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<UUID>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
    ) {
        delegatee.postVisitVedtaksperiode(
            vedtaksperiode,
            id,
            tilstand,
            opprettet,
            oppdatert,
            periode,
            opprinneligPeriode,
            skjæringstidspunkt,
            periodetype,
            forlengelseFraInfotrygd,
            hendelseIder,
            inntektsmeldingInfo,
            inntektskilde
        )
    }

    override fun preVisit(tidslinje: Utbetalingstidslinje) {
        delegatee.preVisit(tidslinje)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag, dato: LocalDate, økonomi: Økonomi) {
        delegatee.visit(dag, dato, økonomi)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag, dato: LocalDate, økonomi: Økonomi) {
        delegatee.visit(dag, dato, økonomi)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag, dato: LocalDate, økonomi: Økonomi) {
        delegatee.visit(dag, dato, økonomi)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag, dato: LocalDate, økonomi: Økonomi) {
        delegatee.visit(dag, dato, økonomi)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag, dato: LocalDate, økonomi: Økonomi) {
        delegatee.visit(dag, dato, økonomi)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag, dato: LocalDate, økonomi: Økonomi) {
        delegatee.visit(dag, dato, økonomi)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag, dato: LocalDate, økonomi: Økonomi) {
        delegatee.visit(dag, dato, økonomi)
    }

    override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag, dato: LocalDate, økonomi: Økonomi) {
        delegatee.visit(dag, dato, økonomi)
    }

    override fun postVisit(tidslinje: Utbetalingstidslinje) {
        delegatee.postVisit(tidslinje)
    }

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        delegatee.preVisitSykdomshistorikk(sykdomshistorikk)
    }

    override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
        delegatee.preVisitSykdomshistorikkElement(element, id, hendelseId, tidsstempel)
    }

    override fun preVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje, hendelseId: UUID?, tidsstempel: LocalDateTime) {
        delegatee.preVisitHendelseSykdomstidslinje(tidslinje, hendelseId, tidsstempel)
    }

    override fun postVisitHendelseSykdomstidslinje(tidslinje: Sykdomstidslinje) {
        delegatee.postVisitHendelseSykdomstidslinje(tidslinje)
    }

    override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
        delegatee.preVisitBeregnetSykdomstidslinje(tidslinje)
    }

    override fun postVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
        delegatee.postVisitBeregnetSykdomstidslinje(tidslinje)
    }

    override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
        delegatee.postVisitSykdomshistorikkElement(element, id, hendelseId, tidsstempel)
    }

    override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        delegatee.postVisitSykdomshistorikk(sykdomshistorikk)
    }

    override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
        delegatee.preVisitSykdomstidslinje(tidslinje, låstePerioder)
    }

    override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        delegatee.visitDag(dag, dato, kilde)
    }

    override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        delegatee.visitDag(dag, dato, kilde)
    }

    override fun visitDag(dag: Dag.Arbeidsgiverdag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        delegatee.visitDag(dag, dato, økonomi, kilde)
    }

    override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        delegatee.visitDag(dag, dato, kilde)
    }

    override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        delegatee.visitDag(dag, dato, kilde)
    }

    override fun visitDag(dag: Dag.ArbeidsgiverHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        delegatee.visitDag(dag, dato, økonomi, kilde)
    }

    override fun visitDag(dag: Dag.Sykedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        delegatee.visitDag(dag, dato, økonomi, kilde)
    }

    override fun visitDag(dag: Dag.ForeldetSykedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        delegatee.visitDag(dag, dato, økonomi, kilde)
    }

    override fun visitDag(dag: Dag.SykHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        delegatee.visitDag(dag, dato, økonomi, kilde)
    }

    override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        delegatee.visitDag(dag, dato, kilde)
    }

    override fun visitDag(dag: Dag.ProblemDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde, melding: String) {
        delegatee.visitDag(dag, dato, kilde, melding)
    }

    override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: MutableList<Periode>) {
        delegatee.postVisitSykdomstidslinje(tidslinje, låstePerioder)
    }

    override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
        delegatee.preVisitInntekthistorikk(inntektshistorikk)
    }

    override fun preVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) {
        delegatee.preVisitInnslag(innslag, id)
    }

    override fun visitInntekt(inntektsopplysning: Inntektshistorikk.Inntektsopplysning, id: UUID, fom: LocalDate, tidsstempel: LocalDateTime) {
        delegatee.visitInntekt(inntektsopplysning, id, fom, tidsstempel)
    }

    override fun visitInntektSkatt(id: UUID, fom: LocalDate, måned: YearMonth, tidsstempel: LocalDateTime) {
        delegatee.visitInntektSkatt(id, fom, måned, tidsstempel)
    }

    override fun visitInntektSaksbehandler(id: UUID, fom: LocalDate, tidsstempel: LocalDateTime) {
        delegatee.visitInntektSaksbehandler(id, fom, tidsstempel)
    }

    override fun postVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) {
        delegatee.postVisitInnslag(innslag, id)
    }

    override fun postVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
        delegatee.postVisitInntekthistorikk(inntektshistorikk)
    }

    override fun visitSaksbehandler(
        saksbehandler: Inntektshistorikk.Saksbehandler,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        delegatee.visitSaksbehandler(saksbehandler, dato, hendelseId, beløp, tidsstempel)
    }

    override fun visitInntektsmelding(
        inntektsmelding: Inntektshistorikk.Inntektsmelding,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        delegatee.visitInntektsmelding(inntektsmelding, dato, hendelseId, beløp, tidsstempel)
    }

    override fun visitInfotrygd(infotrygd: Inntektshistorikk.Infotrygd, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) {
        delegatee.visitInfotrygd(infotrygd, dato, hendelseId, beløp, tidsstempel)
    }

    override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
        delegatee.preVisitSkatt(skattComposite, id, dato)
    }

    override fun visitSkattSykepengegrunnlag(
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
        delegatee.visitSkattSykepengegrunnlag(sykepengegrunnlag, dato, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel)
    }

    override fun visitSkattSammenligningsgrunnlag(
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
        delegatee.visitSkattSammenligningsgrunnlag(sammenligningsgrunnlag, dato, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel)
    }

    override fun postVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
        delegatee.postVisitSkatt(skattComposite, id, dato)
    }

    override fun preVisitUtbetaling(
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
        delegatee.preVisitUtbetaling(
            utbetaling,
            id,
            beregningId,
            type,
            tilstand,
            tidsstempel,
            oppdatert,
            arbeidsgiverNettoBeløp,
            personNettoBeløp,
            maksdato,
            forbrukteSykedager,
            gjenståendeSykedager
        )
    }

    override fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {
        delegatee.preVisitTidslinjer(tidslinjer)
    }

    override fun postVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {
        delegatee.postVisitTidslinjer(tidslinjer)
    }

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        delegatee.preVisitArbeidsgiverOppdrag(oppdrag)
    }

    override fun postVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        delegatee.postVisitArbeidsgiverOppdrag(oppdrag)
    }

    override fun preVisitPersonOppdrag(oppdrag: Oppdrag) {
        delegatee.preVisitPersonOppdrag(oppdrag)
    }

    override fun postVisitPersonOppdrag(oppdrag: Oppdrag) {
        delegatee.postVisitPersonOppdrag(oppdrag)
    }

    override fun visitVurdering(
        vurdering: Utbetaling.Vurdering,
        ident: String,
        epost: String,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        godkjent: Boolean
    ) {
        delegatee.visitVurdering(vurdering, ident, epost, tidspunkt, automatiskBehandling, godkjent)
    }

    override fun preVisitVedtakserperiodeUtbetalinger(utbetalinger: List<Utbetaling>) {
        delegatee.preVisitVedtakserperiodeUtbetalinger(utbetalinger)
    }

    override fun postVisitVedtakserperiodeUtbetalinger(utbetalinger: List<Utbetaling>) {
        delegatee.postVisitVedtakserperiodeUtbetalinger(utbetalinger)
    }

    override fun postVisitUtbetaling(
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
        delegatee.postVisitUtbetaling(
            utbetaling,
            id,
            beregningId,
            type,
            tilstand,
            tidsstempel,
            oppdatert,
            arbeidsgiverNettoBeløp,
            personNettoBeløp,
            maksdato,
            forbrukteSykedager,
            gjenståendeSykedager
        )
    }

    override fun preVisitOppdrag(oppdrag: Oppdrag, totalBeløp: Int, nettoBeløp: Int, tidsstempel: LocalDateTime) {
        delegatee.preVisitOppdrag(oppdrag, totalBeløp, nettoBeløp, tidsstempel)
    }

    override fun postVisitOppdrag(oppdrag: Oppdrag, totalBeløp: Int, nettoBeløp: Int, tidsstempel: LocalDateTime) {
        delegatee.postVisitOppdrag(oppdrag, totalBeløp, nettoBeløp, tidsstempel)
    }

    override fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        satstype: Satstype,
        beløp: Int?,
        aktuellDagsinntekt: Int?,
        grad: Double?,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?,
        klassekode: Klassekode
    ) {
        delegatee.visitUtbetalingslinje(
            linje,
            fom,
            tom,
            satstype,
            beløp,
            aktuellDagsinntekt,
            grad,
            delytelseId,
            refDelytelseId,
            refFagsystemId,
            endringskode,
            datoStatusFom,
            klassekode
        )
    }

    override fun preVisitArbeidsforholdhistorikk(arbeidsforholdhistorikk: Arbeidsforholdhistorikk) {
        delegatee.preVisitArbeidsforholdhistorikk(arbeidsforholdhistorikk)
    }

    override fun postVisitArbeidsforholdhistorikk(arbeidsforholdhistorikk: Arbeidsforholdhistorikk) {
        delegatee.postVisitArbeidsforholdhistorikk(arbeidsforholdhistorikk)
    }

    override fun preVisitArbeidsforholdinnslag(arbeidsforholdinnslag: Arbeidsforholdhistorikk.Innslag, id: UUID, skjæringstidspunkt: LocalDate) {
        delegatee.preVisitArbeidsforholdinnslag(arbeidsforholdinnslag, id, skjæringstidspunkt)
    }

    override fun visitArbeidsforhold(orgnummer: String, fom: LocalDate, tom: LocalDate?) {
        delegatee.visitArbeidsforhold(orgnummer, fom, tom)
    }

    override fun postVisitArbeidsforholdinnslag(arbeidsforholdinnslag: Arbeidsforholdhistorikk.Innslag, id: UUID, skjæringstidspunkt: LocalDate) {
        delegatee.postVisitArbeidsforholdinnslag(arbeidsforholdinnslag, id, skjæringstidspunkt)
    }
}
