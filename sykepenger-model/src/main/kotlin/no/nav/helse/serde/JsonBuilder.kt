package no.nav.helse.serde

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.UkjentInfotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.serde.api.builders.BuilderState
import no.nav.helse.serde.mapping.JsonMedlemskapstatus
import no.nav.helse.serde.reflection.AktivitetsloggMap
import no.nav.helse.serde.reflection.Inntektsopplysningskilde
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.utbetalingslinjer.Feriepengeutbetaling
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.*
import kotlin.collections.set

fun Person.serialize(): SerialisertPerson {
    val jsonBuilder = JsonBuilder()
    this.accept(jsonBuilder)
    return SerialisertPerson(jsonBuilder.toJson())
}

internal class JsonBuilder : AbstractBuilder() {

    private lateinit var personBuilder: PersonState

    internal fun toJson() = personBuilder.build().toString()
    override fun toString() = toJson()

    override fun preVisitPerson(
        person: Person,
        opprettet: LocalDateTime,
        aktørId: String,
        fødselsnummer: Fødselsnummer,
        dødsdato: LocalDate?
    ) {
        personBuilder = PersonState(fødselsnummer.toString(), aktørId, opprettet, dødsdato)
        pushState(personBuilder)
    }

    private class PersonState(fødselsnummer: String, aktørId: String, opprettet: LocalDateTime, dødsdato: LocalDate?) : BuilderState() {
        private val personMap = mutableMapOf<String, Any?>(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "opprettet" to opprettet,
            "dødsdato" to dødsdato
        )

        private val arbeidsgivere = mutableListOf<MutableMap<String, Any?>>()

        fun build() =
            SerialisertPerson.medSkjemaversjon(serdeObjectMapper.valueToTree(personMap))

        override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
            personMap["aktivitetslogg"] = AktivitetsloggMap(aktivitetslogg).toMap()
        }

        override fun preVisitArbeidsgivere() {
            personMap["arbeidsgivere"] = arbeidsgivere
        }

        override fun preVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            val arbeidsgiverMap = mutableMapOf<String, Any?>()
            arbeidsgivere.add(arbeidsgiverMap)
            pushState(ArbeidsgiverState(arbeidsgiver, arbeidsgiverMap))
        }

        override fun postVisitPerson(
            person: Person,
            opprettet: LocalDateTime,
            aktørId: String,
            fødselsnummer: Fødselsnummer,
            dødsdato: LocalDate?
        ) {
            popState()
        }

        override fun preVisitInfotrygdhistorikk() {
            val historikk = mutableListOf<Map<String, Any?>>()
            personMap["infotrygdhistorikk"] = historikk
            pushState(InfotrygdhistorikkState(historikk))
        }

        override fun preVisitVilkårsgrunnlagHistorikk() {
            val historikk = mutableListOf<Map<String, Any?>>()
            personMap["vilkårsgrunnlagHistorikk"] = historikk
            pushState(VilkårsgrunnlagHistorikkState(historikk))
        }
    }

    private companion object {
        fun initVedtaksperiodeMap(
            vedtaksperiodeMap: MutableMap<String, Any?>,
            periode: Periode,
            opprinneligPeriode: Periode,
            hendelseIder: Set<UUID>,
            inntektskilde: Inntektskilde
        ) {
            vedtaksperiodeMap["fom"] = periode.start
            vedtaksperiodeMap["tom"] = periode.endInclusive
            vedtaksperiodeMap["sykmeldingFom"] = opprinneligPeriode.start
            vedtaksperiodeMap["sykmeldingTom"] = opprinneligPeriode.endInclusive
            vedtaksperiodeMap["hendelseIder"] = hendelseIder
            vedtaksperiodeMap["inntektskilde"] = inntektskilde
        }
    }

    private class ArbeidsgiverState(
        arbeidsgiver: Arbeidsgiver,
        private val arbeidsgiverMap: MutableMap<String, Any?>
    ) : BuilderState() {
        init {
            arbeidsgiverMap.putAll(arbeidsgiver.toMap())
        }

        override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
            val inntektshistorikkListe = mutableListOf<Map<String, Any?>>()
            arbeidsgiverMap["inntektshistorikk"] = inntektshistorikkListe
            pushState(InntektshistorikkState(inntektshistorikkListe))
        }

        override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            val sykdomshistorikkList = mutableListOf<MutableMap<String, Any?>>()
            arbeidsgiverMap["sykdomshistorikk"] = sykdomshistorikkList
            pushState(SykdomshistorikkState(sykdomshistorikkList))
        }

        private val utbetalingstidslinjer = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {
            arbeidsgiverMap["utbetalingstidslinjer"] = utbetalingstidslinjer
        }

        override fun preVisit(tidslinje: Utbetalingstidslinje) {
            val utbetalingstidslinjeMap = mutableMapOf<String, Any?>()
            utbetalingstidslinjer.add(utbetalingstidslinjeMap)
        }

        override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
            arbeidsgiverMap["utbetalinger"] = mutableListOf<MutableMap<String, Any?>>().also {
                pushState(UtbetalingerState(it))
            }
        }

        private val vedtaksperiodeListe = mutableListOf<MutableMap<String, Any?>>()

        override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            vedtaksperiodeListe.clear()
        }

        override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
            arbeidsgiverMap["vedtaksperioder"] = vedtaksperiodeListe.toList()
        }

        override fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
            vedtaksperiodeListe.clear()
        }

        override fun preVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak) {
            val vedtaksperiodeMap = mutableMapOf<String, Any?>()
            vedtaksperiodeListe.add(
                mutableMapOf(
                    "vedtaksperiode" to vedtaksperiodeMap,
                    "årsak" to forkastetÅrsak
                )
            )
            pushState(ForkastetVedtaksperiodeState(vedtaksperiodeMap))
        }

        override fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
            arbeidsgiverMap["forkastede"] = vedtaksperiodeListe.toList()
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
            val vedtaksperiodeMap = mutableMapOf<String, Any?>()
            initVedtaksperiodeMap(vedtaksperiodeMap, periode, opprinneligPeriode, hendelseIder, inntektskilde)
            vedtaksperiodeListe.add(vedtaksperiodeMap)
            pushState(VedtaksperiodeState(vedtaksperiode, vedtaksperiodeMap))
        }

        private val feriepengeutbetalingListe = mutableListOf<Map<String, Any>>()

        override fun preVisitFeriepengeutbetalinger(feriepengeutbetalinger: List<Feriepengeutbetaling>) {
            feriepengeutbetalingListe.clear()
        }

        override fun postVisitFeriepengeutbetalinger(feriepengeutbetalinger: List<Feriepengeutbetaling>) {
            arbeidsgiverMap["feriepengeutbetalinger"] = feriepengeutbetalingListe.toList()
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
            val feriepengeutbetalingMap = mutableMapOf<String, Any>(
                "infotrygdFeriepengebeløpPerson" to infotrygdFeriepengebeløpPerson,
                "infotrygdFeriepengebeløpArbeidsgiver" to infotrygdFeriepengebeløpArbeidsgiver,
                "spleisFeriepengebeløpArbeidsgiver" to spleisFeriepengebeløpArbeidsgiver,
                "utbetalingId" to utbetalingId,
                "sendTilOppdrag" to feriepengeutbetaling.sendTilOppdrag
            )
            pushState(OppdragState(feriepengeutbetalingMap))
            pushState(FeriepengeberegnerState(feriepengeutbetalingMap))
            feriepengeutbetalingListe.add(feriepengeutbetalingMap)
        }


        override fun preVisitArbeidsforholdhistorikk(arbeidsforholdhistorikk: Arbeidsforholdhistorikk) {
            val historikk = mutableListOf<Map<String, Any?>>()
            arbeidsgiverMap["arbeidsforholdhistorikk"] = historikk
            pushState(ArbeidsforholdhistorikkState(historikk))
        }

        override fun postVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            popState()
        }
    }

    private class ArbeidsforholdhistorikkState(private val historikk: MutableList<Map<String, Any?>>) : BuilderState() {
        private var arbeidsforholMap: MutableList<Map<String, Any?>> = mutableListOf()

        override fun visitArbeidsforhold(orgnummer: String, fom: LocalDate, tom: LocalDate?) {
            arbeidsforholMap.add(
                mapOf(
                    "orgnummer" to orgnummer,
                    "fom" to fom,
                    "tom" to tom
                )
            )
        }

        override fun postVisitArbeidsforholdinnslag(arbeidsforholdinnslag: Arbeidsforholdhistorikk.Innslag, id: UUID, skjæringstidspunkt: LocalDate) {
            historikk.add(mapOf("id" to id, "skjæringstidspunkt" to skjæringstidspunkt, "arbeidsforhold" to arbeidsforholMap))
            arbeidsforholMap = mutableListOf()
        }

        override fun postVisitArbeidsforholdhistorikk(arbeidsforholdhistorikk: Arbeidsforholdhistorikk) {
            popState()
        }
    }

    private class OppdragState(private val ferieutbetalingMap: MutableMap<String, Any>) : BuilderState() {
        override fun preVisitOppdrag(
            oppdrag: Oppdrag,
            totalBeløp: Int,
            nettoBeløp: Int,
            tidsstempel: LocalDateTime
        ) {
            ferieutbetalingMap["oppdrag"] = oppdrag.toMap()
        }

        override fun postVisitOppdrag(oppdrag: Oppdrag, totalBeløp: Int, nettoBeløp: Int, tidsstempel: LocalDateTime) {
            popState()
        }
    }

    private class FeriepengeberegnerState(private val feriepengeutbetalingMap: MutableMap<String, Any>) : BuilderState() {

        override fun preVisitFeriepengeberegner(
            feriepengeberegner: Feriepengeberegner,
            feriepengedager: List<Feriepengeberegner.UtbetaltDag>,
            opptjeningsår: Year,
            utbetalteDager: List<Feriepengeberegner.UtbetaltDag>
        ) {
            feriepengeutbetalingMap["opptjeningsår"] = opptjeningsår
            pushState(FeriepengerUtbetalteDagerState(feriepengeutbetalingMap, "utbetalteDager"))
            pushState(FeriepengerUtbetalteDagerState(feriepengeutbetalingMap, "feriepengedager"))
        }

        override fun postVisitFeriepengeberegner(feriepengeberegner: Feriepengeberegner) {
            popState()
        }
    }

    private class FeriepengerUtbetalteDagerState(private val ferieutbetalingMap: MutableMap<String, Any>, private val key: String) : BuilderState() {
        private val dager = mutableListOf<Map<String, Any>>()

        override fun preVisitUtbetaleDager() {
            dager.clear()
        }

        override fun preVisitFeriepengedager() {
            dager.clear()
        }

        override fun postVisitUtbetaleDager() {
            ferieutbetalingMap[key] = dager.toList()
            popState()
        }

        override fun postVisitFeriepengedager() {
            ferieutbetalingMap[key] = dager.toList()
            popState()
        }

        override fun visitInfotrygdArbeidsgiverDag(
            infotrygdArbeidsgiver: Feriepengeberegner.UtbetaltDag.InfotrygdArbeidsgiver,
            orgnummer: String,
            dato: LocalDate,
            beløp: Int
        ) {
            leggTilDag("InfotrygdArbeidsgiverDag", orgnummer, dato, beløp)
        }

        override fun visitInfotrygdPersonDag(infotrygdPerson: Feriepengeberegner.UtbetaltDag.InfotrygdPerson, orgnummer: String, dato: LocalDate, beløp: Int) {
            leggTilDag("InfotrygdPersonDag", orgnummer, dato, beløp)
        }

        override fun visitSpleisArbeidsgiverDag(
            spleisArbeidsgiver: Feriepengeberegner.UtbetaltDag.SpleisArbeidsgiver,
            orgnummer: String,
            dato: LocalDate,
            beløp: Int
        ) {
            leggTilDag("SpleisArbeidsgiverDag", orgnummer, dato, beløp)
        }

        private fun leggTilDag(
            type: String,
            orgnummer: String,
            dato: LocalDate,
            beløp: Int
        ) {
            dager.add(
                mapOf(
                    "dato" to dato,
                    "type" to type,
                    "orgnummer" to orgnummer,
                    "beløp" to beløp
                )
            )
        }
    }

    private class InfotrygdhistorikkState(private val historikk: MutableList<Map<String, Any?>>) : BuilderState() {
        override fun preVisitInfotrygdhistorikkElement(
            id: UUID,
            tidsstempel: LocalDateTime,
            oppdatert: LocalDateTime,
            hendelseId: UUID?,
            lagretInntekter: Boolean,
            lagretVilkårsgrunnlag: Boolean,
            harStatslønn: Boolean
        ) {
            val element = mutableMapOf<String, Any?>()
            historikk.add(element)
            pushState(InfotrygdhistorikkElementState(element, id, tidsstempel, oppdatert, hendelseId, lagretInntekter, lagretVilkårsgrunnlag, harStatslønn))
        }

        override fun postVisitInfotrygdhistorikk() {
            popState()
        }
    }

    private class InfotrygdhistorikkElementState(
        element: MutableMap<String, Any?>,
        id: UUID,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        hendelseId: UUID?,
        lagretInntekter: Boolean,
        lagretVilkårsgrunnlag: Boolean,
        harStatslønn: Boolean
    ) : BuilderState() {
        private val ferieperioder = mutableListOf<Map<String, LocalDate>>()
        private val arbeidsgiverutbetalingsperioder = mutableListOf<Map<String, Any>>()
        private val personutbetalingsperioder = mutableListOf<Map<String, Any>>()
        private val ukjenteperioder = mutableListOf<Map<String, LocalDate>>()
        private val inntekter = mutableListOf<Map<String, Any?>>()
        private val arbeidskategorikoder = mutableMapOf<String, LocalDate>()
        private val ugyldigePerioder = mutableListOf<Pair<LocalDate?, LocalDate?>>()

        init {
            element["id"] = id
            element["tidsstempel"] = tidsstempel
            element["hendelseId"] = hendelseId
            element["ferieperioder"] = ferieperioder
            element["arbeidsgiverutbetalingsperioder"] = arbeidsgiverutbetalingsperioder
            element["personutbetalingsperioder"] = personutbetalingsperioder
            element["ukjenteperioder"] = ukjenteperioder
            element["inntekter"] = inntekter
            element["arbeidskategorikoder"] = arbeidskategorikoder
            element["ugyldigePerioder"] = ugyldigePerioder
            element["harStatslønn"] = harStatslønn
            element["lagretInntekter"] = lagretInntekter
            element["lagretVilkårsgrunnlag"] = lagretVilkårsgrunnlag
            element["oppdatert"] = oppdatert
        }

        override fun visitInfotrygdhistorikkFerieperiode(periode: Friperiode) {
            ferieperioder.add(
                mapOf(
                    "fom" to periode.start,
                    "tom" to periode.endInclusive
                )
            )
        }

        override fun visitInfotrygdhistorikkArbeidsgiverUtbetalingsperiode(orgnr: String, periode: Utbetalingsperiode, grad: Prosentdel, inntekt: Inntekt) {
            arbeidsgiverutbetalingsperioder.add(mapOf(
                "orgnr" to orgnr,
                "fom" to periode.start,
                "tom" to periode.endInclusive,
                "grad" to grad.roundToInt(),
                "inntekt" to inntekt.reflection { _, månedlig, _, _ -> månedlig }
            ))
        }

        override fun visitInfotrygdhistorikkPersonUtbetalingsperiode(orgnr: String, periode: Utbetalingsperiode, grad: Prosentdel, inntekt: Inntekt) {
            personutbetalingsperioder.add(mapOf(
                "orgnr" to orgnr,
                "fom" to periode.start,
                "tom" to periode.endInclusive,
                "grad" to grad.roundToInt(),
                "inntekt" to inntekt.reflection { _, månedlig, _, _ -> månedlig }
            ))
        }

        override fun visitInfotrygdhistorikkUkjentPeriode(periode: UkjentInfotrygdperiode) {
            ukjenteperioder.add(
                mapOf(
                    "fom" to periode.start,
                    "tom" to periode.endInclusive
                )
            )
        }

        override fun visitInfotrygdhistorikkInntektsopplysning(
            orgnr: String,
            sykepengerFom: LocalDate,
            inntekt: Inntekt,
            refusjonTilArbeidsgiver: Boolean,
            refusjonTom: LocalDate?,
            lagret: LocalDateTime?
        ) {
            inntekter.add(
                mapOf(
                    "orgnr" to orgnr,
                    "sykepengerFom" to sykepengerFom,
                    "inntekt" to inntekt.reflection { _, månedlig, _, _ -> månedlig },
                    "refusjonTilArbeidsgiver" to refusjonTilArbeidsgiver,
                    "refusjonTom" to refusjonTom,
                    "lagret" to lagret
                )
            )
        }

        override fun visitInfotrygdhistorikkArbeidskategorikoder(arbeidskategorikoder: Map<String, LocalDate>) {
            this.arbeidskategorikoder.putAll(arbeidskategorikoder)
        }

        override fun visitUgyldigePerioder(ugyldigePerioder: List<Pair<LocalDate?, LocalDate?>>) {
            this.ugyldigePerioder.addAll(ugyldigePerioder)
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
            popState()
        }
    }


    private class VilkårsgrunnlagHistorikkState(private val historikk: MutableList<Map<String, Any?>>) : BuilderState() {

        override fun preVisitInnslag(
            innslag: VilkårsgrunnlagHistorikk.Innslag,
            id: UUID,
            opprettet: LocalDateTime
        ) {
            val vilkårsgrunnlag = mutableListOf<Map<String, Any?>>()
            this.historikk.add(
                mutableMapOf(
                    "id" to id,
                    "opprettet" to opprettet,
                    "vilkårsgrunnlag" to vilkårsgrunnlag
                )
            )
            pushState(VilkårsgrunnlagElementState(vilkårsgrunnlag))
        }

        override fun postVisitVilkårsgrunnlagHistorikk() {
            popState()
        }
    }

    private class VilkårsgrunnlagElementState(private val vilkårsgrunnlagElement: MutableList<Map<String, Any?>>) : BuilderState() {

        override fun preVisitGrunnlagsdata(skjæringstidspunkt: LocalDate, grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
            val sykepengegrunnlag = mutableMapOf<String, Any>()
            vilkårsgrunnlagElement.add(
                mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "type" to "Vilkårsprøving",
                    "antallOpptjeningsdagerErMinst" to grunnlagsdata.antallOpptjeningsdagerErMinst,
                    "avviksprosent" to grunnlagsdata.avviksprosent?.ratio(),
                    "sykepengegrunnlag" to sykepengegrunnlag,
                    "sammenligningsgrunnlag" to grunnlagsdata.sammenligningsgrunnlag.reflection { årlig, _, _, _ -> årlig },
                    "harOpptjening" to grunnlagsdata.harOpptjening,
                    "medlemskapstatus" to when (grunnlagsdata.medlemskapstatus) {
                        Medlemskapsvurdering.Medlemskapstatus.Ja -> JsonMedlemskapstatus.JA
                        Medlemskapsvurdering.Medlemskapstatus.Nei -> JsonMedlemskapstatus.NEI
                        Medlemskapsvurdering.Medlemskapstatus.VetIkke -> JsonMedlemskapstatus.VET_IKKE
                    },
                    "harMinimumInntekt" to grunnlagsdata.harMinimumInntekt,
                    "vurdertOk" to grunnlagsdata.vurdertOk,
                    "meldingsreferanseId" to grunnlagsdata.meldingsreferanseId
                )
            )

            pushState(SykepengegrunnlagState(sykepengegrunnlag))
        }

        override fun preVisitInfotrygdVilkårsgrunnlag(
            infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag,
            skjæringstidspunkt: LocalDate,
            sykepengegrunnlag: Sykepengegrunnlag
        ) {
            val sykepengegrunnlag = mutableMapOf<String, Any>()
            vilkårsgrunnlagElement.add(
                mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "type" to "Infotrygd",
                    "sykepengegrunnlag" to sykepengegrunnlag
                )
            )

            pushState(SykepengegrunnlagState(sykepengegrunnlag))
        }


        override fun postVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
            popState()
        }
    }

    class SykepengegrunnlagState(private val sykepengegrunnlag: MutableMap<String, Any>) : BuilderState() {
        override fun preVisitSykepengegrunnlag(sykepengegrunnlag1: Sykepengegrunnlag, sykepengegrunnlag: Inntekt, grunnlagForSykepengegrunnlag: Inntekt) {
            val arbeidsgiverInntektsopplysninger = mutableListOf<Map<String, Any>>()
            this.sykepengegrunnlag.putAll(
                mapOf(
                    "sykepengegrunnlag" to sykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                    "arbeidsgiverInntektsopplysninger" to arbeidsgiverInntektsopplysninger
                )
            )

            pushState(ArbeidsgiverInntektsopplysningerState(arbeidsgiverInntektsopplysninger))
        }

        override fun postVisitGrunnlagsdata(skjæringstidspunkt: LocalDate, grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
            popState()
        }

        override fun postVisitInfotrygdVilkårsgrunnlag(
            skjæringstidspunkt: LocalDate,
            infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag
        ) {
            popState()
        }
    }

    class ArbeidsgiverInntektsopplysningerState(private val arbeidsgiverInntektsopplysninger: MutableList<Map<String, Any>>) : BuilderState() {

        private val inntektsopplysning = mutableMapOf<String, Any?>()

        override fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
            inntektsopplysning.clear()
        }

        override fun visitSaksbehandler(
            saksbehandler: Inntektshistorikk.Saksbehandler,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysning.putAll(saksbehandler.toMap())
        }

        override fun visitInntektsmelding(
            inntektsmelding: Inntektshistorikk.Inntektsmelding,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysning.putAll(inntektsmelding.toMap())
        }

        override fun visitInfotrygd(
            infotrygd: Inntektshistorikk.Infotrygd,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysning.putAll(infotrygd.toMap())
        }

        override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
            val skatteopplysninger = mutableListOf<Map<String, Any?>>()
            this.inntektsopplysning.putAll(
                mutableMapOf(
                    "id" to id,
                    "skatteopplysninger" to skatteopplysninger
                )
            )
            pushState(InntektsendringState(skatteopplysninger))
        }

        override fun postVisitSykepengegrunnlag(sykepengegrunnlag1: Sykepengegrunnlag, sykepengegrunnlag: Inntekt, grunnlagForSykepengegrunnlag: Inntekt) {
            popState()
        }

        override fun postVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
            this.arbeidsgiverInntektsopplysninger.add(
                mapOf(
                    "orgnummer" to orgnummer,
                    "inntektsopplysning" to inntektsopplysning.toMap()
                )
            )
        }

    }

    private class ForkastetVedtaksperiodeState(private val vedtaksperiodeMap: MutableMap<String, Any?>) : BuilderState() {

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
            initVedtaksperiodeMap(vedtaksperiodeMap, periode, opprinneligPeriode, hendelseIder, inntektskilde)
            pushState(VedtaksperiodeState(vedtaksperiode, vedtaksperiodeMap))
        }

        override fun postVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak) {
            popState()
        }
    }

    private class UtbetalingerState(private val utbetalinger: MutableList<MutableMap<String, Any?>>) : BuilderState() {

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
            utbetalinger.add(utbetaling.toMap())
        }

        override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
            popState()
        }
    }

    private class InntektshistorikkState(private val inntekter: MutableList<Map<String, Any?>>) :
        BuilderState() {
        override fun preVisitInnslag(
            innslag: Inntektshistorikk.Innslag,
            id: UUID
        ) {
            val inntektsopplysninger = mutableListOf<Map<String, Any?>>()
            this.inntekter.add(
                mutableMapOf(
                    "id" to id,
                    "inntektsopplysninger" to inntektsopplysninger
                )
            )
            pushState(InntektsendringState(inntektsopplysninger))
        }

        override fun postVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
            popState()
        }
    }

    private class InntektsendringState(private val inntektsopplysninger: MutableList<Map<String, Any?>>) :
        BuilderState() {
        override fun visitSaksbehandler(
            saksbehandler: Inntektshistorikk.Saksbehandler,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(saksbehandler.toMap())
        }

        override fun visitInntektsmelding(
            inntektsmelding: Inntektshistorikk.Inntektsmelding,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(inntektsmelding.toMap())
        }

        override fun visitInfotrygd(
            infotrygd: Inntektshistorikk.Infotrygd,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(infotrygd.toMap())
        }

        override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
            val skatteopplysninger = mutableListOf<Map<String, Any?>>()
            this.inntektsopplysninger.add(
                mutableMapOf(
                    "id" to id,
                    "skatteopplysninger" to skatteopplysninger
                )
            )
            pushState(InntektsendringState(skatteopplysninger))
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
            inntektsopplysninger.add(sykepengegrunnlag.toMap(Inntektsopplysningskilde.SKATT_SYKEPENGEGRUNNLAG))
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
            inntektsopplysninger.add(sammenligningsgrunnlag.toMap(Inntektsopplysningskilde.SKATT_SAMMENLIGNINGSGRUNNLAG))
        }

        override fun postVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) = popState()
        override fun postVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) = popState()
    }

    private class VedtaksperiodeState(
        vedtaksperiode: Vedtaksperiode,
        private val vedtaksperiodeMap: MutableMap<String, Any?>
    ) : BuilderState() {
        init {
            vedtaksperiodeMap.putAll(vedtaksperiode.toMap())
        }

        private var inUtbetaling = false

        override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
            val sykdomstidslinje = mutableMapOf<String, Any?>()

            vedtaksperiodeMap["sykdomstidslinje"] = sykdomstidslinje
            pushState(SykdomstidslinjeState(sykdomstidslinje))
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
            inUtbetaling = true
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
            inUtbetaling = false
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
            popState()
        }
    }

    private class SykdomshistorikkState(
        private val sykdomshistorikkElementer: MutableList<MutableMap<String, Any?>>
    ) : BuilderState() {
        override fun preVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID,
            hendelseId: UUID?,
            tidsstempel: LocalDateTime
        ) {
            val elementMap = mutableMapOf<String, Any?>()
            sykdomshistorikkElementer.add(elementMap)

            pushState(SykdomshistorikkElementState(id, hendelseId, tidsstempel, elementMap))
        }

        override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
            popState()
        }
    }

    private class SykdomshistorikkElementState(
        id: UUID,
        hendelseId: UUID?,
        tidsstempel: LocalDateTime,
        private val elementMap: MutableMap<String, Any?>
    ) : BuilderState() {
        init {
            elementMap["id"] = id
            elementMap["hendelseId"] = hendelseId
            elementMap["tidsstempel"] = tidsstempel
        }

        override fun preVisitHendelseSykdomstidslinje(
            tidslinje: Sykdomstidslinje,
            hendelseId: UUID?,
            tidsstempel: LocalDateTime
        ) {
            val sykdomstidslinje = mutableMapOf<String, Any?>()
            elementMap["hendelseSykdomstidslinje"] = sykdomstidslinje
            pushState(SykdomstidslinjeState(sykdomstidslinje))
        }

        override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            val sykdomstidslinje = mutableMapOf<String, Any?>()
            elementMap["beregnetSykdomstidslinje"] = sykdomstidslinje
            pushState(SykdomstidslinjeState(sykdomstidslinje))
        }

        override fun postVisitSykdomshistorikkElement(
            element: Sykdomshistorikk.Element,
            id: UUID,
            hendelseId: UUID?,
            tidsstempel: LocalDateTime
        ) {
            popState()
        }
    }

    private class SykdomstidslinjeState(private val sykdomstidslinje: MutableMap<String, Any?>) : BuilderState() {
        private val dateRanges = DateRanges()

        override fun postVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: MutableList<Periode>) {
            sykdomstidslinje["låstePerioder"] = låstePerioder.map {
                mapOf("fom" to it.start, "tom" to it.endInclusive)
            }
            sykdomstidslinje["periode"] = tidslinje.periode()?.let { mapOf("fom" to it.start, "tom" to it.endInclusive) }

            sykdomstidslinje["dager"] = dateRanges.toList()
            popState()
        }

        override fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) = leggTilDag(dato, dag, kilde)

        override fun visitDag(
            dag: Arbeidsgiverdag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dato, dag, kilde)

        override fun visitDag(dag: Feriedag, dato: LocalDate, kilde: Hendelseskilde) = leggTilDag(dato, dag, kilde)

        override fun visitDag(dag: Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) = leggTilDag(dato, dag, kilde)

        override fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) = leggTilDag(dato, dag, kilde)

        override fun visitDag(
            dag: ArbeidsgiverHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dato, dag, kilde)

        override fun visitDag(
            dag: Sykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dato, dag, kilde)

        override fun visitDag(
            dag: ForeldetSykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dato, dag, kilde)

        override fun visitDag(
            dag: SykHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dato, dag, kilde)

        override fun visitDag(
            dag: ProblemDag,
            dato: LocalDate,
            kilde: Hendelseskilde,
            melding: String
        ) = leggTilDag(dato, dag, kilde, melding)

        private fun leggTilDag(
            dato: LocalDate,
            dag: Dag,
            kilde: Hendelseskilde,
            melding: String? = null
        ) {
            dateRanges.plus(dato, dag.serialiser(kilde.toJson(), melding))
        }
    }
}

