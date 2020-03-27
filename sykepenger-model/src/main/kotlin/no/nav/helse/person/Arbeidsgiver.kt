package no.nav.helse.person

import no.nav.helse.hendelser.*
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
    private val perioder: MutableList<Vedtaksperiode>
) : Aktivitetskontekst {

    internal fun inntektshistorikk() = inntekthistorikk.clone()

    internal constructor(person: Person, organisasjonsnummer: String) : this(
        person = person,
        organisasjonsnummer = organisasjonsnummer,
        id = UUID.randomUUID(),
        inntekthistorikk = Inntekthistorikk(),
        tidslinjer = mutableListOf(),
        perioder = mutableListOf()
    )

    internal fun accept(visitor: ArbeidsgiverVisitor) {
        visitor.preVisitArbeidsgiver(this, id, organisasjonsnummer)
        inntekthistorikk.accept(visitor)
        visitor.preVisitTidslinjer(tidslinjer)
        tidslinjer.forEach { it.accept(visitor) }
        visitor.postVisitTidslinjer(tidslinjer)
        visitor.preVisitPerioder()
        perioder.forEach { it.accept(visitor) }
        visitor.postVisitPerioder()
        visitor.postVisitArbeidsgiver(this, id, organisasjonsnummer)
    }

    internal fun organisasjonsnummer() = organisasjonsnummer

    internal fun peekTidslinje() = tidslinjer.last()

    internal fun push(tidslinje: Utbetalingstidslinje) = tidslinjer.add(tidslinje)

    internal fun håndter(sykmelding: Sykmelding) {
        sykmelding.kontekst(this)
        if(perioder.map { it.håndter(sykmelding)}.none { it } ) {
            sykmelding.info("Lager ny vedtaksperiode")
            nyVedtaksperiode(sykmelding).håndter(sykmelding)
            Vedtaksperiode.sorter(perioder)
        }
    }

    internal fun håndter(søknad: Søknad) {
        søknad.kontekst(this)
        if(perioder.map { it.håndter(søknad)}.none { it } ) {
            søknad.error("Forventet ikke søknad. Har nok ikke mottatt sykmelding")
        }
    }

    internal fun håndter(søknad: SøknadArbeidsgiver) {
        søknad.kontekst(this)
        if(perioder.map { it.håndter(søknad)}.none { it } ) {
            søknad.error("Forventet ikke søknad til arbeidsgiver. Har nok ikke mottatt sykmelding")
        }
    }

    internal fun håndter(inntektsmelding: Inntektsmelding) {
        inntektsmelding.kontekst(this)
        if(perioder.map { it.håndter(inntektsmelding)}.none { it } ) {
            inntektsmelding.error("Forventet ikke inntektsmelding. Har nok ikke mottatt sykmelding")
        }
    }

    internal fun håndter(ytelser: Ytelser) {
        ytelser.kontekst(this)
        perioder.forEach { it.håndter(ytelser) }
    }

    internal fun håndter(manuellSaksbehandling: ManuellSaksbehandling) {
        manuellSaksbehandling.kontekst(this)
        perioder.forEach { it.håndter(manuellSaksbehandling) }
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        vilkårsgrunnlag.kontekst(this)
        perioder.forEach { it.håndter(vilkårsgrunnlag) }
    }

    internal fun håndter(simulering: Simulering) {
        simulering.kontekst(this)
        perioder.forEach { it.håndter(simulering) }
    }

    internal fun håndter(utbetaling: Utbetaling) {
        utbetaling.kontekst(this)
        perioder.forEach { it.håndter(utbetaling) }
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        påminnelse.kontekst(this)
        return perioder.any { it.håndter(påminnelse) }
    }

    internal fun sykdomstidslinje() = Vedtaksperiode.sykdomstidslinje(perioder)

    internal fun inntekt(dato: LocalDate): BigDecimal? =
        inntekthistorikk.inntekt(dato)

    internal fun invaliderPerioder(hendelse: ArbeidstakerHendelse) {
        perioder.forEach { it.invaliderPeriode(hendelse) }
    }

    internal fun addInntekt(inntektsmelding: Inntektsmelding) {
        inntektsmelding.addInntekt(inntekthistorikk)
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
        perioder.forEach { it.håndter(vedtaksperiode, GjenopptaBehandling(hendelse)) }
    }

    internal class GjenopptaBehandling(internal val hendelse: ArbeidstakerHendelse)

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Arbeidsgiver", mapOf("organisasjonsnummer" to organisasjonsnummer))
    }
}
