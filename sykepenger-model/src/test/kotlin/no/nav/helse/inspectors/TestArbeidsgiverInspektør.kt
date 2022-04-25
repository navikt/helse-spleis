package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeVisitor
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Feriepengeutbetaling
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjeberegning
import org.junit.jupiter.api.fail

internal class TestArbeidsgiverInspektør(
    private val person: Person,
    val orgnummer: String
) : ArbeidsgiverVisitor, VilkårsgrunnlagHistorikkVisitor {
    private val personInspektør = person.inspektør

    internal var vedtaksperiodeTeller: Int = 0
        private set
    private var vedtaksperiodeindeks = 0
    private val tilstander = mutableMapOf<Int, TilstandType>()
    private val perioder = mutableMapOf<Int, Periode>()
    private val skjæringstidspunkter = mutableMapOf<Int, () -> LocalDate>()
    private val maksdatoer = mutableListOf<LocalDate>()
    private val forbrukteSykedagerer = mutableListOf<Int?>()
    private val gjenståendeSykedagerer = mutableListOf<Int?>()
    private val vedtaksperiodeindekser = mutableMapOf<UUID, Int>()
    private val fagsystemIder = mutableMapOf<Int, String>()
    private val vedtaksperiodeForkastet = mutableMapOf<Int, Boolean>()
    val utbetalingstidslinjeberegningData = mutableListOf<UtbetalingstidslinjeberegningData>()
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
    private val vedtaksperiodeutbetalingider = mutableMapOf<Int, MutableList<UUID>>()
    private val vedtaksperiodeutbetalingstyper = mutableMapOf<Int, MutableList<Utbetalingtype>>()
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
    private val hendelseIder = mutableMapOf<Int, Set<Dokumentsporing>>()
    private val inntektskilder = mutableMapOf<Int, Inntektskilde>()
    private val periodetyper = mutableMapOf<Int, () -> Periodetype>()
    private val sykmeldingsperioder = mutableListOf<Periode>()

    internal fun vilkårsgrunnlagHistorikkInnslag() = personInspektør.vilkårsgrunnlagHistorikkInnslag()

    init {
        HentAktivitetslogg(person, orgnummer).also { results ->
            results.arbeidsgiver.accept(this)
        }
    }

    private class HentAktivitetslogg(person: Person, private val valgfriOrgnummer: String?) : PersonVisitor {
        lateinit var aktivitetslogg: Aktivitetslogg
        lateinit var arbeidsgiver: Arbeidsgiver

        init {
            person.accept(this)
        }

        override fun visitPersonAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
            this.aktivitetslogg = aktivitetslogg
        }

        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            if (organisasjonsnummer == valgfriOrgnummer) this.arbeidsgiver = arbeidsgiver
            if (this::arbeidsgiver.isInitialized) return
            this.arbeidsgiver = arbeidsgiver
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
        periodetype: () -> Periodetype,
        skjæringstidspunkt: () -> LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<Dokumentsporing>,
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
        periodetype: () -> Periodetype,
        skjæringstidspunkt: () -> LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<Dokumentsporing>,
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
        grad: Int?,
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
        val grad: Int?,
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
            vedtaksperiodeutbetalingider.getOrPut(vedtaksperiodeindeks) { mutableListOf() }.add(id)
            vedtaksperiodeutbetalingstyper.getOrPut(vedtaksperiodeindeks) { mutableListOf() }.add(type)
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

    override fun visitSykmeldingsperiode(periode: Periode) {
        this.sykmeldingsperioder.add(periode)
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
            periodetype: () -> Periodetype,
            skjæringstidspunkt: () -> LocalDate,
            skjæringstidspunktFraInfotrygd: LocalDate?,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: Set<Dokumentsporing>,
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
    private val IdInnhenter.indeks get() = id(orgnummer).indeks

    private fun <V> UUID.finn(hva: Map<Int, V>) = hva.getValue(this.indeks)
    private fun <V> UUID.finnOrNull(hva: Map<Int, V>) = hva[this.indeks]
    private val UUID.indeks get() = vedtaksperiodeindekser[this] ?: fail { "Vedtaksperiode $this finnes ikke" }

    private val UUID.utbetalingsindeks get() = this.finn(vedtaksperiodeutbetalinger)
    private val UUID.utbetalingsindeksOrNull get() = this.finnOrNull(vedtaksperiodeutbetalinger)

    internal fun gjeldendeUtbetalingForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter).last()
    internal fun ikkeUtbetalteUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = utbetalinger.filterIndexed { index, _ -> index in vedtaksperiodeIdInnhenter.id(orgnummer).utbetalingsindeks }.filter { it.inspektør.erUbetalt }
    internal fun avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = utbetalinger.filterIndexed { index, _ -> index in vedtaksperiodeIdInnhenter.id(orgnummer).utbetalingsindeks }.filter { it.erAvsluttet() }
    internal fun utbetalinger(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.id(orgnummer).utbetalingsindeksOrNull?.let { indekser ->
        utbetalinger.filterIndexed { index, _ -> index in indekser }
    } ?: emptyList()
    internal fun utbetalingstype(vedtaksperiodeIdInnhenter: IdInnhenter, utbetalingIndex: Int) = vedtaksperiodeutbetalingstyper[vedtaksperiodeIdInnhenter.id(orgnummer).indeks]!!.getOrNull(utbetalingIndex)
    internal fun utbetalingtilstand(indeks: Int) = utbetalingstilstander[indeks]
    internal fun utbetaling(indeks: Int) = utbetalinger[indeks]
    internal fun utbetalingId(indeks: Int) = utbetalingIder[indeks]
    internal fun utbetalingUtbetalingstidslinje(indeks: Int) = utbetalingutbetalingstidslinjer[indeks]
    internal fun sisteUtbetalingUtbetalingstidslinje() = utbetalingutbetalingstidslinjer.last()
    internal fun periode(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(perioder)
    internal fun vedtaksperiodeSykdomstidslinje(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeSykdomstidslinje.getValue(vedtaksperiodeIdInnhenter.id(orgnummer))

    internal fun periodeErForkastet(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(vedtaksperiodeForkastet)

    internal fun periodeErIkkeForkastet(vedtaksperiodeIdInnhenter: IdInnhenter) = !periodeErForkastet(vedtaksperiodeIdInnhenter)

    internal fun maksdato(indeks: Int) = maksdatoer[indeks]
    internal fun maksdatoVedSisteVedtak() = utbetalinger.indexOfLast(Utbetaling::erAvsluttet).takeIf { it > -1 }?.let { index -> maksdato(index) }
    internal fun sisteMaksdato(vedtaksperiodeIdInnhenter: IdInnhenter) = maksdatoer.filterIndexed { index, _ -> index in vedtaksperiodeIdInnhenter.id(orgnummer).utbetalingsindeks }.last()

    internal fun forbrukteSykedager(indeks: Int) = forbrukteSykedagerer[indeks]
    internal fun gjenståendeSykedager(indeks: Int) = gjenståendeSykedagerer[indeks]

    internal fun gjenståendeSykedager(vedtaksperiodeIdInnhenter: IdInnhenter) = gjenståendeSykedagerer.filterIndexed { index, _ -> index in vedtaksperiodeIdInnhenter.id(orgnummer).utbetalingsindeks }.last() ?: fail {
        "Vedtaksperiode ${vedtaksperiodeIdInnhenter.id(orgnummer)} har ikke oppgitt gjenstående sykedager"
    }

    internal fun utbetalingId(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeutbetalingider[vedtaksperiodeIdInnhenter.id(orgnummer).indeks]?.last() ?: fail {
        "Vedtaksperiode ${vedtaksperiodeIdInnhenter.id(orgnummer)} har ingen utbetalinger"
    }

    internal fun forlengelseFraInfotrygd(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(forlengelserFraInfotrygd)

    internal fun vilkårsgrunnlag(vedtaksperiodeIdInnhenter: IdInnhenter) = person.vilkårsgrunnlagFor(skjæringstidspunkt(vedtaksperiodeIdInnhenter))

    internal fun utbetalingslinjer(indeks: Int) = arbeidsgiverOppdrag[indeks]

    internal fun sisteTilstand(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(tilstander)

    internal fun skjæringstidspunkt(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(skjæringstidspunkter)()

    internal fun utbetalingstidslinjer(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(utbetalingstidslinjer)

    internal fun vedtaksperioder(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(vedtaksperioder)

    internal fun hendelseIder(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(hendelseIder).ider()

    internal fun fagsystemId(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(fagsystemIder)

    internal fun inntektskilde(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(inntektskilder)

    internal fun periodetype(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(periodetyper)()

    internal fun vedtaksperiodeId(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.id(orgnummer)

    internal fun sykmeldingsperioder() = sykmeldingsperioder.toList()

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
