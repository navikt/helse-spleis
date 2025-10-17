package no.nav.helse.inspectors

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.feriepenger.Feriepengerendringskode
import no.nav.helse.feriepenger.Feriepengerklassekode
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Person
import no.nav.helse.person.Yrkesaktivitet
import no.nav.helse.spleis.e2e.IdInnhenter
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import org.junit.jupiter.api.fail

internal class TestArbeidsgiverInspektør(
    private val person: Person,
    val orgnummer: String
) {
    internal companion object {
        internal operator fun TestArbeidsgiverInspektør.invoke(blokk: TestArbeidsgiverInspektør.() -> Unit) {
            this.apply(blokk)
        }
    }

    internal var yrkesaktivitet: Yrkesaktivitet = person.yrkesaktiviteter.first { it.organisasjonsnummer() == orgnummer }
    private val view = person.view().arbeidsgivere.single { it.organisasjonsnummer == orgnummer }

    private val personInspektør = person.inspektør
    internal val vedtaksperiodeTeller: Int = view.aktiveVedtaksperioder.size + view.forkastetVedtaksperioder.size
    private val vedtaksperioder = (view.aktiveVedtaksperioder + view.forkastetVedtaksperioder)
        .associateBy { it.id }
    private val tilstander = (view.aktiveVedtaksperioder + view.forkastetVedtaksperioder)
        .mapIndexed { index, periode -> index to periode.tilstand }
        .toMap()

    private val vedtaksperiodeindekser = (view.aktiveVedtaksperioder + view.forkastetVedtaksperioder).mapIndexed { index, periode ->
        periode.id to index
    }.toMap()

    private val vedtaksperiodeForkastet = view.forkastetVedtaksperioder.map { it.id }.toSet()
    internal val inntektInspektør get() = InntektshistorikkInspektør(view.inntektshistorikk)
    val sykdomshistorikk = view.sykdomshistorikk.inspektør
    internal val sykdomstidslinje: Sykdomstidslinje get() = sykdomshistorikk.tidslinje(0)
    internal val utbetalinger = view.utbetalinger.map { it.inspektør }
    internal val antallUtbetalinger get() = utbetalinger.size

    val ubrukteRefusjonsopplysninger = view.ubrukteRefusjonsopplysninger

    internal val feriepengeoppdrag = view.feriepengeutbetalinger
        .flatMap { listOf(it.oppdrag, it.personoppdrag) }
        .map {
            Feriepengeoppdrag(
                fagsystemId = it.fagsystemId,
                feriepengeutbetalingslinjer = listOfNotNull(it.linje?.let { linje ->
                    Feriepengeutbetalingslinje(linje.fom, linje.tom, linje.beløp, linje.klassekode, linje.endringskode, linje.statuskode)
                })
            )
        }
    internal val infotrygdFeriepengebeløpPerson = view.feriepengeutbetalinger.map { it.infotrygdFeriepengebeløpPerson }
    internal val infotrygdFeriepengebeløpArbeidsgiver = view.feriepengeutbetalinger.map { it.infotrygdFeriepengebeløpArbeidsgiver }
    internal val spleisFeriepengebeløpArbeidsgiver = view.feriepengeutbetalinger.map { it.spleisFeriepengebeløpArbeidsgiver }
    internal val spleisFeriepengebeløpPerson = view.feriepengeutbetalinger.map { it.spleisFeriepengebeløpPerson }

    private val sykmeldingsperioder = view.sykmeldingsperioder.perioder

    internal fun vilkårsgrunnlaghistorikk() = person.view().vilkårsgrunnlaghistorikk.inspektør
    internal fun vilkårsgrunnlagHistorikkInnslag() = vilkårsgrunnlaghistorikk().vilkårsgrunnlagHistorikkInnslag()

    internal data class Feriepengeoppdrag(
        val fagsystemId: String,
        val feriepengeutbetalingslinjer: List<Feriepengeutbetalingslinje>
    ) {
        internal companion object {
            val List<Feriepengeoppdrag>.utbetalingslinjer
                get(): List<Feriepengeutbetalingslinje> {
                    val sisteOppdragPerFagsystemId = groupBy { it.fagsystemId }.map { (_, oppdrag) -> oppdrag.last() }
                    return sisteOppdragPerFagsystemId.flatMap { it.feriepengeutbetalingslinjer }
                }
        }
    }

    internal data class Feriepengeutbetalingslinje(
        val fom: LocalDate,
        val tom: LocalDate,
        val beløp: Int?,
        val klassekode: Feriepengerklassekode,
        val endringskode: Feriepengerendringskode,
        val statuskode: String? = null
    )

    private fun <V> IdInnhenter.finn(hva: Map<Int, V>) = hva.getValue(this.indeks)
    private val IdInnhenter.indeks get() = id(orgnummer).indeks

    private fun <V> UUID.finn(hva: Map<Int, V>) = hva.getValue(this.indeks)
    private val UUID.indeks get() = vedtaksperiodeindekser[this] ?: fail { "Vedtaksperiode $this finnes ikke" }

    internal fun sisteAvsluttedeUtbetalingForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter).last()
    internal fun ikkeUtbetalteUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = ikkeUtbetalteUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun ikkeUtbetalteUtbetalingerForVedtaksperiode(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger.filter { it.inspektør.erUbetalt }
    internal fun avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun avsluttedeUtbetalingerForVedtaksperiode(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger.filter { it.erAvsluttet }
    internal fun utbetalinger(vedtaksperiodeIdInnhenter: IdInnhenter) = utbetalinger(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun utbetalinger(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger

    internal fun utbetalingerInFlight() = utbetalinger.filter { it.tilstand == Utbetalingstatus.OVERFØRT }
    internal fun sisteUtbetaling() = utbetalinger.last()
    internal fun utbetalingtilstand(indeks: Int) = utbetalinger[indeks].tilstand
    internal fun utbetaling(indeks: Int) = utbetalinger[indeks]
    internal fun utbetalingId(indeks: Int) = utbetalinger[indeks].utbetalingId
    internal fun utbetalingslinjer(indeks: Int) = utbetalinger[indeks].arbeidsgiverOppdrag

    internal fun periode(vedtaksperiodeIdInnhenter: IdInnhenter) = periode(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun periode(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.periode
    internal fun vedtaksperiodeSykdomstidslinje(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter).inspektør.sykdomstidslinje

    internal fun periodeErForkastet(vedtaksperiodeIdInnhenter: IdInnhenter) = periodeErForkastet(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun periodeErForkastet(vedtaksperiodeId: UUID) = vedtaksperiodeId in vedtaksperiodeForkastet

    internal fun periodeErIkkeForkastet(vedtaksperiodeIdInnhenter: IdInnhenter) = !periodeErForkastet(vedtaksperiodeIdInnhenter)
    internal fun periodeErIkkeForkastet(vedtaksperiodeId: UUID) = !periodeErForkastet(vedtaksperiodeId)

    internal fun sisteMaksdato(vedtaksperiodeIdInnhenter: IdInnhenter) = sisteMaksdato(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun sisteMaksdato(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.maksdatoer.last()

    internal fun sisteUtbetalingId(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter).inspektør.utbetalinger.last().id
    internal fun sisteUtbetalingId(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger.last().id

    internal fun vilkårsgrunnlag(vedtaksperiodeIdInnhenter: IdInnhenter) = person.vilkårsgrunnlagFor(skjæringstidspunkt(vedtaksperiodeIdInnhenter))
    internal fun vilkårsgrunnlag(vedtaksperiodeId: UUID) = person.vilkårsgrunnlagFor(skjæringstidspunkt(vedtaksperiodeId))
    internal fun vilkårsgrunnlag(skjæringstidspunkt: LocalDate) = person.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun sisteTilstand(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.finn(tilstander)

    internal fun skjæringstidspunkt(vedtaksperiodeIdInnhenter: IdInnhenter) = skjæringstidspunkt(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun skjæringstidspunkt(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.skjæringstidspunkt

    internal fun skjæringstidspunkter(vedtaksperiodeIdInnhenter: IdInnhenter) = skjæringstidspunkter(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun skjæringstidspunkter(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.skjæringstidspunkter

    internal fun førsteFraværsdag(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.førsteFraværsdag

    internal fun utbetalingstidslinjer(vedtaksperiodeIdInnhenter: IdInnhenter) = utbetalingstidslinjer(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun utbetalingstidslinjer(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalingstidslinje

    internal fun vedtaksperioder(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder.getValue(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun vedtaksperioder(vedtaksperiodeId: UUID) = vedtaksperioder.getValue(vedtaksperiodeId)
    internal fun vedtaksperioder(periode: Periode) = vedtaksperioder.values.first { it.periode == periode }
    internal fun førsteVedtaksperiodeSomOverlapperEllerErEtter(dato: LocalDate) = vedtaksperioder.values.firstOrNull { it.periode.start >= dato } ?: error("Ingen perioder overlapper eller starter etter $dato")

    internal fun hendelser(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter.id(orgnummer)).inspektør.hendelser
    internal fun hendelseIder(vedtaksperiodeIdInnhenter: IdInnhenter) = hendelseIder(vedtaksperiodeIdInnhenter.id(orgnummer))
    internal fun hendelseIder(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.hendelseIder.map { it.id }.toSet()

    internal fun sisteArbeidsgiveroppdragFagsystemId(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter).inspektør.utbetalinger.last().arbeidsgiverOppdrag.fagsystemId
    internal fun sisteArbeidsgiveroppdragFagsystemId(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.utbetalinger.last().arbeidsgiverOppdrag.fagsystemId

    internal fun vedtaksperiodeId(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperiodeIdInnhenter.id(orgnummer)

    internal fun sykmeldingsperioder() = sykmeldingsperioder.toList()

    internal fun arbeidsgiverperioden(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter).inspektør.arbeidsgiverperiode
    internal fun arbeidsgiverperioder(vedtaksperiodeIdInnhenter: IdInnhenter) = arbeidsgiverperioden(vedtaksperiodeIdInnhenter)
    internal fun arbeidsgiverperiode(vedtaksperiodeIdInnhenter: IdInnhenter) = arbeidsgiverperioder(vedtaksperiodeIdInnhenter)
    internal fun arbeidsgiverperiode(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.arbeidsgiverperiode

    internal fun ventetid(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).inspektør.ventetid

    internal fun egenmeldingsdager(vedtaksperiodeId: UUID) = vedtaksperioder(vedtaksperiodeId).egenmeldingsdager
    internal fun egenmeldingsdager(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter).egenmeldingsdager

    internal fun refusjon(vedtaksperiodeIdInnhenter: IdInnhenter) = vedtaksperioder(vedtaksperiodeIdInnhenter).refusjonstidslinje
    internal fun refusjon(vedtaksperiodeId: UUID) = vedtaksperioder.getValue(vedtaksperiodeId).refusjonstidslinje

    internal fun dagerNavOvertarAnsvar(vedtaksperiodeId: UUID) = vedtaksperioder.getValue(vedtaksperiodeId).dagerNavOvertarAnsvar
    internal fun dagerNavOvertarAnsvar(vedtaksperiodeId: IdInnhenter) = vedtaksperioder.getValue(vedtaksperiodeId.id(orgnummer)).dagerNavOvertarAnsvar
}
