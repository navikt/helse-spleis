package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.somFødselsnummer
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.tournament.Dagturnering
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.økonomi.Prosentdel

class Søknad(
    meldingsreferanseId: UUID,
    fnr: String,
    aktørId: String,
    orgnummer: String,
    private val perioder: List<Søknadsperiode>,
    private val andreInntektskilder: List<Inntektskilde>,
    private val sendtTilNAVEllerArbeidsgiver: LocalDateTime,
    private val permittert: Boolean,
    private val merknaderFraSykmelding: List<Merknad>,
    sykmeldingSkrevet: LocalDateTime
) : SykdomstidslinjeHendelse(meldingsreferanseId, fnr, aktørId, orgnummer, sykmeldingSkrevet, Søknad::class) {

    private val sykdomsperiode: Periode
    private val sykdomstidslinje: Sykdomstidslinje

    internal companion object {
        internal const val tidslinjegrense = 40L
        internal const val ERRORTEKST_PERSON_UNDER_18_ÅR = "Søker er ikke gammel nok på søknadstidspunktet til å søke sykepenger uten fullmakt fra verge"
    }

    init {
        if (perioder.isEmpty()) severe("Søknad må inneholde perioder")
        sykdomsperiode = Søknadsperiode.sykdomsperiode(perioder) ?: severe("Søknad inneholder ikke sykdomsperioder")
        sykdomstidslinje = perioder
            .map { it.sykdomstidslinje(sykdomsperiode, avskjæringsdato(), kilde) }
            .filter { it.periode()?.start?.isAfter(sykdomsperiode.start.minusDays(tidslinjegrense)) ?: false }
            .merge(Dagturnering.SØKNAD::beste)
            .subset(sykdomsperiode)
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    override fun sykdomstidslinje() = sykdomstidslinje

    override fun validerIkkeOppgittFlereArbeidsforholdMedSykmelding(): IAktivitetslogg {
        andreInntektskilder.forEach { it.validerIkkeSykmeldt(this) }
        return this
    }

    // registrerer feriedager i forkant av sykmeldingsperioden i søknaden som vi ikke ønsker å beholde
    // kan fjernes når søkere ikke lenger har anledning til å oppgi slik informasjon
    internal fun harUlikFerieinformasjon(other: Sykdomstidslinje) =
        Søknadsperiode.Ferie.harUlikFerieinformasjon(sykdomsperiode, perioder, other)

    internal fun harArbeidsdager() = perioder.filterIsInstance<Søknadsperiode.Arbeid>().isNotEmpty()

    override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver): IAktivitetslogg {
        perioder.forEach { it.subsumsjon(subsumsjonObserver) }
        perioder.forEach { it.valider(this) }
        andreInntektskilder.forEach { it.valider(this) }
        forUng(fødselsnummer.somFødselsnummer().alder())
        if (permittert) warn("Søknaden inneholder permittering. Vurder om permittering har konsekvens for rett til sykepenger")
        merknaderFraSykmelding.forEach { it.valider(this) }
        if (sykdomstidslinje.any { it is Dag.ForeldetSykedag }) warn("Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd")
        return this
    }

    private fun forUng(alder: Alder) = alder.forUngForÅSøke(sendtTilNAVEllerArbeidsgiver.toLocalDate()).also {
        if (it) error(ERRORTEKST_PERSON_UNDER_18_ÅR)
    }
    private fun avskjæringsdato(): LocalDate = sendtTilNAVEllerArbeidsgiver.toLocalDate().minusMonths(3).withDayOfMonth(1)

    override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.søknad(meldingsreferanseId()))
    }

    class Merknad(private val type: String, beskrivelse: String?) {
        internal fun valider(aktivitetslogg: IAktivitetslogg) {
            if (type == "UGYLDIG_TILBAKEDATERING" || type == "TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER") {
                aktivitetslogg.warn("Sykmeldingen er tilbakedatert, vurder fra og med dato for utbetaling.")
            }
        }
    }

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

        internal open fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde): Sykdomstidslinje =
            Sykdomstidslinje.problemdager(periode.start, periode.endInclusive, kilde, "Dagtype støttes ikke av systemet")

        internal open fun valider(søknad: Søknad) {}

        internal fun valider(søknad: Søknad, beskjed: String) {
            if (periode.utenfor(søknad.sykdomsperiode)) søknad.warn(beskjed)
        }

        internal open fun subsumsjon(subsumsjonObserver: SubsumsjonObserver) {}

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

            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.sykedager(periode.start, periode.endInclusive, avskjæringsdato, sykdomsgrad, kilde)
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            internal companion object {
                internal fun harUlikFerieinformasjon(sykdomsperiode: Periode, perioder: List<Søknadsperiode>, other: Sykdomstidslinje) =
                    perioder.filterIsInstance<Ferie>()
                        .filter { it.periode.start < sykdomsperiode.start }
                        .any { Sykdomstidslinje.ulikFerieinformasjon(other, it.periode.start til minOf(sykdomsperiode.start, it.periode.endInclusive)) }
            }
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.feriedager(periode.start, periode.endInclusive, kilde).subset(sykdomsperiode.oppdaterTom(periode))
        }

        class Papirsykmelding(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.problemdager(periode.start, periode.endInclusive, kilde, "Papirdager ikke støttet")

            override fun valider(søknad: Søknad) =
                søknad.error("Søknaden inneholder en Papirsykmeldingsperiode")
        }

        class Utdanning(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.ukjent(periode.start, periode.endInclusive, kilde)

            override fun valider(søknad: Søknad) =
                søknad.warn("Utdanning oppgitt i perioden i søknaden.")
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.permisjonsdager(periode.start, periode.endInclusive, kilde)

            override fun valider(søknad: Søknad) {
                valider(søknad, "Søknaden inneholder Permisjonsdager utenfor sykdomsvindu")
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

            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje()
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun valider(søknad: Søknad) =
                valider(søknad, "Søknaden inneholder Arbeidsdager utenfor sykdomsvindu")

            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.arbeidsdager(periode.start, periode.endInclusive, kilde)
        }

        class Utlandsopphold(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.ukjent(periode.start, periode.endInclusive, kilde)

            override fun valider(søknad: Søknad) {
                søknad.warn("Utenlandsopphold oppgitt i perioden i søknaden.")
            }

            override fun subsumsjon(subsumsjonObserver: SubsumsjonObserver) {
                subsumsjonObserver.`§ 8-9 ledd 1`(false, periode)
            }
        }
    }

    class Inntektskilde(
        private val sykmeldt: Boolean,
        private val type: String
    ) {
        fun valider(aktivitetslogg: IAktivitetslogg) {
            if (type == "ANNET") {
                aktivitetslogg.warn("Det er oppgitt annen inntektskilde i søknaden. Vurder inntekt.")
            } else if (type != "ANDRE_ARBEIDSFORHOLD") {
                aktivitetslogg.error("Søknaden inneholder andre inntektskilder enn ANDRE_ARBEIDSFORHOLD")
            }
        }

        fun validerIkkeSykmeldt(aktivitetslogg: IAktivitetslogg) {
            if (sykmeldt) aktivitetslogg.error("Søknaden inneholder ANDRE_ARBEIDSFORHOLD med sykdom, men vi kjenner kun til sykdom ved ett arbeidsforhold")
        }
    }
}
