package no.nav.helse.serde

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.*
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.UgyldigPeriode
import no.nav.helse.person.infotrygdhistorikk.UkjentInfotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.serde.PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType
import no.nav.helse.serde.PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.*
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData.TypeData
import no.nav.helse.serde.api.builders.BuilderState
import no.nav.helse.serde.mapping.JsonMedlemskapstatus
import no.nav.helse.serde.reflection.AktivitetsloggMap
import no.nav.helse.serde.reflection.Inntektsopplysningskilde
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsgiverdag
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjeberegning
import no.nav.helse.økonomi.*
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
        dødsdato: LocalDate?,
        vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
    ) {
        personBuilder = PersonState(fødselsnummer, aktørId, opprettet, dødsdato)
        pushState(personBuilder)
    }

    private class PersonState(fødselsnummer: Fødselsnummer, aktørId: String, opprettet: LocalDateTime, dødsdato: LocalDate?) : BuilderState() {
        private val personMap = mutableMapOf<String, Any?>(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer.toString(),
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
            pushState(ArbeidsgiverState(arbeidsgiverMap))
        }

        override fun postVisitPerson(
            person: Person,
            opprettet: LocalDateTime,
            aktørId: String,
            fødselsnummer: Fødselsnummer,
            dødsdato: LocalDate?,
            vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
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

    private class ArbeidsgiverState(private val arbeidsgiverMap: MutableMap<String, Any?>) : BuilderState() {
        private val beregningerList = mutableListOf<Map<String, Any?>>()
        private val refusjonOpphører = mutableListOf<LocalDate?>()
        private val feriepengeutbetalingListe = mutableListOf<Map<String, Any>>()

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

        override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
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
            skjæringstidspunktFraInfotrygd: LocalDate?,
            periodetype: Periodetype,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: Set<UUID>,
            inntektsmeldingInfo: InntektsmeldingInfo?,
            inntektskilde: Inntektskilde
        ) {
            val vedtaksperiodeMap = mutableMapOf<String, Any?>()
            vedtaksperiodeListe.add(vedtaksperiodeMap)
            pushState(VedtaksperiodeState(
                vedtaksperiodeMap = vedtaksperiodeMap
            ))
        }

        override fun preVisitUtbetalingstidslinjeberegninger(beregninger: List<Utbetalingstidslinjeberegning>) {
            pushState(UtbetalingstidslinjeberegningerState(beregningerList))
        }

        override fun visitRefusjonOpphører(refusjonOpphører: List<LocalDate?>) {
            this.refusjonOpphører.addAll(refusjonOpphører)
        }

        override fun preVisitFeriepengeutbetalinger(feriepengeutbetalinger: List<Feriepengeutbetaling>) {
            pushState(FeriepengeutbetalingerState(feriepengeutbetalingListe))
        }

        override fun preVisitRefusjonshistorikk(refusjonshistorikk: Refusjonshistorikk) {
            val historikk = mutableListOf<Map<String, Any?>>()
            arbeidsgiverMap["refusjonshistorikk"] = historikk
            pushState(RefusjonshistorikkState(historikk))
        }

        override fun preVisitArbeidsforholdhistorikk(arbeidsforholdhistorikk: Arbeidsforholdhistorikk) {
            val historikk = mutableListOf<Map<String, Any?>>()
            arbeidsgiverMap["arbeidsforholdhistorikk"] = historikk
            pushState(ArbeidsforholdhistorikkState(historikk))
        }

        override fun preVisitInntektsmeldinginfoHistorikk(inntektsmeldingInfoHistorikk: InntektsmeldingInfoHistorikk) {
            val historikk = mutableListOf<Map<String, Any>>()
            arbeidsgiverMap["inntektsmeldingInfo"] = historikk
            pushState(InntektsmeldingInfoHistorikkState(historikk))
        }

        override fun postVisitArbeidsgiver(
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            organisasjonsnummer: String
        ) {
            arbeidsgiverMap["id"] = id
            arbeidsgiverMap["organisasjonsnummer"] = organisasjonsnummer
            arbeidsgiverMap["beregnetUtbetalingstidslinjer"] = beregningerList
            arbeidsgiverMap["refusjonOpphører"] = refusjonOpphører
            arbeidsgiverMap["feriepengeutbetalinger"] = feriepengeutbetalingListe.toList()
            popState()
        }
    }

    private class UtbetalingstidslinjeberegningerState(private val beregninger: MutableList<Map<String, Any?>>) : BuilderState() {
        private val utbetalingstidslinjeMap = mutableMapOf<String, Any>()

        override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
            pushState(UtbetalingstidslinjeState(utbetalingstidslinjeMap))
        }

        override fun postVisitUtbetalingstidslinjeberegning(
            id: UUID,
            tidsstempel: LocalDateTime,
            organisasjonsnummer: String,
            sykdomshistorikkElementId: UUID,
            inntektshistorikkInnslagId: UUID,
            vilkårsgrunnlagHistorikkInnslagId: UUID
        ) {
            beregninger.add(mapOf(
                "id" to id,
                "sykdomshistorikkElementId" to sykdomshistorikkElementId,
                "vilkårsgrunnlagHistorikkInnslagId" to vilkårsgrunnlagHistorikkInnslagId,
                "inntektshistorikkInnslagId" to inntektshistorikkInnslagId,
                "tidsstempel" to tidsstempel,
                "organisasjonsnummer" to organisasjonsnummer,
                "utbetalingstidslinje" to utbetalingstidslinjeMap
            ))
        }

        override fun postVisitUtbetalingstidslinjeberegninger(beregninger: List<Utbetalingstidslinjeberegning>) {
            popState()
        }
    }

    private class FeriepengeutbetalingerState(private val feriepengeutbetalinger: MutableList<Map<String, Any>>) : BuilderState() {

        override fun preVisitFeriepengeutbetaling(
            feriepengeutbetaling: Feriepengeutbetaling,
            infotrygdFeriepengebeløpPerson: Double,
            infotrygdFeriepengebeløpArbeidsgiver: Double,
            spleisFeriepengebeløpArbeidsgiver: Double,
            overføringstidspunkt: LocalDateTime?,
            avstemmingsnøkkel: Long?,
            utbetalingId: UUID
        ) {
            val feriepengeutbetalingMap = mutableMapOf<String, Any>()
            feriepengeutbetalinger.add(feriepengeutbetalingMap)
            pushState(FeriepengeutbetalingState(feriepengeutbetalingMap))
        }

        override fun postVisitFeriepengeutbetalinger(feriepengeutbetalinger: List<Feriepengeutbetaling>) {
            popState()
        }
    }

    private class FeriepengeutbetalingState(private val feriepengeutbetalingMap: MutableMap<String, Any>) : BuilderState() {
        private val oppdragMap = mutableMapOf<String, Any?>()

        override fun preVisitFeriepengeberegner(
            feriepengeberegner: Feriepengeberegner,
            feriepengedager: List<Feriepengeberegner.UtbetaltDag>,
            opptjeningsår: Year,
            utbetalteDager: List<Feriepengeberegner.UtbetaltDag>
        ) {
            pushState(FeriepengeberegnerState(feriepengeutbetalingMap))
        }

        override fun preVisitOppdrag(
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
            pushState(OppdragState(oppdragMap))
        }

        override fun postVisitFeriepengeutbetaling(
            feriepengeutbetaling: Feriepengeutbetaling,
            infotrygdFeriepengebeløpPerson: Double,
            infotrygdFeriepengebeløpArbeidsgiver: Double,
            spleisFeriepengebeløpArbeidsgiver: Double,
            overføringstidspunkt: LocalDateTime?,
            avstemmingsnøkkel: Long?,
            utbetalingId: UUID
        ) {
            feriepengeutbetalingMap["infotrygdFeriepengebeløpPerson"] = infotrygdFeriepengebeløpPerson
            feriepengeutbetalingMap["infotrygdFeriepengebeløpArbeidsgiver"] = infotrygdFeriepengebeløpArbeidsgiver
            feriepengeutbetalingMap["spleisFeriepengebeløpArbeidsgiver"] = spleisFeriepengebeløpArbeidsgiver
            feriepengeutbetalingMap["utbetalingId"] = utbetalingId
            feriepengeutbetalingMap["sendTilOppdrag"] = feriepengeutbetaling.sendTilOppdrag
            feriepengeutbetalingMap["oppdrag"] = oppdragMap
            popState()
        }
    }

    private class RefusjonshistorikkState(private val historikk: MutableList<Map<String, Any?>>) : BuilderState() {
        private var endringerIRefusjon: MutableList<Map<String, Any?>> = mutableListOf()

        override fun preVisitRefusjon(
            meldingsreferanseId: UUID,
            førsteFraværsdag: LocalDate?,
            arbeidsgiverperioder: List<Periode>,
            beløp: Inntekt?,
            sisteRefusjonsdag: LocalDate?,
            endringerIRefusjon: List<Refusjonshistorikk.Refusjon.EndringIRefusjon>,
            tidsstempel: LocalDateTime
        ) {
            this.endringerIRefusjon = mutableListOf()
            historikk.add(
                mapOf(
                    "meldingsreferanseId" to meldingsreferanseId,
                    "førsteFraværsdag" to førsteFraværsdag,
                    "arbeidsgiverperioder" to arbeidsgiverperioder.map { mapOf("fom" to it.start, "tom" to it.endInclusive) },
                    "beløp" to beløp?.reflection { _, månedlig, _, _ -> månedlig },
                    "sisteRefusjonsdag" to sisteRefusjonsdag,
                    "endringerIRefusjon" to this.endringerIRefusjon,
                    "tidsstempel" to tidsstempel
                )
            )
        }

        override fun visitEndringIRefusjon(beløp: Inntekt, endringsdato: LocalDate) {
            endringerIRefusjon.add(
                mapOf(
                    "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
                    "endringsdato" to endringsdato
                )
            )
        }

        override fun postVisitRefusjonshistorikk(refusjonshistorikk: Refusjonshistorikk) {
            popState()
        }
    }

    private class ArbeidsforholdhistorikkState(private val historikk: MutableList<Map<String, Any?>>) : BuilderState() {
        private var arbeidsforholMap: MutableList<Map<String, Any?>> = mutableListOf()

        override fun visitArbeidsforhold(ansattFom: LocalDate, ansattTom: LocalDate?, erAktivt: Boolean) {
            arbeidsforholMap.add(
                mapOf(
                    "ansattFom" to ansattFom,
                    "ansattTom" to ansattTom,
                    "erAktivt" to erAktivt
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

    private class InntektsmeldingInfoHistorikkState(private val historikk: MutableList<Map<String, Any>>) : BuilderState() {

        override fun preVisitInntektsmeldinginfoElement(dato: LocalDate, elementer: List<InntektsmeldingInfo>) {
            val element = mutableMapOf<String, Any>()
            historikk.add(element)
            pushState(InntektsmeldingInfoHistorikkElementState(dato, element))
        }

        override fun postVisitInntektsmeldinginfoHistorikk(inntektsmeldingInfoHistorikk: InntektsmeldingInfoHistorikk) {
            popState()
        }

        private class InntektsmeldingInfoHistorikkElementState(dato: LocalDate, element: MutableMap<String, Any>) : BuilderState() {
            private val elementer = mutableListOf<Map<String, Any>>()

            init {
                element["dato"] = dato
                element["inntektsmeldinger"] = elementer
            }

            override fun visitInntektsmeldinginfo(id: UUID, arbeidsforholdId: String?) {
                elementer.add(mutableMapOf<String, Any>("id" to id).apply {
                    compute("arbeidsforholdId") { _, _ -> arbeidsforholdId }
                })
            }

            override fun postVisitInntektsmeldinginfoElement(dato: LocalDate, elementer: List<InntektsmeldingInfo>) {
                popState()
            }
        }
    }

    private class OppdragState(private val oppdragMap: MutableMap<String, Any?>) : BuilderState() {
        private val linjer = mutableListOf<Map<String, Any?>>()

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
            linjer.add(mapOf(
                "fom" to fom,
                "tom" to tom,
                "satstype" to "$satstype",
                "sats" to beløp,
                "lønn" to aktuellDagsinntekt,
                "grad" to grad,
                "stønadsdager" to stønadsdager,
                "totalbeløp" to totalbeløp,
                "endringskode" to endringskode,
                "delytelseId" to delytelseId,
                "refDelytelseId" to refDelytelseId,
                "refFagsystemId" to refFagsystemId,
                "statuskode" to statuskode,
                "datoStatusFom" to datoStatusFom,
                "klassekode" to klassekode.verdi
            ))
        }

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
            oppdragMap["mottaker"] = mottaker
            oppdragMap["fagområde"] = "$fagområde"
            oppdragMap["linjer"] = linjer
            oppdragMap["fagsystemId"] = fagsystemId
            oppdragMap["endringskode"] = "$endringskode"
            oppdragMap["sisteArbeidsgiverdag"] = sisteArbeidsgiverdag
            oppdragMap["tidsstempel"] = tidsstempel
            oppdragMap["nettoBeløp"] = nettoBeløp
            oppdragMap["stønadsdager"] = stønadsdager
            oppdragMap["avstemmingsnøkkel"] = avstemmingsnøkkel?.let { "$it" }
            oppdragMap["status"] = status?.let { "$it" }
            oppdragMap["overføringstidspunkt"] = overføringstidspunkt
            oppdragMap["fom"] = førstedato
            oppdragMap["tom"] = sistedato
            oppdragMap["erSimulert"] = erSimulert
            oppdragMap["simuleringsResultat"] = simuleringsResultat?.toMap()

            popState()
        }
    }

    private class FeriepengeberegnerState(private val feriepengeutbetalingMap: MutableMap<String, Any>) : BuilderState() {
        private val utbetalteDager = mutableListOf<Map<String, Any>>()
        private val feriepengedager = mutableListOf<Map<String, Any>>()

        override fun preVisitUtbetaleDager() {
            pushState(FeriepengerUtbetalteDagerState(utbetalteDager))
        }

        override fun preVisitFeriepengedager() {
            pushState(FeriepengerUtbetalteDagerState(feriepengedager))
        }

        override fun postVisitFeriepengeberegner(
            feriepengeberegner: Feriepengeberegner,
            feriepengedager: List<Feriepengeberegner.UtbetaltDag>,
            opptjeningsår: Year,
            utbetalteDager: List<Feriepengeberegner.UtbetaltDag>
        ) {
            feriepengeutbetalingMap["opptjeningsår"] = opptjeningsår
            feriepengeutbetalingMap["utbetalteDager"] = this.utbetalteDager
            feriepengeutbetalingMap["feriepengedager"] = this.feriepengedager
            popState()
        }
    }

    private class FeriepengerUtbetalteDagerState(private val dager: MutableList<Map<String, Any>>) : BuilderState() {
        override fun postVisitUtbetaleDager() {
            popState()
        }

        override fun postVisitFeriepengedager() {
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
        private val ugyldigePerioder = mutableListOf<Map<String, Any?>>()

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

        override fun visitUgyldigePerioder(ugyldigePerioder: List<UgyldigPeriode>) {
            this.ugyldigePerioder.addAll(ugyldigePerioder.map { it.toMap() })
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

        override fun preVisitGrunnlagsdata(
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
        ) {
            pushState(GrunnlagsdataState(vilkårsgrunnlagElement))
        }

        override fun preVisitInfotrygdVilkårsgrunnlag(
            infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag,
            skjæringstidspunkt: LocalDate,
            sykepengegrunnlag: Sykepengegrunnlag,
            vilkårsgrunnlagId: UUID
        ) {
            val sykepengegrunnlagMap = mutableMapOf<String, Any>()
            vilkårsgrunnlagElement.add(
                mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "type" to "Infotrygd",
                    "sykepengegrunnlag" to sykepengegrunnlagMap,
                    "vilkårsgrunnlagId" to vilkårsgrunnlagId
                )
            )

            pushState(SykepengegrunnlagState(sykepengegrunnlagMap))
        }


        override fun postVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
            popState()
        }
    }

    class GrunnlagsdataState(private val vilkårsgrunnlagElement: MutableList<Map<String, Any?>>) : BuilderState() {
        private val sykepengegrunnlagMap = mutableMapOf<String, Any>()
        private val sammenligningsgrunnlagMap = mutableMapOf<String, Any>()
        override fun preVisitSykepengegrunnlag(
            sykepengegrunnlag1: Sykepengegrunnlag,
            sykepengegrunnlag: Inntekt,
            grunnlagForSykepengegrunnlag: Inntekt,
            begrensning: Sykepengegrunnlag.Begrensning
        ) {
            pushState(SykepengegrunnlagState(sykepengegrunnlagMap))
        }

        override fun preVisitSammenligningsgrunnlag(sammenligningsgrunnlag1: Sammenligningsgrunnlag, sammenligningsgrunnlag: Inntekt) {
            pushState(SammenlingningsgrunnlagState(sammenligningsgrunnlagMap))
        }

        override fun postVisitGrunnlagsdata(
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
        ) {
            vilkårsgrunnlagElement.add(
                mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "type" to "Vilkårsprøving",
                    "antallOpptjeningsdagerErMinst" to antallOpptjeningsdagerErMinst,
                    "avviksprosent" to avviksprosent?.ratio(),
                    "sykepengegrunnlag" to sykepengegrunnlagMap,
                    "sammenligningsgrunnlag" to sammenligningsgrunnlagMap,
                    "harOpptjening" to harOpptjening,
                    "medlemskapstatus" to when (medlemskapstatus) {
                        Medlemskapsvurdering.Medlemskapstatus.Ja -> JsonMedlemskapstatus.JA
                        Medlemskapsvurdering.Medlemskapstatus.Nei -> JsonMedlemskapstatus.NEI
                        Medlemskapsvurdering.Medlemskapstatus.VetIkke -> JsonMedlemskapstatus.VET_IKKE
                    },
                    "harMinimumInntekt" to harMinimumInntekt,
                    "vurdertOk" to vurdertOk,
                    "meldingsreferanseId" to meldingsreferanseId,
                    "vilkårsgrunnlagId" to vilkårsgrunnlagId
                )
            )
            popState()
        }
    }


    class SykepengegrunnlagState(private val sykepengegrunnlag: MutableMap<String, Any>) : BuilderState() {
        val arbeidsgiverInntektsopplysninger = mutableListOf<Map<String, Any>>()

        override fun postVisitSykepengegrunnlag(
            sykepengegrunnlag1: Sykepengegrunnlag,
            sykepengegrunnlag: Inntekt,
            grunnlagForSykepengegrunnlag: Inntekt,
            begrensning: Sykepengegrunnlag.Begrensning
        ) {

            this.sykepengegrunnlag.putAll(
                mapOf(
                    "sykepengegrunnlag" to sykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                    "arbeidsgiverInntektsopplysninger" to arbeidsgiverInntektsopplysninger,
                    "begrensning" to begrensning
                )
            )
            popState()
        }

        override fun preVisitArbeidsgiverInntektsopplysninger() {
            pushState(ArbeidsgiverInntektsopplysningerState(arbeidsgiverInntektsopplysninger))
        }
    }

    class SammenlingningsgrunnlagState(private val sammenligningsgrunnlag: MutableMap<String, Any>) : BuilderState() {
        val arbeidsgiverInntektsopplysninger = mutableListOf<Map<String, Any>>()

        override fun postVisitSammenligningsgrunnlag(sammenligningsgrunnlag1: Sammenligningsgrunnlag, sammenligningsgrunnlag: Inntekt) {
            this.sammenligningsgrunnlag.putAll(
                mapOf(
                    "sammenligningsgrunnlag" to sammenligningsgrunnlag.reflection { årlig, _, _, _ -> årlig },
                    "arbeidsgiverInntektsopplysninger" to arbeidsgiverInntektsopplysninger,
                )
            )
            popState()
        }

        override fun preVisitArbeidsgiverInntektsopplysninger() {
            pushState(ArbeidsgiverInntektsopplysningerState(arbeidsgiverInntektsopplysninger))
        }
    }

    class ArbeidsgiverInntektsopplysningerState(private val arbeidsgiverInntektsopplysninger: MutableList<Map<String, Any>>) : BuilderState() {

        private val inntektsopplysninger = mutableMapOf<String, Any?>()

        override fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
            inntektsopplysninger.clear()
        }

        override fun visitSaksbehandler(
            saksbehandler: Inntektshistorikk.Saksbehandler,
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.putAll(mapOf(
                "id" to id,
                "dato" to dato,
                "hendelseId" to hendelseId,
                "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
                "kilde" to Inntektsopplysningskilde.SAKSBEHANDLER,
                "tidsstempel" to tidsstempel
            ))
        }

        override fun visitInntektsmelding(
            inntektsmelding: Inntektshistorikk.Inntektsmelding,
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.putAll(mapOf(
                "id" to id,
                "dato" to dato,
                "hendelseId" to hendelseId,
                "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
                "kilde" to Inntektsopplysningskilde.INNTEKTSMELDING,
                "tidsstempel" to tidsstempel
            ))
        }

        override fun visitIkkeRapportert(id: UUID, dato: LocalDate, tidsstempel: LocalDateTime) {
            inntektsopplysninger.putAll(mapOf(
                "id" to id,
                "dato" to dato,
                "kilde" to Inntektsopplysningskilde.IKKE_RAPPORTERT,
                "tidsstempel" to tidsstempel
            ))
        }

        override fun visitInfotrygd(
            infotrygd: Inntektshistorikk.Infotrygd,
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.putAll(mapOf(
                "id" to id,
                "dato" to dato,
                "hendelseId" to hendelseId,
                "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
                "kilde" to Inntektsopplysningskilde.INFOTRYGD,
                "tidsstempel" to tidsstempel
            )
            )
        }

        override fun preVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) {
            val skatteopplysninger = mutableListOf<Map<String, Any?>>()
            this.inntektsopplysninger.putAll(
                mutableMapOf(
                    "id" to id,
                    "skatteopplysninger" to skatteopplysninger
                )
            )
            pushState(InntektsendringState(skatteopplysninger))
        }

        override fun postVisitArbeidsgiverInntektsopplysninger() {
            popState()
        }

        override fun postVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
            this.arbeidsgiverInntektsopplysninger.add(
                mapOf(
                    "orgnummer" to orgnummer,
                    "inntektsopplysning" to inntektsopplysninger.toMap()
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
            skjæringstidspunktFraInfotrygd: LocalDate?,
            periodetype: Periodetype,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: Set<UUID>,
            inntektsmeldingInfo: InntektsmeldingInfo?,
            inntektskilde: Inntektskilde
        ) {
            pushState(VedtaksperiodeState(
                vedtaksperiodeMap = vedtaksperiodeMap
            ))
        }

        override fun postVisitForkastetPeriode(vedtaksperiode: Vedtaksperiode, forkastetÅrsak: ForkastetÅrsak) {
            popState()
        }
    }

    private class UtbetalingerState(private val utbetalinger: MutableList<MutableMap<String, Any?>>) : BuilderState() {

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
            val utbetalingMap = mutableMapOf<String, Any?>()
            utbetalinger.add(utbetalingMap)
            pushState(UtbetalingState(utbetalingMap))
        }

        override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
            popState()
        }
    }

    private class UtbetalingState(private val utbetalingMap: MutableMap<String, Any?>) : BuilderState() {
        private val utbetalingstidslinjeMap = mutableMapOf<String, Any>()
        private val arbeidsgiverOppdragMap = mutableMapOf<String, Any?>()
        private val personOppdragMap = mutableMapOf<String, Any?>()
        private var vurderingMap: Map<String, Any?>? = null

        override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
            pushState(UtbetalingstidslinjeState(utbetalingstidslinjeMap))
        }

        override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
            pushState(OppdragState(arbeidsgiverOppdragMap))
        }

        override fun preVisitPersonOppdrag(oppdrag: Oppdrag) {
            pushState(OppdragState(personOppdragMap))
        }

        override fun visitVurdering(
            vurdering: Utbetaling.Vurdering,
            ident: String,
            epost: String,
            tidspunkt: LocalDateTime,
            automatiskBehandling: Boolean,
            godkjent: Boolean
        ) {
            vurderingMap = mapOf<String, Any?>(
                "godkjent" to godkjent,
                "ident" to ident,
                "epost" to epost,
                "tidspunkt" to tidspunkt,
                "automatiskBehandling" to automatiskBehandling
            )
        }

        override fun postVisitUtbetaling(
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
            utbetalingMap["id"] = id
            utbetalingMap["korrelasjonsId"] = korrelasjonsId
            utbetalingMap["beregningId"] = beregningId
            utbetalingMap["utbetalingstidslinje"] = utbetalingstidslinjeMap
            utbetalingMap["arbeidsgiverOppdrag"] = arbeidsgiverOppdragMap
            utbetalingMap["personOppdrag"] = personOppdragMap
            utbetalingMap["fom"] = periode.start
            utbetalingMap["tom"] = periode.endInclusive
            utbetalingMap["stønadsdager"] = stønadsdager
            utbetalingMap["tidsstempel"] = tidsstempel
            utbetalingMap["status"] = Utbetalingstatus.fraTilstand(tilstand)
            utbetalingMap["type"] = type
            utbetalingMap["maksdato"] = maksdato
            utbetalingMap["forbrukteSykedager"] = forbrukteSykedager
            utbetalingMap["gjenståendeSykedager"] = gjenståendeSykedager
            utbetalingMap["vurdering"] = vurderingMap
            utbetalingMap["overføringstidspunkt"] = overføringstidspunkt
            utbetalingMap["avstemmingsnøkkel"] = avstemmingsnøkkel?.let { "$it" }
            utbetalingMap["avsluttet"] = avsluttet
            utbetalingMap["oppdatert"] = oppdatert
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
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(mapOf(
                "id" to id,
                "dato" to dato,
                "hendelseId" to hendelseId,
                "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
                "kilde" to Inntektsopplysningskilde.SAKSBEHANDLER,
                "tidsstempel" to tidsstempel
            ))
        }

        override fun visitInntektsmelding(
            inntektsmelding: Inntektshistorikk.Inntektsmelding,
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(mapOf(
                "id" to id,
                "dato" to dato,
                "hendelseId" to hendelseId,
                "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
                "kilde" to Inntektsopplysningskilde.INNTEKTSMELDING,
                "tidsstempel" to tidsstempel
            ))
        }

        override fun visitInfotrygd(
            infotrygd: Inntektshistorikk.Infotrygd,
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            inntektsopplysninger.add(mapOf(
                "id" to id,
                "dato" to dato,
                "hendelseId" to hendelseId,
                "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
                "kilde" to Inntektsopplysningskilde.INFOTRYGD,
                "tidsstempel" to tidsstempel
            ))
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
            inntektsopplysninger.add(skattMap(
                Inntektsopplysningskilde.SKATT_SYKEPENGEGRUNNLAG,
                dato,
                hendelseId,
                beløp,
                måned,
                type,
                fordel,
                beskrivelse,
                tidsstempel
            ))
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
            inntektsopplysninger.add(skattMap(
                Inntektsopplysningskilde.SKATT_SAMMENLIGNINGSGRUNNLAG,
                dato,
                hendelseId,
                beløp,
                måned,
                type,
                fordel,
                beskrivelse,
                tidsstempel
            ))
        }

        private fun skattMap(
            kilde: Inntektsopplysningskilde,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Inntektshistorikk.Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) =
            mapOf(
                "dato" to dato,
                "hendelseId" to hendelseId,
                "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
                "kilde" to kilde,
                "tidsstempel" to tidsstempel,
                "måned" to måned,
                "type" to type,
                "fordel" to fordel,
                "beskrivelse" to beskrivelse
            )

        override fun postVisitSkatt(skattComposite: Inntektshistorikk.SkattComposite, id: UUID, dato: LocalDate) = popState()
        override fun postVisitInnslag(innslag: Inntektshistorikk.Innslag, id: UUID) = popState()
    }

    private class VedtaksperiodeState(private val vedtaksperiodeMap: MutableMap<String, Any?>) : BuilderState() {
        private val utbetalinger = mutableListOf<UUID>()

        override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
            val sykdomstidslinje = mutableMapOf<String, Any?>()

            vedtaksperiodeMap["sykdomstidslinje"] = sykdomstidslinje
            pushState(SykdomstidslinjeState(sykdomstidslinje))
        }

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
            utbetalinger.add(id)
            pushState(UtbetalingState(mutableMapOf()))
        }

        override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
            val utbetalingstidslinjeMap = mutableMapOf<String, Any>()
            vedtaksperiodeMap["utbetalingstidslinje"] = utbetalingstidslinjeMap
            pushState(UtbetalingstidslinjeState(utbetalingstidslinjeMap))
        }

        override fun postVisitVedtakserperiodeUtbetalinger(utbetalinger: List<Utbetaling>) {
            vedtaksperiodeMap["utbetalinger"] = this.utbetalinger
        }

        override fun visitInntektsmeldinginfo(id: UUID, arbeidsforholdId: String?) {
            vedtaksperiodeMap["inntektsmeldingInfo"] = mapOf("id" to id, "arbeidsforholdId" to arbeidsforholdId)
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
            skjæringstidspunktFraInfotrygd: LocalDate?,
            periodetype: Periodetype,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: Set<UUID>,
            inntektsmeldingInfo: InntektsmeldingInfo?,
            inntektskilde: Inntektskilde
        ) {
            vedtaksperiodeMap.putAll(mutableMapOf(
                "id" to id,
                "fom" to periode.start,
                "tom" to periode.endInclusive,
                "sykmeldingFom" to opprinneligPeriode.start,
                "sykmeldingTom" to opprinneligPeriode.endInclusive,
                "hendelseIder" to hendelseIder,
                "periodetype" to periodetype,
                "inntektskilde" to inntektskilde,
                "tilstand" to tilstand.type.name,
                "skjæringstidspunktFraInfotrygd" to skjæringstidspunktFraInfotrygd,
                "skjæringstidspunkt" to skjæringstidspunkt,
                "forlengelseFraInfotrygd" to forlengelseFraInfotrygd,
                "opprettet" to opprettet,
                "oppdatert" to oppdatert
            ))

            popState()
        }
    }

    private class UtbetalingstidslinjeState(private val utbetalingstidslinjeMap: MutableMap<String, Any>) : BuilderState() {
        private val dager = DateRanges()

        private fun leggTilDag(dato: LocalDate, builder: UtbetalingsdagJsonBuilder) {
            dager.plus(dato, builder.build())
        }

        override fun visit(dag: ArbeidsgiverperiodeDag, dato: LocalDate, økonomi: Økonomi) {
            leggTilDag(dato, UtbetalingsdagJsonBuilder(TypeData.ArbeidsgiverperiodeDag).økonomi(økonomi))
        }

        override fun visit(dag: NavDag, dato: LocalDate, økonomi: Økonomi) {
            leggTilDag(dato, UtbetalingsdagJsonBuilder(TypeData.NavDag).økonomi(økonomi))
        }

        override fun visit(dag: NavHelgDag, dato: LocalDate, økonomi: Økonomi) {
            leggTilDag(dato, UtbetalingsdagJsonBuilder(TypeData.NavHelgDag).økonomi(økonomi))
        }

        override fun visit(dag: Utbetalingsdag.Arbeidsdag, dato: LocalDate, økonomi: Økonomi) {
            leggTilDag(dato, UtbetalingsdagJsonBuilder(TypeData.Arbeidsdag).økonomi(økonomi))
        }

        override fun visit(dag: Fridag, dato: LocalDate, økonomi: Økonomi) {
            leggTilDag(dato, UtbetalingsdagJsonBuilder(TypeData.Fridag).økonomi(økonomi))
        }

        override fun visit(dag: AvvistDag, dato: LocalDate, økonomi: Økonomi) {
            leggTilDag(dato, UtbetalingsdagJsonBuilder(TypeData.AvvistDag).økonomi(økonomi).begrunnelser(dag.begrunnelser))
        }

        override fun visit(dag: ForeldetDag, dato: LocalDate, økonomi: Økonomi) {
            leggTilDag(dato, UtbetalingsdagJsonBuilder(TypeData.ForeldetDag).økonomi(økonomi))
        }

        override fun visit(dag: UkjentDag, dato: LocalDate, økonomi: Økonomi) {
            leggTilDag(dato, UtbetalingsdagJsonBuilder(TypeData.UkjentDag).økonomi(økonomi))
        }

        override fun postVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
            utbetalingstidslinjeMap["dager"] = dager.toList()
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

        // TODO: serialiserer ikke UKJENT_DAG eller AVSLÅTT_DAG

        override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dato, DagJsonBuilder(ARBEIDSDAG, kilde))

        override fun visitDag(
            dag: Arbeidsgiverdag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dato, DagJsonBuilder(ARBEIDSGIVERDAG, kilde).økonomi(økonomi))

        override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dato, DagJsonBuilder(FERIEDAG, kilde))

        override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dato, DagJsonBuilder(PERMISJONSDAG, kilde))

        override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) =
            leggTilDag(dato, DagJsonBuilder(FRISK_HELGEDAG, kilde))

        override fun visitDag(
            dag: Dag.ArbeidsgiverHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dato, DagJsonBuilder(ARBEIDSGIVERDAG, kilde).økonomi(økonomi))

        override fun visitDag(
            dag: Dag.Sykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dato, DagJsonBuilder(SYKEDAG, kilde).økonomi(økonomi))

        override fun visitDag(
            dag: Dag.ForeldetSykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dato, DagJsonBuilder(FORELDET_SYKEDAG, kilde).økonomi(økonomi))

        override fun visitDag(
            dag: Dag.SykHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: Hendelseskilde
        ) = leggTilDag(dato, DagJsonBuilder(SYKEDAG, kilde).økonomi(økonomi))

        override fun visitDag(
            dag: Dag.ProblemDag,
            dato: LocalDate,
            kilde: Hendelseskilde,
            melding: String
        ) = leggTilDag(dato, DagJsonBuilder(PROBLEMDAG, kilde).melding(melding))

        private fun leggTilDag(dato: LocalDate, builder: DagJsonBuilder) {
            dateRanges.plus(dato, builder.build())
        }
    }

    private class DagJsonBuilder(
        private val type: JsonDagType,
        private val kilde: Hendelseskilde
    ) {
        private var melding: String? = null
        private var økonomiBuilder: ØkonomiJsonBuilder? = null

        fun melding(melding: String?) = apply {
            this.melding = melding
        }

        fun økonomi(økonomi: Økonomi) = apply {
            this.økonomiBuilder = ØkonomiJsonBuilder().also { økonomi.builder(it) }
        }

        fun build() = mutableMapOf<String, Any>().apply {
            this["type"] = type
            this["kilde"] = kilde.toJson()
            this.compute("melding") { _, _ -> melding }
            økonomiBuilder?.build()?.also { putAll(it) }
        }
    }

    private class UtbetalingsdagJsonBuilder(
        private val type: TypeData
    ) {
        private var økonomiBuilder: ØkonomiJsonBuilder? = null
        private var begrunnelser: List<PersonData.UtbetalingstidslinjeData.BegrunnelseData>? = null

        fun økonomi(økonomi: Økonomi) = apply {
            this.økonomiBuilder = ØkonomiJsonBuilder().also { økonomi.builder(it) }
        }

        fun begrunnelser(begrunnelser: List<Begrunnelse>) = apply {
            this.begrunnelser = begrunnelser.map { PersonData.UtbetalingstidslinjeData.BegrunnelseData.fraBegrunnelse(it) }
        }

        fun build() = mutableMapOf<String, Any>().apply {
            this["type"] = type
            this.compute("begrunnelser") { _, _ -> begrunnelser }
            økonomiBuilder?.build()?.also { putAll(it) }
        }
    }

    private class ØkonomiJsonBuilder : ØkonomiBuilder() {
        fun build() = mutableMapOf<String, Any>().apply {
            this["grad"] = grad
            this.compute("totalGrad") { _, _ -> totalGrad }
            this.compute("arbeidsgiverperiode") { _, _ ->
                arbeidsgiverperiode?.toList()?.grupperSammenhengendePerioder()?.map {
                    mapOf("fom" to it.start, "tom" to it.endInclusive)
                }
            }
            this.compute("arbeidsgiverRefusjonsbeløp") { _, _ -> arbeidsgiverRefusjonsbeløp }
            this.compute("skjæringstidspunkt") { _, _ -> skjæringstidspunkt }
            this.compute("grunnbeløpgrense") { _, _ -> grunnbeløpgrense }
            this.compute("dekningsgrunnlag") { _, _ -> dekningsgrunnlag }
            this.compute("aktuellDagsinntekt") { _, _ -> aktuellDagsinntekt }
            this.compute("arbeidsgiverbeløp") { _, _ -> arbeidsgiverbeløp }
            this.compute("personbeløp") { _, _ -> personbeløp }
            this.compute("er6GBegrenset") { _, _ -> er6GBegrenset }
        }
    }
}

