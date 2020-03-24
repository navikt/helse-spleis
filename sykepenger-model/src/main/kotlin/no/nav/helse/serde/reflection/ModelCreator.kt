package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
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
    aktivitetslogg: Aktivitetslogg
) = Person::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(aktørId, fødselsnummer, arbeidsgivere, aktivitetslogg)

internal fun createArbeidsgiver(
    person: Person,
    organisasjonsnummer: String,
    id: UUID,
    inntekthistorikk: Inntekthistorikk,
    tidslinjer: MutableList<Utbetalingstidslinje>,
    perioder: MutableList<Vedtaksperiode>
) = Arbeidsgiver::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(person, organisasjonsnummer, id, inntekthistorikk, tidslinjer, perioder)

internal fun createVedtaksperiode(
    person: Person,
    arbeidsgiver: Arbeidsgiver,
    id: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
    maksdato: LocalDate?,
    forbrukteSykedager: Int?,
    utbetalingslinjer: List<Utbetalingslinje>?,
    godkjentAv: String?,
    godkjenttidspunkt: LocalDateTime?,
    utbetalingsreferanse: String?,
    førsteFraværsdag: LocalDate?,
    dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?,
    sykdomshistorikk: Sykdomshistorikk,
    utbetalingstidslinje: Utbetalingstidslinje?
) = Vedtaksperiode::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(
        person, arbeidsgiver, id, aktørId, fødselsnummer, organisasjonsnummer, tilstand, maksdato, forbrukteSykedager, utbetalingslinjer, godkjentAv,
        godkjenttidspunkt, utbetalingsreferanse, førsteFraværsdag, dataForVilkårsvurdering, sykdomshistorikk, utbetalingstidslinje
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
    hendelseId: UUID
) = Sykdomshistorikk.Element::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(hendelseId, timestamp, hendelseSykdomstidslinje, beregnetSykdomstidslinje)

internal fun createUtbetalingstidslinje(
    utbetalingsdager: MutableList<Utbetalingstidslinje.Utbetalingsdag>
) = Utbetalingstidslinje::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(utbetalingsdager)

internal fun createNavUtbetalingdag(
    inntekt: Double,
    dato: LocalDate,
    utbetaling: Int,
    grad: Double
) = Utbetalingstidslinje.Utbetalingsdag.NavDag::class.primaryConstructor!!
    .apply { isAccessible = true }
    .call(inntekt, dato, utbetaling, grad)
