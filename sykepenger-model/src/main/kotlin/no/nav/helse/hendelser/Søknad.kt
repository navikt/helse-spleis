package no.nav.helse.hendelser

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Companion.inneholderDagerEtter
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Companion.subsumsjonsFormat
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.Personopplysninger
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.*
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.tournament.Dagturnering
import no.nav.helse.Alder
import no.nav.helse.økonomi.Prosentdel

class Søknad(
    meldingsreferanseId: UUID,
    fnr: String,
    aktørId: String,
    personopplysninger: Personopplysninger,
    orgnummer: String,
    private val perioder: List<Søknadsperiode>,
    private val andreInntektskilder: Boolean,
    private val sendtTilNAVEllerArbeidsgiver: LocalDateTime,
    private val permittert: Boolean,
    private val merknaderFraSykmelding: List<Merknad>,
    sykmeldingSkrevet: LocalDateTime,
    private val korrigerer: UUID?,
    private val opprinneligSendt: LocalDateTime?,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : SykdomstidslinjeHendelse(meldingsreferanseId, fnr, aktørId, orgnummer, sykmeldingSkrevet, Søknad::class, aktivitetslogg, personopplysninger) {

    private val sykdomsperiode: Periode
    private val sykdomstidslinje: Sykdomstidslinje

    internal companion object {
        internal const val tidslinjegrense = 40L
    }

    init {
        if (perioder.isEmpty()) logiskFeil("Søknad må inneholde perioder")
        sykdomsperiode = Søknadsperiode.sykdomsperiode(perioder) ?: logiskFeil("Søknad inneholder ikke sykdomsperioder")
        if (perioder.inneholderDagerEtter(sykdomsperiode.endInclusive)) logiskFeil("Søknad inneholder dager etter siste sykdomsdag")
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

    internal fun harArbeidsdager() = perioder.filterIsInstance<Søknadsperiode.Arbeid>().isNotEmpty()

    override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver): IAktivitetslogg {
        perioder.forEach { it.subsumsjon(this.perioder.subsumsjonsFormat(), subsumsjonObserver) }
        perioder.forEach { it.valider(this) }
        if (permittert) varsel(RV_SØ_1)
        merknaderFraSykmelding.forEach { it.valider(this) }
        if (sykdomstidslinje.any { it is Dag.ForeldetSykedag }) varsel(RV_SØ_2)
        return this
    }

    internal fun validerInntektskilder(manglerVilkårsgrunnlag: Boolean): IAktivitetslogg {
        if(andreInntektskilder){
            if(manglerVilkårsgrunnlag){
                this.funksjonellFeil(RV_SØ_10)
            } else {
                this.varsel(RV_SØ_10)
            }
        }
        return this
    }

    internal fun forUng(alder: Alder) = alder.forUngForÅSøke(sendtTilNAVEllerArbeidsgiver.toLocalDate()).also {
        if (it) funksjonellFeil(RV_SØ_17)
    }
    private fun avskjæringsdato(): LocalDate = when (korrigerer) {
        null -> sendtTilNAVEllerArbeidsgiver.toLocalDate().minusMonths(3).withDayOfMonth(1)
        else -> opprinneligSendt?.toLocalDate()?.minusMonths(3)?.withDayOfMonth(1) ?: LocalDate.MIN
        //else -> LocalDate.MIN // det er tidspunktet den originale søknaden ble sendt inn som er bestemmende for foreldelse
    }

    override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.søknad(meldingsreferanseId()))
    }

    internal fun lagVedtaksperiode(person: Person, arbeidsgiver: Arbeidsgiver, jurist: MaskinellJurist): Vedtaksperiode {
        val periode = requireNotNull(sykdomstidslinje.periode()) { "ugyldig søknad: tidslinjen er tom" }
        return Vedtaksperiode(
            søknad = this,
            person = person,
            arbeidsgiver = arbeidsgiver,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            sykdomstidslinje = sykdomstidslinje,
            dokumentsporing = Dokumentsporing.søknad(meldingsreferanseId()),
            periode = periode,
            jurist = jurist
        )
    }

    internal fun slettSykmeldingsperioderSomDekkes(sykmeldingsperioder: Sykmeldingsperioder, person: Person) {
        val sisteDag = periode().endInclusive
        sykmeldingsperioder.fjern(sisteDag)
        person.slettUtgåtteSykmeldingsperioder(sisteDag)
    }

    class Merknad(private val type: String) {
        internal fun valider(aktivitetslogg: IAktivitetslogg) {
            if (type == "UGYLDIG_TILBAKEDATERING" || type == "TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER") {
                aktivitetslogg.varsel(RV_SØ_3)
            }
        }
    }

    sealed class Søknadsperiode(fom: LocalDate, tom: LocalDate, private val navn: String) {
        protected val periode = Periode(fom, tom)

        internal companion object {
            fun sykdomsperiode(liste: List<Søknadsperiode>) =
                søknadsperiode(liste.filterIsInstance<Sykdom>())

            fun List<Søknadsperiode>.inneholderDagerEtter(sisteSykdomsdato: LocalDate) =
                any { it.periode.endInclusive > sisteSykdomsdato }

            fun List<Søknadsperiode>.subsumsjonsFormat(): List<Map<String, Serializable>> {
                return map { mapOf("fom" to it.periode.start, "tom" to it.periode.endInclusive, "type" to it.navn) }
            }

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

        internal abstract fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde): Sykdomstidslinje

        internal open fun valider(søknad: Søknad) {}

        internal fun valider(søknad: Søknad, varselkode: Varselkode) {
            if (periode.utenfor(søknad.sykdomsperiode)) søknad.varsel(varselkode)
        }

        internal open fun subsumsjon(søknadsperioder: List<Map<String, Serializable>>, subsumsjonObserver: SubsumsjonObserver) {}

        class Sykdom(
            fom: LocalDate,
            tom: LocalDate,
            sykmeldingsgrad: Prosentdel,
            arbeidshelse: Prosentdel? = null
        ) : Søknadsperiode(fom, tom, "sykdom") {
            private val søknadsgrad = arbeidshelse?.not()
            private val sykdomsgrad = søknadsgrad ?: sykmeldingsgrad

            init {
                if (søknadsgrad != null && søknadsgrad > sykmeldingsgrad) throw IllegalStateException("Bruker har oppgitt at de har jobbet mindre enn sykmelding tilsier")
            }

            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.sykedager(periode.start, periode.endInclusive, avskjæringsdato, sykdomsgrad, kilde)
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom, "ferie") {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.feriedager(periode.start, periode.endInclusive, kilde).subset(sykdomsperiode.oppdaterTom(periode))
        }

        class Papirsykmelding(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom, "papirsykmelding") {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.problemdager(periode.start, periode.endInclusive, kilde, "Papirdager ikke støttet")

            override fun valider(søknad: Søknad) =
                søknad.funksjonellFeil(RV_SØ_22)
        }

        class Utdanning(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom, "utdanning") {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.ukjent(periode.start, periode.endInclusive, kilde)

            override fun valider(søknad: Søknad) = søknad.varsel(RV_SØ_4)
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom, "permisjon") {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.permisjonsdager(periode.start, periode.endInclusive, kilde)

            override fun valider(søknad: Søknad) {
                valider(søknad, RV_SØ_5)
            }
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom, "arbeid") {
            override fun valider(søknad: Søknad) =
                valider(søknad, RV_SØ_7)

            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.arbeidsdager(periode.start, periode.endInclusive, kilde)
        }

        class Utlandsopphold(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom, "utlandsopphold") {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.ukjent(periode.start, periode.endInclusive, kilde)

            override fun valider(søknad: Søknad) {
                if (alleUtlandsdagerErFerie(søknad)) return
                søknad.varsel(RV_SØ_8)
            }

            override fun subsumsjon(søknadsperioder: List<Map<String, Serializable>>, subsumsjonObserver: SubsumsjonObserver) {
                subsumsjonObserver.`§ 8-9 ledd 1`(false, periode, søknadsperioder)
            }

            private fun alleUtlandsdagerErFerie(søknad:Søknad):Boolean {
                val feriePerioder = søknad.perioder.filterIsInstance<Ferie>()
                return this.periode.all { utlandsdag -> feriePerioder.any { ferie -> ferie.periode.contains(utlandsdag)} }
            }
        }
    }
}
