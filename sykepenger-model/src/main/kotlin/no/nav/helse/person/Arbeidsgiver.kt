package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Validation.Companion.validation
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetaling
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.utbetalte
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class Arbeidsgiver private constructor(
    private val person: Person,
    private val organisasjonsnummer: String,
    private val id: UUID,
    private val inntekthistorikk: Inntekthistorikk,
    private val sykdomshistorikk: Sykdomshistorikk,
    private val perioder: MutableList<Vedtaksperiode>,
    private val forkastede: MutableList<Vedtaksperiode>,
    private val utbetalinger: MutableList<Utbetaling>
) : Aktivitetskontekst {

    internal fun inntektshistorikk() = inntekthistorikk.clone()

    internal constructor(person: Person, organisasjonsnummer: String) : this(
        person = person,
        organisasjonsnummer = organisasjonsnummer,
        id = UUID.randomUUID(),
        inntekthistorikk = Inntekthistorikk(),
        sykdomshistorikk = Sykdomshistorikk(),
        perioder = mutableListOf(),
        forkastede = mutableListOf(),
        utbetalinger = mutableListOf()
    )

    internal fun accept(visitor: ArbeidsgiverVisitor) {
        visitor.preVisitArbeidsgiver(this, id, organisasjonsnummer)
        inntekthistorikk.accept(visitor)
        sykdomshistorikk.accept(visitor)
        visitor.preVisitUtbetalinger(utbetalinger)
        utbetalinger.forEach { it.accept(visitor) }
        visitor.postVisitUtbetalinger(utbetalinger)
        visitor.preVisitPerioder(perioder)
        perioder.forEach { it.accept(visitor) }
        visitor.postVisitPerioder(perioder)
        visitor.preVisitForkastedePerioder(forkastede)
        forkastede.forEach { it.accept(visitor) }
        visitor.postVisitForkastedePerioder(forkastede)
        visitor.postVisitArbeidsgiver(this, id, organisasjonsnummer)
    }

    internal fun organisasjonsnummer() = organisasjonsnummer

    internal fun utbetaling() = utbetalinger.lastOrNull()

    internal fun utbetalteUtbetalinger() = utbetalinger.utbetalte()

    internal fun nåværendeTidslinje() =
        utbetaling()?.utbetalingstidslinje() ?: throw IllegalStateException("mangler utbetalinger")

    internal fun push(utbetaling: Utbetaling) = utbetalinger.add(utbetaling)

    private fun validerSykdomstidslinjer() = perioder.forEach {
        it.validerSykdomstidslinje(sykdomshistorikk.sykdomstidslinje())
    }

    internal fun håndter(sykmelding: Sykmelding) {
        sykmelding.kontekst(this)
        if (perioder.toList().map { it.håndter(sykmelding) }.none { it }) {
            sykmelding.info("Lager ny vedtaksperiode")
            nyVedtaksperiode(sykmelding).håndter(sykmelding)
            Vedtaksperiode.sorter(perioder)
        }
        validation(sykmelding) {
            onSuccess {
                sykdomshistorikk.nyHåndter(sykmelding)
                validerSykdomstidslinjer()
            }
        }
    }

    internal fun håndter(søknad: Søknad) {
        søknad.kontekst(this)
        validation(søknad) {
            valider("Forventet ikke søknad. Har nok ikke mottatt sykmelding") {
                perioder.toList().map { it.håndter(søknad) }.any { it }
            }
            onSuccess {
                sykdomshistorikk.nyHåndter(søknad)
                validerSykdomstidslinjer()
            }
        }
    }

    internal fun håndter(søknad: SøknadArbeidsgiver) {
        søknad.kontekst(this)
        validation(søknad) {
            valider("Forventet ikke søknad til arbeidsgiver. Har nok ikke mottatt sykmelding") {
                perioder.toList().map { it.håndter(søknad) }.any { it }
            }
            onSuccess {
                sykdomshistorikk.nyHåndter(søknad)
                validerSykdomstidslinjer()
            }
        }
    }

    internal fun håndter(inntektsmelding: Inntektsmelding) {
        inntektsmelding.kontekst(this)
        validation(inntektsmelding) {
            valider("Forventet ikke inntektsmelding. Har nok ikke mottatt sykmelding") {
                perioder.toList().map { it.håndter(inntektsmelding) }.any { it }
            }
            onSuccess {
                sykdomshistorikk.nyHåndter(inntektsmelding)
                validerSykdomstidslinjer()
            }
        }
    }

    internal fun håndter(utbetalingshistorikk: Utbetalingshistorikk) {
        utbetalingshistorikk.kontekst(this)
        perioder.toList().forEach { it.håndter(utbetalingshistorikk) }
    }

    internal fun håndter(ytelser: Ytelser) {
        ytelser.kontekst(this)
        perioder.toList().forEach { it.håndter(ytelser) }
    }

    internal fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        utbetalingsgodkjenning.kontekst(this)
        perioder.toList().forEach { it.håndter(utbetalingsgodkjenning) }
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        vilkårsgrunnlag.kontekst(this)
        perioder.toList().forEach { it.håndter(vilkårsgrunnlag) }
    }

    internal fun håndter(simulering: Simulering) {
        simulering.kontekst(this)
        perioder.toList().forEach { it.håndter(simulering) }
    }

    internal fun håndter(utbetaling: UtbetalingOverført) {
        utbetaling.kontekst(this)
        perioder.toList().forEach { it.håndter(utbetaling) }
    }

    internal fun håndter(utbetaling: UtbetalingHendelse) {
        utbetaling.kontekst(this)
        perioder.toList().forEach { it.håndter(utbetaling) }
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        påminnelse.kontekst(this)
        return perioder.toList().any { it.håndter(påminnelse) }
    }

    internal fun håndter(kansellerUtbetaling: KansellerUtbetaling) {
        // TODO: Håndterer kun arbeidsgiverOppdrag p.t. Må på sikt håndtere personOppdrag
        kansellerUtbetaling.kontekst(this)
        utbetalinger.reversed().firstOrNull {
            it.arbeidsgiverOppdrag().fagsystemId() == kansellerUtbetaling.fagsystemId
        }
            ?.kansellerUtbetaling()
            ?.also {
                kansellerUtbetaling.info("Annullerer utbetalinger med fagsystemId ${kansellerUtbetaling.fagsystemId}")
                utbetalinger.add(it)
                utbetaling(
                    aktivitetslogg = kansellerUtbetaling.aktivitetslogg,
                    oppdrag = it.arbeidsgiverOppdrag(),
                    saksbehandler = kansellerUtbetaling.saksbehandler
                )
                perioder.toList().forEach { it.håndter(kansellerUtbetaling) }
            }
            ?: kansellerUtbetaling.error(
                "Avvis hvis vi ikke finner fagsystemId %s",
                kansellerUtbetaling.fagsystemId
            )
    }

    internal fun håndter(hendelse: Rollback) {
        hendelse.kontekst(this)
        perioder.toList().forEach { it.forkast(TilbakestillBehandling(organisasjonsnummer, hendelse)) }
    }

    internal fun sykdomstidslinje() = Vedtaksperiode.sykdomstidslinje(perioder)

    internal fun inntekt(dato: LocalDate): BigDecimal? =
        inntekthistorikk.inntekt(dato)

    internal fun sykepengegrunnlag(dato: LocalDate): Double? = inntekthistorikk.sykepengegrunnlag(dato)

    internal fun forkastPerioder(hendelse: ArbeidstakerHendelse) {
        hendelse.kontekst(this)
        perioder.toList().forEach { it.forkast(hendelse) }
    }

    internal fun forkast(vedtaksperiode: Vedtaksperiode) {
        if (!perioder.remove(vedtaksperiode)) return
        sykdomshistorikk.fjernTidligereDager(vedtaksperiode.periode())
        forkastede.add(vedtaksperiode)
        Vedtaksperiode.sorter(forkastede)
    }

    internal fun forkastPåfølgendeUnntattNye(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
        forkast(vedtaksperiode)
        perioder.toList().forEach { it.forkastHvisPåfølgendeUnntattNy(vedtaksperiode, hendelse) }
    }

    internal fun forkastAlleEtterfølgende(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
        forkast(vedtaksperiode)
        perioder.toList().forEach { it.forkastHvisEtterfølgende(vedtaksperiode, hendelse) }
    }

    internal fun addInntekt(inntektsmelding: Inntektsmelding) {
        inntektsmelding.addInntekt(inntekthistorikk)
    }

    internal fun addInntekt(ytelser: Ytelser) {
        ytelser.addInntekt(organisasjonsnummer, inntekthistorikk)
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

    internal fun finnForegåendePeriode(vedtaksperiode: Vedtaksperiode) =
        perioder.firstOrNull { other -> other.etterfølgesAv(vedtaksperiode) }

    internal fun finnPåfølgendePeriode(vedtaksperiode: Vedtaksperiode) =
        perioder.firstOrNull { other -> vedtaksperiode.etterfølgesAv(other) }

    internal fun harPerioderSomStarterEtter(vedtaksperiode: Vedtaksperiode) =
        perioder.any { it.starterSenereEnn(vedtaksperiode) }

    internal fun tidligerePerioderFerdigBehandlet(vedtaksperiode: Vedtaksperiode) =
        Vedtaksperiode.tidligerePerioderFerdigBehandlet(perioder, vedtaksperiode)

    internal fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
        perioder.toList().forEach { it.håndter(vedtaksperiode, GjenopptaBehandling(hendelse)) }
    }

    internal class GjenopptaBehandling(internal val hendelse: ArbeidstakerHendelse)
    internal class TilbakestillBehandling(
        internal val organisasjonsnummer: String,
        internal val hendelse: PersonHendelse
    ) : ArbeidstakerHendelse(hendelse.aktivitetslogg) {
        override fun organisasjonsnummer() = organisasjonsnummer
        override fun aktørId() = hendelse.aktørId()
        override fun fødselsnummer() = hendelse.fødselsnummer()
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Arbeidsgiver", mapOf("organisasjonsnummer" to organisasjonsnummer))
    }

    internal fun lås(periode: Periode) {
        sykdomshistorikk.sykdomstidslinje().lås(periode)
    }
}
