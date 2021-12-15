package no.nav.helse.inspectors

import no.nav.helse.Organisasjonsnummer
import no.nav.helse.antallEtterspurteBehov
import no.nav.helse.etterspurteBehov
import no.nav.helse.etterspurteBehovFinnes
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjeberegning
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class TestArbeidsgiverInspektør(
    private val person: Person,
    val orgnummer: Organisasjonsnummer
) : ArbeidsgiverVisitor, VilkårsgrunnlagHistorikkVisitor {
    internal var vedtaksperiodeTeller: Int = 0
        private set
    private var vedtaksperiodeindeks = 0
    private val tilstander = mutableMapOf<Int, TilstandType>()
    private val perioder = mutableMapOf<Int, Periode>()
    private val skjæringstidspunkter = mutableMapOf<Int, LocalDate>()
    private val maksdatoer = mutableListOf<LocalDate>()
    private val forbrukteSykedagerer = mutableListOf<Int?>()
    private val gjenståendeSykedagerer = mutableListOf<Int?>()
    private val vedtaksperiodeindekser = mutableMapOf<UUID, Int>()
    private val fagsystemIder = mutableMapOf<Int, String>()
    private val vedtaksperiodeForkastet = mutableMapOf<Int, Boolean>()
    val utbetalingstidslinjeberegningData = mutableListOf<UtbetalingstidslinjeberegningData>()
    internal val personLogg: Aktivitetslogg
    internal lateinit var arbeidsgiver: Arbeidsgiver
    internal val inntektInspektør get() = InntektshistorikkInspektør(arbeidsgiver)
    internal lateinit var sykdomshistorikk: Sykdomshistorikk
    internal lateinit var sykdomstidslinje: Sykdomstidslinje
    internal val vedtaksperiodeSykdomstidslinje = mutableMapOf<UUID, Sykdomstidslinje>()
    internal val utbetalinger = mutableListOf<Utbetaling>()
    internal val feriepengeoppdrag = mutableListOf<Feriepengeoppdrag>()
    internal val infotrygdFeriepengebeløpPerson = mutableListOf<Double>()
    internal val infotrygdFeriepengebeløpArbeidsgiver = mutableListOf<Double>()
    internal val spleisFeriepengebeløpArbeidsgiver = mutableListOf<Double>()
    private val vedtaksperiodeutbetalinger = mutableMapOf<Int, MutableList<Int>>()
    private val utbetalingstilstander = mutableListOf<Utbetaling.Tilstand>()
    private val utbetalingIder = mutableListOf<UUID>()
    private val utbetalingutbetalingstidslinjer = mutableListOf<Utbetalingstidslinje>()
    internal val arbeidsgiverOppdrag = mutableListOf<Oppdrag>()
    internal val totalBeløp = mutableListOf<Int>()
    internal val nettoBeløp = mutableListOf<Int>()
    internal lateinit var utbetalingstidslinjeBeregninger: List<Utbetalingstidslinjeberegning>
    private val utbetalingstidslinjer = mutableMapOf<Int, Utbetalingstidslinje>()
    private val vedtaksperioder = mutableMapOf<Int, Vedtaksperiode>()
    private var forkastetPeriode = false
    private var inVedtaksperiode = false
    private var inUtbetaling = false
    private var inFeriepengeutbetaling = false
    private val forlengelserFraInfotrygd = mutableMapOf<Int, ForlengelseFraInfotrygd>()
    private val hendelseIder = mutableMapOf<Int, Set<UUID>>()
    private val inntektskilder = mutableMapOf<Int, Inntektskilde>()
    private val periodetyper = mutableMapOf<Int, Periodetype>()
    internal val vilkårsgrunnlagHistorikk: MutableList<Pair<LocalDate, VilkårsgrunnlagHistorikk.Grunnlagsdata>>
    internal var warnings: List<String>
    private var personInspektør: HentAktivitetslogg

    private val vilkårsgrunnlagHistorikkInnslag: List<InnslagId>
    internal fun vilkårsgrunnlagHistorikkInnslag() = vilkårsgrunnlagHistorikkInnslag.sortedByDescending { it.timestamp }.toList()

    init {
        personInspektør = HentAktivitetslogg(person, orgnummer).also { results ->
            personLogg = results.aktivitetslogg
            warnings = results.warnings
            vilkårsgrunnlagHistorikk = results.vilkårsgrunnlagHistorikk
            vilkårsgrunnlagHistorikkInnslag = results.vilkårsgrunnlagHistorikkInnslag.toList()
            results.arbeidsgiver.accept(this)
        }
    }

    private class HentAktivitetslogg(person: Person, private val valgfriOrgnummer: Organisasjonsnummer?) : PersonVisitor {
        lateinit var aktivitetslogg: Aktivitetslogg
        lateinit var arbeidsgiver: Arbeidsgiver
        val vilkårsgrunnlagHistorikk = mutableListOf<Pair<LocalDate,VilkårsgrunnlagHistorikk.Grunnlagsdata>>()
        val warnings = mutableListOf<String>()
        val vilkårsgrunnlagHistorikkInnslag: MutableList<InnslagId> = mutableListOf()

        init {
            person.accept(this)
        }

        override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
            this.aktivitetslogg = aktivitetslogg
        }

        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            if (organisasjonsnummer == valgfriOrgnummer?.toString()) this.arbeidsgiver = arbeidsgiver
            if (this::arbeidsgiver.isInitialized) return
            this.arbeidsgiver = arbeidsgiver
        }

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
            vilkårsgrunnlagHistorikk.add(skjæringstidspunkt to grunnlagsdata)
        }

        override fun preVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
            vilkårsgrunnlagHistorikkInnslag.add(InnslagId(id, opprettet))
        }

        override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
            warnings.add(melding)
        }
    }

    override fun preVisitUtbetalingstidslinjeberegninger(beregninger: List<Utbetalingstidslinjeberegning>) {
        utbetalingstidslinjeBeregninger = beregninger
    }

    override fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {
        this.arbeidsgiver = arbeidsgiver
    }

    override fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        forkastetPeriode = true
    }

    override fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        forkastetPeriode = false
    }

    override fun preVisitUtbetalingstidslinjeberegning(
        id: UUID,
        tidsstempel: LocalDateTime,
        organisasjonsnummer: String,
        sykdomshistorikkElementId: UUID,
        inntektshistorikkInnslagId: UUID,
        vilkårsgrunnlagHistorikkInnslagId: UUID
    ) {
        utbetalingstidslinjeberegningData.add(
            UtbetalingstidslinjeberegningData(
            id = id,
            tidsstempel = tidsstempel,
            sykdomshistorikkElementId = sykdomshistorikkElementId,
            inntektshistorikkInnslagId = inntektshistorikkInnslagId,
            vilkårsgrunnlagHistorikkInnslagId = vilkårsgrunnlagHistorikkInnslagId
        )
        )
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
        inVedtaksperiode = true
        vedtaksperiodeTeller += 1
        vedtaksperiodeindekser[id] = vedtaksperiodeindeks
        vedtaksperiodeForkastet[vedtaksperiodeindeks] = forkastetPeriode
        vedtaksperioder[vedtaksperiodeindeks] = vedtaksperiode
        this.hendelseIder[vedtaksperiodeindeks] = hendelseIder
        perioder[vedtaksperiodeindeks] = periode
        tilstander[vedtaksperiodeindeks] = tilstand.type
        inntektskilder[vedtaksperiodeindeks] = inntektskilde
        skjæringstidspunkter[vedtaksperiodeindeks] = skjæringstidspunkt
        forlengelserFraInfotrygd[vedtaksperiodeindeks] = forlengelseFraInfotrygd
        periodetyper[vedtaksperiodeindeks] = periodetype
        vedtaksperiode.accept(VedtaksperiodeSykdomstidslinjeinnhenter())
    }

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        if (inVedtaksperiode && !inUtbetaling) utbetalingstidslinjer[vedtaksperiodeindeks] = tidslinje
        else if (!inVedtaksperiode && inUtbetaling) utbetalingutbetalingstidslinjer.add(tidslinje)
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
        vedtaksperiodeindeks += 1
        inVedtaksperiode = false
    }

    override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        this.utbetalinger.addAll(utbetalinger)
    }

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        if (!inVedtaksperiode) arbeidsgiverOppdrag.add(oppdrag)
        if (inVedtaksperiode) fagsystemIder[vedtaksperiodeindeks] = oppdrag.fagsystemId()
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
        if (inFeriepengeutbetaling) feriepengeoppdrag.add(Feriepengeoppdrag(oppdrag.fagsystemId()))

        if (oppdrag != arbeidsgiverOppdrag.lastOrNull()) return
        this.totalBeløp.add(totalBeløp)
        this.nettoBeløp.add(nettoBeløp)
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
        grad: Double?,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?,
        statuskode: String?,
        klassekode: Klassekode
    ) {
        if(inFeriepengeutbetaling) {
            feriepengeoppdrag
                .lastOrNull()
                ?.feriepengeutbetalingslinjer
                ?.add(Feriepengeutbetalingslinje(fom, tom, satstype, beløp, grad, klassekode, endringskode))
        }
    }

    internal data class Feriepengeoppdrag(
        val fagsystemId: String,
        val feriepengeutbetalingslinjer: MutableList<Feriepengeutbetalingslinje> = mutableListOf()
    )

    internal data class Feriepengeutbetalingslinje(
        val fom: LocalDate,
        val tom: LocalDate,
        val satstype: Satstype,
        val beløp: Int?,
        val grad: Double?,
        val klassekode: Klassekode,
        val endringskode: Endringskode
    )

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
        inUtbetaling = true
        if (!inVedtaksperiode) {
            maksdatoer.add(maksdato)
            utbetalingIder.add(id)
            utbetalingstilstander.add(tilstand)
            forbrukteSykedagerer.add(forbrukteSykedager)
            gjenståendeSykedagerer.add(gjenståendeSykedager)
        } else {
            vedtaksperiodeutbetalinger.getOrPut(vedtaksperiodeindeks) { mutableListOf() }.add(utbetalinger.indexOf(utbetaling))
        }
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
        inUtbetaling = false
    }

    override fun preVisitFeriepengeutbetaling(
        feriepengeutbetaling: Feriepengeutbetaling,
        infotrygdFeriepengebeløpPerson: Double,
        infotrygdFeriepengebeløpArbeidsgiver: Double,
        spleisFeriepengebeløpArbeidsgiver: Double,
        overføringstidspunkt: LocalDateTime?,
        avstemmingsnøkkel: Long?,
        utbetalingId: UUID,
    ) {
        inFeriepengeutbetaling = true
        this.infotrygdFeriepengebeløpArbeidsgiver.add(infotrygdFeriepengebeløpArbeidsgiver)
        this.infotrygdFeriepengebeløpPerson.add(infotrygdFeriepengebeløpPerson)
        this.spleisFeriepengebeløpArbeidsgiver.add(spleisFeriepengebeløpArbeidsgiver)
    }

    override fun postVisitFeriepengeutbetaling(
        feriepengeutbetaling: Feriepengeutbetaling,
        infotrygdFeriepengebeløpPerson: Double,
        infotrygdFeriepengebeløpArbeidsgiver: Double,
        spleisFeriepengebeløpArbeidsgiver: Double,
        overføringstidspunkt: LocalDateTime?,
        avstemmingsnøkkel: Long?,
        utbetalingId: UUID,
    ) {
        inFeriepengeutbetaling = false
    }

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        if (inVedtaksperiode) return
        this.sykdomshistorikk = sykdomshistorikk
        if (!sykdomshistorikk.isEmpty()) {
            sykdomstidslinje = sykdomshistorikk.sykdomstidslinje()
        }
    }

    private inner class VedtaksperiodeSykdomstidslinjeinnhenter: VedtaksperiodeVisitor {
        private lateinit var vedtaksperiodeId: UUID
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
            vedtaksperiodeId = id
        }

        override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
            vedtaksperiodeSykdomstidslinje[vedtaksperiodeId] = tidslinje
        }
    }

    private fun <V> IdInnhenter.finn(hva: Map<Int, V>) = hva.getValue(this.indeks)
    private val IdInnhenter.indeks get() = this(orgnummer).indeks

    private fun <V> UUID.finn(hva: Map<Int, V>) = hva.getValue(this.indeks)
    private val UUID.indeks get() = vedtaksperiodeindekser[this] ?: fail { "Vedtaksperiode $this finnes ikke" }

    private val UUID.utbetalingsindeks get() = this.finn(vedtaksperiodeutbetalinger)

    internal fun gjeldendeUtbetalingForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter).last()
    internal fun ikkeUtbetalteUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = utbetalinger.filterIndexed { index, _ -> index in vedtaksperiodeIdInnhenter(orgnummer).utbetalingsindeks }.filter { it.inspektør.erUbetalt }
    internal fun avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = utbetalinger.filterIndexed { index, _ -> index in vedtaksperiodeIdInnhenter(orgnummer).utbetalingsindeks }.filter { it.erAvsluttet() }
    internal fun utbetalingtilstand(indeks: Int) = utbetalingstilstander[indeks]
    internal fun utbetaling(indeks: Int) = utbetalinger[indeks]
    internal fun utbetalingId(indeks: Int) = utbetalingIder[indeks]
    internal fun utbetalingUtbetalingstidslinje(indeks: Int) = utbetalingutbetalingstidslinjer[indeks]
    internal fun sisteUtbetalingUtbetalingstidslinje() = utbetalingutbetalingstidslinjer.last()
    internal fun periode(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(perioder)
    internal fun vedtaksperiodeSykdomstidslinje(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeSykdomstidslinje.getValue(vedtaksperiodeIdInnhenter(orgnummer))

    internal fun periodeErForkastet(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(vedtaksperiodeForkastet)

    internal fun periodeErIkkeForkastet(vedtaksperiodeIdInnhenter: IdInnhenter) = !periodeErForkastet(vedtaksperiodeIdInnhenter)

    internal fun antallEtterspurteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        personLogg.antallEtterspurteBehov(vedtaksperiodeIdInnhenter(orgnummer), behovtype)

    internal fun etterspurteBehov(vedtaksperiodeIdInnhenter: IdInnhenter, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        personLogg.etterspurteBehovFinnes(vedtaksperiodeIdInnhenter(orgnummer), behovtype)

    internal fun etterspurteBehov(vedtaksperiodeId: UUID, tilstand: TilstandType, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        personLogg.etterspurteBehovFinnes(vedtaksperiodeId, tilstand, behovtype)

    internal fun etterspurteBehov(vedtaksperiodeIdInnhenter: IdInnhenter) =
        personLogg.etterspurteBehov(vedtaksperiodeIdInnhenter(orgnummer))

    internal fun sisteBehov(vedtaksperiodeIdInnhenter: IdInnhenter) =
        personLogg.behov().last { it.kontekst()["vedtaksperiodeId"] == vedtaksperiodeIdInnhenter(orgnummer).toString() }

    internal fun sisteBehov(type: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
        personLogg.behov().last { it.type == type }

    internal fun maksdato(indeks: Int) = maksdatoer[indeks]
    internal fun maksdatoVedSisteVedtak() = utbetalinger.indexOfLast(Utbetaling::erAvsluttet).takeIf { it > -1 }?.let { index -> maksdato(index) }
    internal fun sisteMaksdato(vedtaksperiodeIdInnhenter: IdInnhenter) = maksdatoer.filterIndexed { index, _ -> index in vedtaksperiodeIdInnhenter(orgnummer).utbetalingsindeks }.last()

    internal fun forbrukteSykedager(indeks: Int) = forbrukteSykedagerer[indeks]
    internal fun gjenståendeSykedager(indeks: Int) = gjenståendeSykedagerer[indeks]

    internal fun gjenståendeSykedager(vedtaksperiodeIdInnhenter: IdInnhenter) = gjenståendeSykedagerer.filterIndexed { index, _ -> index in vedtaksperiodeIdInnhenter(orgnummer).utbetalingsindeks }.last() ?: fail {
        "Vedtaksperiode ${vedtaksperiodeIdInnhenter(orgnummer)} har ikke oppgitt gjenstående sykedager"
    }

    internal fun forlengelseFraInfotrygd(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(forlengelserFraInfotrygd)

    internal fun vilkårsgrunnlag(vedtaksperiodeIdInnhenter: IdInnhenter) = person.vilkårsgrunnlagFor(skjæringstidspunkt(vedtaksperiodeIdInnhenter))

    internal fun utbetalingslinjer(indeks: Int) = arbeidsgiverOppdrag[indeks]

    internal fun sisteTilstand(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(tilstander)

    internal fun skjæringstidspunkt(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(skjæringstidspunkter)

    internal fun utbetalingstidslinjer(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(utbetalingstidslinjer)

    internal fun vedtaksperioder(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(vedtaksperioder)

    internal fun hendelseIder(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(hendelseIder)

    internal fun fagsystemId(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(fagsystemIder)

    internal fun inntektskilde(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(inntektskilder)

    internal fun periodetype(id: UUID) = id.finn(periodetyper)

    internal fun vedtaksperiodeId(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter(orgnummer)

    internal data class UtbetalingstidslinjeberegningData(
        val id: UUID,
        val tidsstempel: LocalDateTime,
        val sykdomshistorikkElementId: UUID,
        val inntektshistorikkInnslagId: UUID,
        val vilkårsgrunnlagHistorikkInnslagId: UUID
    )

    internal data class InnslagId(
        val id: UUID,
        val timestamp: LocalDateTime
    )
}
