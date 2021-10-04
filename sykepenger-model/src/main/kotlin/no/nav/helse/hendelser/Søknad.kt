package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.tournament.Dagturnering
import no.nav.helse.økonomi.Prosentdel
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Søknad(
    meldingsreferanseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val perioder: List<Søknadsperiode>,
    private val andreInntektskilder: List<Inntektskilde>,
    private val sendtTilNAV: LocalDateTime,
    private val permittert: Boolean,
    private val merknaderFraSykmelding: List<Merknad>,
    sykmeldingSkrevet: LocalDateTime
) : SykdomstidslinjeHendelse(meldingsreferanseId, sykmeldingSkrevet) {

    private val sykdomsperiode: Periode
    private val sykdomstidslinje: Sykdomstidslinje
    private var sykdomstidslinjeUtenUønsketFerieIForkant: Sykdomstidslinje? = null

    private companion object {
        private const val tidslinjegrense = 40L
    }

    init {
        if (perioder.isEmpty()) severe("Søknad må inneholde perioder")
        sykdomsperiode = Søknadsperiode.sykdomsperiode(perioder) ?: severe("Søknad inneholder ikke sykdomsperioder")
        sykdomstidslinje = perioder
            .map { it.sykdomstidslinje(avskjæringsdato(), kilde) }
            .filter { it.periode()?.start?.isAfter(sykdomsperiode.start.minusDays(tidslinjegrense)) ?: false }
            .merge(Dagturnering.SØKNAD::beste)
    }

    override fun forGammel() = (sykdomsperiode.endInclusive < avskjæringsdato()).also {
        if (it) error("Søknaden kan ikke være eldre enn avskjæringsdato")
    }

    override fun sykdomstidslinje() =
        sykdomstidslinjeUtenUønsketFerieIForkant ?: sykdomstidslinje

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun validerIkkeOppgittFlereArbeidsforholdMedSykmelding(): IAktivitetslogg {
        andreInntektskilder.forEach { it.validerIkkeSykmeldt(this) }
        return this
    }

    override fun valider(periode: Periode): IAktivitetslogg {
        perioder.forEach { it.valider(this) }
        andreInntektskilder.forEach { it.valider(this) }
        if (permittert) warn("Søknaden inneholder permittering. Vurder om permittering har konsekvens for rett til sykepenger")
        if (merknaderFraSykmelding.any { it.type == "UGYLDIG_TILBAKEDATERING" || it.type == "TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER" }) {
            warn("Sykmeldingen er tilbakedatert, vurder fra og med dato for utbetaling.")
        }
        if (sykdomstidslinje.any { it is Dag.ForeldetSykedag }) warn("Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd")
        return this
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun melding(klassName: String) = "Søknad"

    internal fun harArbeidsdager() = perioder.filterIsInstance<Søknadsperiode.Arbeid>().isNotEmpty()

    internal fun feriedagerIForkantAvSykmeldingsperiode(): Sykdomstidslinje? {
        val sykmeldingFom = sykdomsperiode.start
        return sykdomstidslinje.kunFeriedager().filter { it.key < sykmeldingFom }.let { if (it.isNotEmpty()) Sykdomstidslinje(it) else null}
    }

    // registrerer feriedager i forkant av sykmeldingsperioden i søknaden som vi ikke ønsker å beholde
    // kan fjernes når søkere ikke lenger har anledning til å oppgi slik informasjon
    internal fun leggTilFeriedagerSomIkkeSkalVæreMedISykdomstidslinja(feriedagerÅFjerne: Sykdomstidslinje) {
        sykdomstidslinjeUtenUønsketFerieIForkant = sykdomstidslinje.filtrerVekk(feriedagerÅFjerne)
        info("Feriedager oppgitt i forkant av sykmeldingsperiode, oversees")
    }

    private fun avskjæringsdato(): LocalDate = sendtTilNAV.toLocalDate().minusMonths(3).withDayOfMonth(1)

    data class Merknad(val type: String, val beskrivelse: String?)

    sealed class Søknadsperiode(fom: LocalDate, tom: LocalDate) {
        protected val periode = Periode(fom, tom)

        internal companion object {
            fun sykdomsperiode(liste: List<Søknadsperiode>) =
                søknadsperiode(liste.filterIsInstance<Sykdom>())

            fun søknadsperiode(liste: List<Søknadsperiode>) =
                liste
                    .map(Søknadsperiode::periode)
                    .takeIf(List<*>::isNotEmpty)
                    ?.let {
                        it.reduce { champion, challenger ->
                            Periode(
                                fom = minOf(champion.start, challenger.start),
                                tom = maxOf(champion.endInclusive, challenger.endInclusive)
                            )
                        }
                    }
        }

        internal open fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde): Sykdomstidslinje =
            Sykdomstidslinje.problemdager(periode.start, periode.endInclusive, kilde, "Dagtype støttes ikke av systemet")

        internal open fun valider(søknad: Søknad) {}

        internal fun valider(søknad: Søknad, beskjed: String) {
            if (periode.utenfor(søknad.sykdomsperiode)) søknad.warn(beskjed)
        }

        class Sykdom(
            fom: LocalDate,
            tom: LocalDate,
            private val sykmeldingsgrad: Prosentdel,
            arbeidshelse: Prosentdel? = null
        ) : Søknadsperiode(fom, tom) {
            private val søknadsgrad = arbeidshelse?.not()
            private val sykdomsgrad = søknadsgrad ?: sykmeldingsgrad

            override fun valider(søknad: Søknad) {
                if (søknadsgrad != null && søknadsgrad > sykmeldingsgrad) søknad.error("Bruker har oppgitt at de har jobbet mindre enn sykmelding tilsier")
            }

            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.sykedager(periode.start, periode.endInclusive, avskjæringsdato, sykdomsgrad, kilde)
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {

            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.feriedager(periode.start, periode.endInclusive, kilde)
        }

        class Papirsykmelding(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.problemdager(periode.start, periode.endInclusive, kilde, "Papirdager ikke støttet")

            override fun valider(søknad: Søknad) =
                søknad.error("Søknaden inneholder en Papirsykmeldingsperiode")
        }

        class Utdanning(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.ukjent(periode.start, periode.endInclusive, kilde)

            override fun valider(søknad: Søknad) =
                søknad.warn("Utdanning oppgitt i perioden i søknaden.")
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.permisjonsdager(periode.start, periode.endInclusive, kilde)

            override fun valider(søknad: Søknad) {
                valider(søknad, "Søknaden inneholder Permisjonsdager utenfor sykdomsvindu")
                søknad.warn("Permisjon oppgitt i perioden i søknaden.")
            }
        }

        class Egenmelding(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun valider(søknad: Søknad) {
                if (periode.start < søknad.sykdomsperiode.start.minusDays(tidslinjegrense)) {
                    søknad.info("Søknaden inneholder egenmeldingsdager som er mer enn $tidslinjegrense dager før sykmeldingsperioden")
                }
                if (periode.endInclusive > søknad.sykdomsperiode.endInclusive) {
                    søknad.warn("Søknaden inneholder egenmeldingsdager etter sykmeldingsperioden")
                }
            }

            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje()
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun valider(søknad: Søknad) =
                valider(søknad, "Søknaden inneholder Arbeidsdager utenfor sykdomsvindu")

            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.arbeidsdager(periode.start, periode.endInclusive, kilde)
        }

        class Utlandsopphold(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.ukjent(periode.start, periode.endInclusive, kilde)

            override fun valider(søknad: Søknad) {
                søknad.warn("Utenlandsopphold oppgitt i perioden i søknaden.")
            }
        }
    }

    class Inntektskilde(
        private val sykmeldt: Boolean,
        private val type: String
    ) {
        fun valider(aktivitetslogg: IAktivitetslogg) {
            if (type != "ANDRE_ARBEIDSFORHOLD") {
                aktivitetslogg.error("Søknaden inneholder andre inntektskilder enn ANDRE_ARBEIDSFORHOLD")
            }
        }

        fun validerIkkeSykmeldt(aktivitetslogg: IAktivitetslogg) {
            if (sykmeldt) aktivitetslogg.error("Søknaden inneholder ANDRE_ARBEIDSFORHOLD med sykdom, men vi kjenner kun til sykdom ved ett arbeidsforhold")
        }
    }
}
