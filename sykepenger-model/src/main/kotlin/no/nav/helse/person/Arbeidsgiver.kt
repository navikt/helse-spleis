package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class Arbeidsgiver private constructor(
    private val person: Person,
    private val organisasjonsnummer: String,
    private val id: UUID,
    private val inntekthistorikk: Inntekthistorikk,
    private val tidslinjer: MutableList<Utbetalingstidslinje>,
    private val perioder: MutableList<Vedtaksperiode>,
    private val aktivitetslogger: Aktivitetslogger
) {

    internal fun inntektshistorikk() = inntekthistorikk.clone()

    internal constructor(person: Person, organisasjonsnummer: String) : this(
        person = person,
        organisasjonsnummer = organisasjonsnummer,
        id = UUID.randomUUID(),
        inntekthistorikk = Inntekthistorikk(),
        tidslinjer = mutableListOf(),
        perioder = mutableListOf(),
        aktivitetslogger = Aktivitetslogger()
    )

    internal fun accept(visitor: ArbeidsgiverVisitor) {
        visitor.preVisitArbeidsgiver(this, id, organisasjonsnummer)
        visitor.visitArbeidsgiverAktivitetslogger(aktivitetslogger)
        inntekthistorikk.accept(visitor)
        visitor.preVisitTidslinjer()
        tidslinjer.forEach { it.accept(visitor) }
        visitor.postVisitTidslinjer()
        visitor.preVisitPerioder()
        perioder.forEach { it.accept(visitor) }
        visitor.postVisitPerioder()
        visitor.postVisitArbeidsgiver(this, id, organisasjonsnummer)
    }

    internal fun organisasjonsnummer() = organisasjonsnummer

    internal fun peekTidslinje() = tidslinjer.last()

    internal fun push(tidslinje: Utbetalingstidslinje) = tidslinjer.add(tidslinje)

    internal fun håndter(sykmelding: Sykmelding) {
        if (!perioder.fold(false) { håndtert, periode -> håndtert || periode.håndter(sykmelding) }) {
            aktivitetslogger.infoOld("Lager ny vedtaksperiode")
            nyVedtaksperiode(sykmelding).håndter(sykmelding)
        }
        sykmelding.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(søknad: Søknad) {
        if (perioder.none { it.håndter(søknad) }) {
            søknad.errorOld("Forventet ikke søknad. Har nok ikke mottatt sykmelding")
        }
        søknad.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(inntektsmelding: Inntektsmelding) {
        inntekthistorikk.add(
            inntektsmelding.førsteFraværsdag.minusDays(1),  // Assuming salary is the day before the first sykedag
            inntektsmelding.meldingsreferanseId(),
            inntektsmelding.beregnetInntekt.toBigDecimal()
        )
        if (perioder.none { it.håndter(inntektsmelding) }) {
            inntektsmelding.errorOld("Forventet ikke inntektsmelding. Har nok ikke mottatt sykmelding")
        }
        inntektsmelding.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(ytelser: Ytelser) {
        ytelser.addInntekter(inntekthistorikk)
        perioder.forEach { it.håndter(ytelser) }
        ytelser.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(manuellSaksbehandling: ManuellSaksbehandling) {
        perioder.forEach { it.håndter(manuellSaksbehandling) }
        manuellSaksbehandling.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        perioder.forEach { it.håndter(vilkårsgrunnlag) }
        vilkårsgrunnlag.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(utbetaling: Utbetaling) {
        perioder.forEach { it.håndter(utbetaling) }
        utbetaling.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(påminnelse: Påminnelse) =
        perioder.any { it.håndter(påminnelse) }.also {
            påminnelse.kopierAktiviteterTil(aktivitetslogger)
        }

    internal fun sykdomstidslinje(): ConcreteSykdomstidslinje? =
        Vedtaksperiode.sykdomstidslinje(perioder)

    internal fun inntekt(dato: LocalDate): BigDecimal? =
        inntekthistorikk.inntekt(dato)

    internal fun invaliderPerioder(hendelse: ArbeidstakerHendelse) {
        perioder.forEach { it.invaliderPeriode(hendelse) }
    }

    private fun nyVedtaksperiode(sykmelding: Sykmelding): Vedtaksperiode {
        return Vedtaksperiode(
            person = person,
            arbeidsgiver = this,
            id = UUID.randomUUID(),
            aktørId = sykmelding.aktørId(),
            fødselsnummer = sykmelding.fødselsnummer(),
            organisasjonsnummer = sykmelding.organisasjonsnummer()
        ).also {
            perioder.add(it)
        }
    }

    internal fun tilstøtende(vedtaksperiode: Vedtaksperiode) =
        Vedtaksperiode.tilstøtendePeriode(vedtaksperiode, perioder)

    internal fun tidligerePerioderFerdigBehandlet(vedtaksperiode: Vedtaksperiode) =
        perioder.all { it.erFerdigBehandlet(vedtaksperiode) }

    internal fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
        perioder.forEach { it.håndter(this, vedtaksperiode, GjenopptaBehandling(hendelse)) }
    }

    internal class GjenopptaBehandling(val hendelse: ArbeidstakerHendelse)
}
