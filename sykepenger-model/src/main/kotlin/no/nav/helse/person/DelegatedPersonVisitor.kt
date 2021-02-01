package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
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

    override fun visitBehov(
        kontekster: List<SpesifikkKontekst>,
        aktivitet: Aktivitetslogg.Aktivitet.Behov,
        type: Aktivitetslogg.Aktivitet.Behov.Behovtype,
        melding: String,
        detaljer: Map<String, Any>,
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

    override fun preVisitPerson(person: Person, opprettet: LocalDateTime, aktørId: String, fødselsnummer: String) {
        delegatee.preVisitPerson(person, opprettet, aktørId, fødselsnummer)
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

    override fun postVisitPerson(person: Person, opprettet: LocalDateTime, aktørId: String, fødselsnummer: String) {
        delegatee.postVisitPerson(person, opprettet, aktørId, fødselsnummer)
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
        hendelseIder: List<UUID>
    ) {
        delegatee.preVisitVedtaksperiode(vedtaksperiode, id, tilstand, opprettet, oppdatert, periode, opprinneligPeriode, hendelseIder)
    }

    override fun visitSkjæringstidspunkt(skjæringstidspunkt: LocalDate) {
        delegatee.visitSkjæringstidspunkt(skjæringstidspunkt)
    }

    override fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?) {
        delegatee.visitDataForVilkårsvurdering(dataForVilkårsvurdering)
    }

    override fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {
        delegatee.visitDataForSimulering(dataForSimuleringResultat)
    }

    override fun visitForlengelseFraInfotrygd(forlengelseFraInfotrygd: ForlengelseFraInfotrygd) {
        delegatee.visitForlengelseFraInfotrygd(forlengelseFraInfotrygd)
    }

    override fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode
    ) {
        delegatee.postVisitVedtaksperiode(vedtaksperiode, id, tilstand, opprettet, oppdatert, periode, opprinneligPeriode)
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

    override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID?, tidsstempel: LocalDateTime) {
        delegatee.preVisitSykdomshistorikkElement(element, id, tidsstempel)
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

    override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID?, tidsstempel: LocalDateTime) {
        delegatee.postVisitSykdomshistorikkElement(element, id, tidsstempel)
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

    override fun visitDag(dag: Dag.Studiedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        delegatee.visitDag(dag, dato, kilde)
    }

    override fun visitDag(dag: Dag.Utenlandsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        delegatee.visitDag(dag, dato, kilde)
    }

    override fun visitDag(dag: Dag.ProblemDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde, melding: String) {
        delegatee.visitDag(dag, dato, kilde, melding)
    }

    override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) {
        delegatee.postVisitSykdomstidslinje(tidslinje)
    }

    override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
        delegatee.preVisitInntekthistorikk(inntektshistorikk)
    }

    override fun visitInntekt(inntektsendring: Inntektshistorikk.Inntektsendring, id: UUID) {
        delegatee.visitInntekt(inntektsendring, id)
    }

    override fun postVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
        delegatee.postVisitInntekthistorikk(inntektshistorikk)
    }

    override fun preVisitInntekthistorikkVol2(inntektshistorikk: InntektshistorikkVol2) {
        delegatee.preVisitInntekthistorikkVol2(inntektshistorikk)
    }

    override fun preVisitInnslag(innslag: InntektshistorikkVol2.Innslag) {
        delegatee.preVisitInnslag(innslag)
    }

    override fun visitInntektVol2(inntektsopplysning: InntektshistorikkVol2.Inntektsopplysning, id: UUID, fom: LocalDate, tidsstempel: LocalDateTime) {
        delegatee.visitInntektVol2(inntektsopplysning, id, fom, tidsstempel)
    }

    override fun visitInntektSkattVol2(id: UUID, fom: LocalDate, måned: YearMonth, tidsstempel: LocalDateTime) {
        delegatee.visitInntektSkattVol2(id, fom, måned, tidsstempel)
    }

    override fun visitInntektSaksbehandlerVol2(id: UUID, fom: LocalDate, tidsstempel: LocalDateTime) {
        delegatee.visitInntektSaksbehandlerVol2(id, fom, tidsstempel)
    }

    override fun postVisitInnslag(innslag: InntektshistorikkVol2.Innslag) {
        delegatee.postVisitInnslag(innslag)
    }

    override fun postVisitInntekthistorikkVol2(inntektshistorikk: InntektshistorikkVol2) {
        delegatee.postVisitInntekthistorikkVol2(inntektshistorikk)
    }

    override fun visitSaksbehandler(
        saksbehandler: InntektshistorikkVol2.Saksbehandler,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        delegatee.visitSaksbehandler(saksbehandler, dato, hendelseId, beløp, tidsstempel)
    }

    override fun visitInntektsmelding(
        inntektsmelding: InntektshistorikkVol2.Inntektsmelding,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        delegatee.visitInntektsmelding(inntektsmelding, dato, hendelseId, beløp, tidsstempel)
    }

    override fun visitInntektsopplysningKopi(
        inntektsopplysning: InntektshistorikkVol2.InntektsopplysningKopi,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        delegatee.visitInntektsopplysningKopi(inntektsopplysning, dato, hendelseId, beløp, tidsstempel)
    }

    override fun visitInfotrygd(infotrygd: InntektshistorikkVol2.Infotrygd, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) {
        delegatee.visitInfotrygd(infotrygd, dato, hendelseId, beløp, tidsstempel)
    }

    override fun preVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite) {
        delegatee.preVisitSkatt(skattComposite)
    }

    override fun visitSkattSykepengegrunnlag(
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
        delegatee.visitSkattSykepengegrunnlag(sykepengegrunnlag, dato, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel)
    }

    override fun visitSkattSammenligningsgrunnlag(
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
        delegatee.visitSkattSammenligningsgrunnlag(sammenligningsgrunnlag, dato, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel)
    }

    override fun postVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite) {
        delegatee.postVisitSkatt(skattComposite)
    }

    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
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

    override fun visitVurdering(vurdering: Utbetaling.Vurdering, ident: String, epost: String, tidspunkt: LocalDateTime, automatiskBehandling: Boolean) {
        delegatee.visitVurdering(vurdering, ident, epost, tidspunkt, automatiskBehandling)
    }

    override fun postVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
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

    override fun visitUtbetalingslinje(
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
        delegatee.visitUtbetalingslinje(linje, fom, tom, beløp, aktuellDagsinntekt, grad, delytelseId, refDelytelseId, refFagsystemId, endringskode, datoStatusFom)
    }

    override fun postVisitOppdrag(oppdrag: Oppdrag, totalBeløp: Int, nettoBeløp: Int, tidsstempel: LocalDateTime) {
        delegatee.postVisitOppdrag(oppdrag, totalBeløp, nettoBeløp, tidsstempel)
    }
}
