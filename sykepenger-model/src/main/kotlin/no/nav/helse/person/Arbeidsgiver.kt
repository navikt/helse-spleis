package no.nav.helse.person

import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelManuellSaksbehandling
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelPåminnelse
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.hendelser.ModelVilkårsgrunnlag
import no.nav.helse.hendelser.ModelYtelser
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal class Arbeidsgiver private constructor(
    private val organisasjonsnummer: String,
    private val id: UUID,
    private val inntekthistorikk: Inntekthistorikk,
    private val tidslinjer: MutableList<Utbetalingstidslinje>,
    private val perioder: MutableList<Vedtaksperiode>,
    private val vedtaksperiodeObservers: MutableList<VedtaksperiodeObserver>,
    private val aktivitetslogger: Aktivitetslogger
) {

    internal constructor(organisasjonsnummer: String) : this(
        organisasjonsnummer = organisasjonsnummer,
        id = UUID.randomUUID(),
        inntekthistorikk = Inntekthistorikk(),
        tidslinjer = mutableListOf(),
        perioder = mutableListOf(),
        vedtaksperiodeObservers = mutableListOf(),
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

    internal fun håndter(nySøknad: ModelNySøknad, arbeidsgiver: Arbeidsgiver) {
        if (!perioder.fold(false) { håndtert, periode -> håndtert || periode.håndter(nySøknad) }) {
            aktivitetslogger.info("Lager ny vedtaksperiode")
            nyVedtaksperiode(nySøknad).håndter(nySøknad)
        }
        nySøknad.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(sendtSøknad: ModelSendtSøknad, arbeidsgiver: Arbeidsgiver) {
        if (perioder.none { it.håndter(sendtSøknad) }) {
            sendtSøknad.error("Uventet sendt søknad, mangler ny søknad")
        }
        sendtSøknad.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(inntektsmelding: ModelInntektsmelding, arbeidsgiver: Arbeidsgiver) {
        inntekthistorikk.add(
            inntektsmelding.førsteFraværsdag,
            inntektsmelding,
            inntektsmelding.beregnetInntekt.toBigDecimal()
        )
        if (perioder.none { it.håndter(inntektsmelding) }) {
            inntektsmelding.error("Uventet inntektsmelding, mangler ny søknad")
        }
        inntektsmelding.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(person: Person, ytelser: ModelYtelser) {
        perioder.forEach { it.håndter(person, this, ytelser) }
        ytelser.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(manuellSaksbehandling: ModelManuellSaksbehandling) {
        perioder.forEach { it.håndter(manuellSaksbehandling) }
        manuellSaksbehandling.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(vilkårsgrunnlag: ModelVilkårsgrunnlag) {
        perioder.forEach { it.håndter(vilkårsgrunnlag) }
        vilkårsgrunnlag.kopierAktiviteterTil(aktivitetslogger)
    }

    internal fun håndter(påminnelse: ModelPåminnelse) =
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

    fun addObserver(observer: VedtaksperiodeObserver) {
        vedtaksperiodeObservers.add(observer)
        perioder.forEach { it.addVedtaksperiodeObserver(observer) }
    }

    private fun nyVedtaksperiode(hendelse: SykdomstidslinjeHendelse): Vedtaksperiode {
        return Vedtaksperiode.nyPeriode(hendelse).also {
            vedtaksperiodeObservers.forEach(it::addVedtaksperiodeObserver)
            perioder.add(it)
        }
    }

}
