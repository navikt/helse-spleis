package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.tournament.søknadDagturnering
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Søknad constructor(
    meldingsreferanseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val perioder: List<Søknadsperiode>,
    private val harAndreInntektskilder: Boolean,
    private val sendtTilNAV: LocalDateTime,
    private val permittert: Boolean
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private val sykdomsperiode: Periode
    private val sykdomstidslinje: Sykdomstidslinje
    private var forrigeTom: LocalDate? = null

    private companion object {
        private const val tidslinjegrense = 16L
    }

    init {
        if (perioder.isEmpty()) severe("Søknad må inneholde perioder")
        perioder.onEach { it.sjekkUgyldig(aktivitetslogg) }
        sykdomsperiode = Søknadsperiode.sykdomsperiode(perioder) ?: severe("Søknad inneholder ikke sykdomsperioder")
        sykdomstidslinje = perioder
            .map { it.sykdomstidslinje(avskjæringsdato(), kilde) }
            .filter { it.periode()?.start?.isAfter(sykdomsperiode.start.minusDays(tidslinjegrense)) ?: false }
            .merge(søknadDagturnering::beste)
    }

    override fun sykdomstidslinje(tom: LocalDate): Sykdomstidslinje {
        require(forrigeTom == null || (forrigeTom != null && tom > forrigeTom)) { "Kalte metoden flere ganger med samme eller en tidligere dato" }

        return (forrigeTom?.let { sykdomstidslinje.subset(Periode(it.plusDays(1), tom))} ?: sykdomstidslinje.kutt(tom))
            .also { trimLeft(tom) }
            .also { it.periode() ?: severe("Ugyldig subsetting av tidslinjen til søknad") }
    }

    override fun sykdomstidslinje() = sykdomstidslinje

    internal fun trimLeft(dato: LocalDate) { forrigeTom = dato }

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun aktørId() = aktørId

    override fun valider(periode: Periode): Aktivitetslogg {
        perioder.forEach { it.valider(this) }
        if (harAndreInntektskilder) error("Søknaden inneholder andre inntektskilder")
        if (permittert) warn("Søknaden inneholder permittering. Vurder om permittering har konsekvens for rett til sykepenger")
        return aktivitetslogg
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun melding(klassName: String) = "Søknad"

    private fun avskjæringsdato(): LocalDate = sendtTilNAV.toLocalDate().minusMonths(3).withDayOfMonth(1)

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
                                tom = maxOf(champion.endInclusive, challenger.endInclusive))
                        }
                    }
        }

        internal open fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde): Sykdomstidslinje =
            Sykdomstidslinje.problemdager(periode.start, periode.endInclusive, kilde, "Dagtype støttes ikke av systemet")

        internal open fun sjekkUgyldig(aktivitetslogg: Aktivitetslogg) {}
        internal open fun valider(søknad: Søknad) {}

        internal fun valider(søknad: Søknad, beskjed: String) {
            if (periode.utenfor(søknad.sykdomsperiode)) søknad.error(beskjed)
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun valider(søknad: Søknad) =
                valider(søknad, "Søknaden inneholder Feriedager utenfor sykdomsvindu")

            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.feriedager(periode.start, periode.endInclusive, kilde)
        }

        class Sykdom(
            fom: LocalDate,
            tom: LocalDate,
            private val gradFraSykmelding: Int,
            faktiskSykdomsgrad: Int? = null
        ) : Søknadsperiode(fom, tom) {
            private val grad = (faktiskSykdomsgrad ?: gradFraSykmelding).toDouble()
            override fun sjekkUgyldig(aktivitetslogg: Aktivitetslogg) {
                if (grad > 100) aktivitetslogg.severe("Utregnet grad er over 100")
                if (grad < 0) aktivitetslogg.severe("Utregnet sykdomsgrad er et negativt tall")
            }

            override fun valider(søknad: Søknad) {
                if (grad > gradFraSykmelding) søknad.error("Bruker har oppgitt at de har jobbet mindre enn sykmelding tilsier")
            }

            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.sykedager(periode.start, periode.endInclusive, avskjæringsdato, grad, kilde)
        }

        class Papirsykmelding(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.problemdager(periode.start, periode.endInclusive, kilde, "Papirdager ikke støttet")

            override fun valider(søknad: Søknad) =
                søknad.error("Søknaden inneholder en Papirsykmeldingsperiode")
        }

        class Utdanning(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.problemdager(periode.start, periode.endInclusive, kilde, "Utdanningsdager ikke støttet")

            override fun valider(søknad: Søknad) =
                søknad.error("Søknaden inneholder en Utdanningsperiode")
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.problemdager(periode.start, periode.endInclusive, kilde, "Permisjonsdager ikke støttet")

            override fun valider(søknad: Søknad) =
                søknad.error("Søknaden inneholder en Permisjonsperiode")
        }

        class Egenmelding(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun valider(søknad: Søknad) {
                if (periode.start < søknad.sykdomsperiode.start.minusDays(tidslinjegrense)) søknad.info("Søknaden inneholder egenmeldingsdager som er mer enn $tidslinjegrense dager før sykmeldingsperioden")
                if (periode.endInclusive > søknad.sykdomsperiode.endInclusive) søknad.warn("Søknaden inneholder egenmeldingsdager etter sykmeldingsperioden")
            }

            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.arbeidsgiverdager(periode.start, periode.endInclusive, 100.0, kilde)
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun valider(søknad: Søknad) =
                valider(søknad, "Søknaden inneholder Arbeidsdager utenfor sykdomsvindu")

            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.arbeidsdager(periode.start, periode.endInclusive, kilde)
        }

        class Utlandsopphold(fom: LocalDate, tom: LocalDate): Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.problemdager(periode.start, periode.endInclusive, kilde, "Utenlandsdager ikke støttet")

            override fun valider(søknad: Søknad) {
                søknad.error("Søknaden inneholder utenlandsopphold")
            }
        }
    }
}
