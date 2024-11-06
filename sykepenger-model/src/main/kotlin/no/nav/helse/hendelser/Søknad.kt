package no.nav.helse.hendelser

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.Alder
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 22-13 ledd 3`
import no.nav.helse.etterlevelse.`§ 8-9 ledd 1`
import no.nav.helse.hendelser.Periode.Companion.delvisOverlappMed
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Companion.inneholderDagerEtter
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Companion.subsumsjonsFormat
import no.nav.helse.nesteDag
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Person
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.*
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Arbeidsledigsøknad er lagt til grunn`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Støtter ikke førstegangsbehandlinger for arbeidsledigsøknader`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Støtter ikke søknadstypen`
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.NyInntektUnderveis
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.tournament.Dagturnering
import no.nav.helse.ukedager
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel

class Søknad(
    meldingsreferanseId: UUID,
    fnr: String,
    aktørId: String,
    private val orgnummer: String,
    private val perioder: List<Søknadsperiode>,
    private val andreInntektskilder: Boolean,
    private val ikkeJobbetIDetSisteFraAnnetArbeidsforhold: Boolean,
    sendtTilNAVEllerArbeidsgiver: LocalDateTime,
    private val permittert: Boolean,
    private val merknaderFraSykmelding: List<Merknad>,
    sykmeldingSkrevet: LocalDateTime,
    private val opprinneligSendt: LocalDateTime?,
    private val utenlandskSykmelding: Boolean,
    private val arbeidUtenforNorge: Boolean,
    private val sendTilGosys: Boolean,
    private val yrkesskade: Boolean,
    private val egenmeldinger: List<Periode>,
    private val søknadstype: Søknadstype,
    registrert: LocalDateTime,
    private val tilkomneInntekter: List<TilkommenInntekt>
) : SykdomstidslinjeHendelse() {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        fødselsnummer = fnr,
        aktørId = aktørId,
        organisasjonsnummer = orgnummer
    )
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = Avsender.SYKMELDT,
        innsendt = sendtTilNAVEllerArbeidsgiver,
        registrert = registrert,
        automatiskBehandling = false
    )

    private val kilde: Hendelseskilde = Hendelseskilde(this::class, metadata.meldingsreferanseId, sykmeldingSkrevet)
    private val sykdomsperiode: Periode
    private var sykdomstidslinje: Sykdomstidslinje

    internal companion object {
        internal const val tidslinjegrense = 40L
    }

    init {
        if (perioder.isEmpty()) error("Søknad må inneholde perioder")
        sykdomsperiode = Søknadsperiode.sykdomsperiode(perioder) ?: error("Søknad inneholder ikke sykdomsperioder")
        if (perioder.inneholderDagerEtter(sykdomsperiode.endInclusive)) error("Søknad inneholder dager etter siste sykdomsdag")

        sykdomstidslinje = perioder
            .map { it.sykdomstidslinje(sykdomsperiode, avskjæringsdato(), kilde) }
            .filter { it.periode()?.start?.isAfter(sykdomsperiode.start.minusDays(tidslinjegrense)) ?: false }
            .merge(Dagturnering.SØKNAD::beste)
            .subset(sykdomsperiode)
    }

    override fun erRelevant(other: Periode) = other.overlapperMed(sykdomsperiode)

    override fun sykdomstidslinje(): Sykdomstidslinje {
        return sykdomstidslinje
    }

    internal fun egenmeldingsperioder(): List<Periode> {
        return egenmeldinger
    }

    internal fun delvisOverlappende(other: Periode) = other.delvisOverlappMed(sykdomsperiode)

    internal fun valider(aktivitetslogg: IAktivitetslogg, vilkårsgrunnlag: VilkårsgrunnlagElement?, subsumsjonslogg: Subsumsjonslogg): IAktivitetslogg {
        valider(aktivitetslogg, subsumsjonslogg)
        validerInntektskilder(aktivitetslogg, vilkårsgrunnlag)
        søknadstype.valider(aktivitetslogg, vilkårsgrunnlag, orgnummer, sykdomstidslinje.periode())
        return aktivitetslogg
    }

    private fun valider(aktivitetslogg: IAktivitetslogg, subsumsjonslogg: Subsumsjonslogg): IAktivitetslogg {
        val utlandsopphold = perioder.filterIsInstance<Søknadsperiode.Utlandsopphold>().map { it.periode }
        subsumsjonslogg.logg(`§ 8-9 ledd 1`(false, utlandsopphold, this.perioder.subsumsjonsFormat()))
        perioder.forEach { it.valider(this, aktivitetslogg) }
        if (permittert) aktivitetslogg.varsel(RV_SØ_1)
        validerTilkomneInntekter(aktivitetslogg)
        merknaderFraSykmelding.forEach { it.valider(aktivitetslogg) }
        val foreldedeDager = ForeldetSubsumsjonsgrunnlag(sykdomstidslinje).build()
        if (foreldedeDager.isNotEmpty()) {
            subsumsjonslogg.logg(`§ 22-13 ledd 3`(avskjæringsdato(), foreldedeDager))
            aktivitetslogg.varsel(RV_SØ_2)
        }
        if (arbeidUtenforNorge) {
            aktivitetslogg.varsel(RV_MV_3)
        }
        if (utenlandskSykmelding) aktivitetslogg.funksjonellFeil(RV_SØ_29)
        if (sendTilGosys) aktivitetslogg.funksjonellFeil(RV_SØ_30)
        if (yrkesskade) aktivitetslogg.varsel(RV_YS_1)
        return aktivitetslogg
    }

    private fun validerTilkomneInntekter(aktivitetslogg: IAktivitetslogg) {
        if (tilkomneInntekter.isEmpty()) return
        if (tålerTilkommenInntekt()) aktivitetslogg.varsel(RV_SV_5) else aktivitetslogg.varsel(RV_IV_9)
    }

    private fun tålerTilkommenInntekt() = perioder.none { it is Søknadsperiode.Ferie || it is Søknadsperiode.Permisjon }

    private fun validerInntektskilder(aktivitetslogg: IAktivitetslogg, vilkårsgrunnlag: VilkårsgrunnlagElement?) {
        if (ikkeJobbetIDetSisteFraAnnetArbeidsforhold) aktivitetslogg.varsel(RV_SØ_44)
        if (!andreInntektskilder) return
        if (vilkårsgrunnlag == null) return aktivitetslogg.funksjonellFeil(RV_SØ_10)
        aktivitetslogg.varsel(RV_SØ_10)
    }

    internal fun utenlandskSykmelding(): Boolean {
        if (utenlandskSykmelding) return true
        return false
    }

    internal fun sendtTilGosys(): Boolean {
        if (sendTilGosys) return true
        return false
    }

    internal fun forUng(aktivitetslogg: IAktivitetslogg, alder: Alder) = alder.forUngForÅSøke(metadata.innsendt.toLocalDate()).also {
        if (it) aktivitetslogg.funksjonellFeil(RV_SØ_17)
    }
    private fun avskjæringsdato(): LocalDate =
        (opprinneligSendt ?: metadata.innsendt).toLocalDate().minusMonths(3).withDayOfMonth(1)


    internal fun lagVedtaksperiode(aktivitetslogg: IAktivitetslogg, person: Person, arbeidsgiver: Arbeidsgiver, subsumsjonslogg: Subsumsjonslogg): Vedtaksperiode {
        requireNotNull(sykdomstidslinje.periode()) { "ugyldig søknad: tidslinjen er tom" }
        return Vedtaksperiode(
            søknad = this,
            aktivitetslogg = aktivitetslogg,
            person = person,
            arbeidsgiver = arbeidsgiver,
            sykdomstidslinje = sykdomstidslinje,
            dokumentsporing = Dokumentsporing.søknad(metadata.meldingsreferanseId),
            sykmeldingsperiode = sykdomsperiode,
            subsumsjonslogg = subsumsjonslogg
        )
    }

    internal fun slettSykmeldingsperioderSomDekkes(arbeidsgiveren: Sykmeldingsperioder) {
        arbeidsgiveren.fjern(sykdomsperiode)
    }

    internal fun nyeInntekterUnderveis(aktivitetslogg: IAktivitetslogg): List<NyInntektUnderveis> {
        val tilkommetkilde = Kilde(metadata.meldingsreferanseId, Avsender.SYKMELDT, metadata.registrert)
        return if (!tålerTilkommenInntekt()) emptyList() else tilkomneInntekter.map { tilkommenInntekt ->
            tilkommenInntekt.beløpstidslinje(tilkommetkilde).also { tilkommenInntekt.loggMetadata(aktivitetslogg) }
        }
    }

    class Merknad(private val type: String) {
        private companion object {
            private val tilbakedateringer = setOf(
                "UGYLDIG_TILBAKEDATERING",
                "TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER",
                "UNDER_BEHANDLING",
                "DELVIS_GODKJENT"
            )
        }
        internal fun valider(aktivitetslogg: IAktivitetslogg) {
            if (type !in tilbakedateringer) return
            aktivitetslogg.varsel(RV_SØ_3)
        }
    }

    class TilkommenInntekt(
        fom: LocalDate,
        tom: LocalDate,
        private val orgnummer: String,
        private val råttBeløp: Int?,
    ) {
        private val periode = fom til tom
        private val antallVirkedager = (fom til tom.nesteDag).ukedager()
        private val smurtBeløp = if (råttBeløp == null) Inntekt.INGEN else (råttBeløp / antallVirkedager).daglig
        internal fun beløpstidslinje(kilde: Kilde) = NyInntektUnderveis(
            orgnummer = orgnummer,
            beløpstidslinje = Beløpstidslinje(periode.map {
                Beløpsdag(it, smurtBeløp, kilde)
            })
        )

        internal fun loggMetadata(aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Rått beløp: $råttBeløp og antall virkedager i $periode: $antallVirkedager har ført til anvendt daglig beløp: ${smurtBeløp.daglig}")
        }
    }

    class Søknadstype(private val type: String) {
        internal fun valider(
            aktivitetslogg: IAktivitetslogg,
            vilkårsgrunnlag: VilkårsgrunnlagElement?,
            orgnummer: String,
            periode: Periode?
        ) {
            if (this == Arbeidstaker) return
            if (this != Arbeidsledig) return aktivitetslogg.funksjonellFeil(`Støtter ikke søknadstypen`)
            if (vilkårsgrunnlag == null) return aktivitetslogg.funksjonellFeil(`Støtter ikke førstegangsbehandlinger for arbeidsledigsøknader`)
            if (vilkårsgrunnlag.refusjonsopplysninger(orgnummer).overlappendeEllerSenereRefusjonsopplysninger(periode).all { it.beløp == Inntekt.INGEN }) {
                return aktivitetslogg.info("Arbeidsledigsøknad lagt til grunn og vi har ikke registrert refusjon i søknadstidsrommet")
            }
            aktivitetslogg.varsel(`Arbeidsledigsøknad er lagt til grunn`)
        }
        override fun equals(other: Any?): Boolean {
            if (other !is Søknadstype) return false
            return this.type == other.type
        }
        override fun hashCode() = type.hashCode()
        companion object {
            val Arbeidstaker = Søknadstype("ARBEIDSTAKERE")
            val Arbeidsledig = Søknadstype("ARBEIDSLEDIG")
        }
    }

    sealed class Søknadsperiode(fom: LocalDate, tom: LocalDate) {
        val periode = Periode(fom, tom)

        internal companion object {
            fun sykdomsperiode(liste: List<Søknadsperiode>) =
                søknadsperiode(liste.filterIsInstance<Sykdom>())

            fun List<Søknadsperiode>.inneholderDagerEtter(sisteSykdomsdato: LocalDate) =
                any { it.periode.endInclusive > sisteSykdomsdato }

            fun List<Søknadsperiode>.subsumsjonsFormat(): List<Map<String, Serializable>> {
                return map {
                    mapOf(
                        "fom" to it.periode.start,
                        "tom" to it.periode.endInclusive,
                        "type" to when (it) {
                            is Arbeid -> "arbeid"
                            is Ferie -> "ferie"
                            is Papirsykmelding -> "papirsykmelding"
                            is Permisjon -> "permisjon"
                            is Sykdom -> "sykdom"
                            is Utlandsopphold -> "utlandsopphold"
                        }
                    )
                }
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

        internal open fun valider(søknad: Søknad, aktivitetslogg: IAktivitetslogg) {}

        internal fun valider(søknad: Søknad, aktivitetslogg: IAktivitetslogg, varselkode: Varselkode) {
            if (periode.utenfor(søknad.sykdomsperiode)) aktivitetslogg.varsel(varselkode)
        }

        class Sykdom(
            fom: LocalDate,
            tom: LocalDate,
            sykmeldingsgrad: Prosentdel,
            arbeidshelse: Prosentdel? = null
        ) : Søknadsperiode(fom, tom) {
            private val søknadsgrad = arbeidshelse?.not()
            private val sykdomsgrad = søknadsgrad ?: sykmeldingsgrad

            init {
                if (søknadsgrad != null && søknadsgrad > sykmeldingsgrad) throw IllegalStateException("Bruker har oppgitt at de har jobbet mindre enn sykmelding tilsier")
            }

            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.sykedager(periode.start, periode.endInclusive, avskjæringsdato, sykdomsgrad, kilde)
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.feriedager(periode.start, periode.endInclusive, kilde).subset(sykdomsperiode.oppdaterTom(periode))
        }

        class Papirsykmelding(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.problemdager(periode.start, periode.endInclusive, kilde, "Papirdager ikke støttet")

            override fun valider(søknad: Søknad, aktivitetslogg: IAktivitetslogg) =
                aktivitetslogg.funksjonellFeil(RV_SØ_22)
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.permisjonsdager(periode.start, periode.endInclusive, kilde)

            override fun valider(søknad: Søknad, aktivitetslogg: IAktivitetslogg) {
                valider(søknad, aktivitetslogg, RV_SØ_5)
            }
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun valider(søknad: Søknad, aktivitetslogg: IAktivitetslogg) =
                valider(søknad, aktivitetslogg, RV_SØ_7)

            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.arbeidsdager(periode.start, periode.endInclusive, kilde)
        }

        class Utlandsopphold(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(sykdomsperiode: Periode, avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.ukjent(periode.start, periode.endInclusive, kilde)

            override fun valider(søknad: Søknad, aktivitetslogg: IAktivitetslogg) {
                if (alleUtlandsdagerErFerie(søknad)) return
                aktivitetslogg.varsel(RV_SØ_8)
            }

            private fun alleUtlandsdagerErFerie(søknad:Søknad):Boolean {
                val feriePerioder = søknad.perioder.filterIsInstance<Ferie>()
                return this.periode.all { utlandsdag -> feriePerioder.any { ferie -> ferie.periode.contains(utlandsdag)} }
            }
        }
    }

    private class ForeldetSubsumsjonsgrunnlag(sykdomstidslinje: Sykdomstidslinje) {
        private val foreldedeDager = sykdomstidslinje.filterIsInstance<Dag.ForeldetSykedag>().map { it.dato }

        fun build() = foreldedeDager.grupperSammenhengendePerioder()
    }
}
