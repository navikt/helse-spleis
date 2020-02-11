package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelVilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

internal fun createPerson(
    aktørId: String,
    fødselsnummer: String,
    arbeidsgivere: MutableList<Arbeidsgiver>,
    hendelser: MutableList<ArbeidstakerHendelse>,
    aktivitetslogger: Aktivitetslogger
) = Person::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(aktørId, fødselsnummer, arbeidsgivere, hendelser, aktivitetslogger)

internal fun createArbeidsgiver(
    person: Person,
    organisasjonsnummer: String,
    id: UUID,
    inntekthistorikk: Inntekthistorikk,
    tidslinjer: MutableList<Utbetalingstidslinje>,
    perioder: MutableList<Vedtaksperiode>,
    utbetalingsreferanse: Long,
    aktivitetslogger: Aktivitetslogger
) = Arbeidsgiver::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(person, organisasjonsnummer, id, inntekthistorikk, tidslinjer, perioder, utbetalingsreferanse, aktivitetslogger)

internal fun createVedtaksperiode(
    person: Person,
    arbeidsgiver: Arbeidsgiver,
    id: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
    maksdato: LocalDate?,
    utbetalingslinjer: List<Utbetalingslinje>?,
    godkjentAv: String?,
    utbetalingsreferanse: String?,
    førsteFraværsdag: LocalDate?,
    inntektFraInntektsmelding: Double?,
    dataForVilkårsvurdering: ModelVilkårsgrunnlag.Grunnlagsdata?,
    sykdomshistorikk: Sykdomshistorikk,
    aktivitetslogger: Aktivitetslogger
) = Vedtaksperiode::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(
        person, arbeidsgiver, id, aktørId, fødselsnummer, organisasjonsnummer, tilstand, maksdato, utbetalingslinjer, godkjentAv,
        utbetalingsreferanse, førsteFraværsdag, inntektFraInntektsmelding, dataForVilkårsvurdering, sykdomshistorikk,
        aktivitetslogger
    )

internal fun createSykdomshistorikk(
    elementer: List<Sykdomshistorikk.Element>
) = Sykdomshistorikk::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(elementer)

internal fun createSykdomshistorikkElement(
    timestamp: LocalDateTime,
    hendelseSykdomstidslinje: ConcreteSykdomstidslinje,
    beregnetSykdomstidslinje: ConcreteSykdomstidslinje,
    hendelse: SykdomstidslinjeHendelse
) = Sykdomshistorikk.Element::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(timestamp, hendelseSykdomstidslinje, beregnetSykdomstidslinje, hendelse)

internal fun createUtbetalingstidslinje(
    utbetalingsdager: MutableList<Utbetalingstidslinje.Utbetalingsdag>
) = Utbetalingstidslinje::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(utbetalingsdager)

internal fun createNavUtbetalingdag(
    inntekt: Double,
    dato: LocalDate,
    utbetaling: Int
) = Utbetalingstidslinje.Utbetalingsdag.NavDag::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(inntekt, dato, utbetaling)
