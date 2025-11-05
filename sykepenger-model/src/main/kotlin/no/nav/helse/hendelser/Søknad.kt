package no.nav.helse.hendelser

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.Toggle
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 22-13 ledd 3`
import no.nav.helse.etterlevelse.`§ 8-9 ledd 1`
import no.nav.helse.hendelser.Avsender.SYKMELDT
import no.nav.helse.hendelser.Periode.Companion.delvisOverlappMed
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.Søknad.PensjonsgivendeInntekt.Companion.harFlereTyperPensjonsgivendeInntekt
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Companion.inneholderDagerEtter
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Companion.subsumsjonsFormat
import no.nav.helse.person.Behandlinger
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.EventBus
import no.nav.helse.person.Person
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.Yrkesaktivitet
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Arbeidsledigsøknad er lagt til grunn`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Selvstendigsøknad med flere typer pensjonsgivende inntekter`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Støtter ikke førstegangsbehandlinger for arbeidsledigsøknader`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Støtter ikke søknadstypen`
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_17
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_22
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_29
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_30
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_YS_1
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.tournament.Dagturnering
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel

class Søknad(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
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
    private val arbeidssituasjon: Arbeidssituasjon,
    registrert: LocalDateTime,
    private val inntekterFraNyeArbeidsforhold: Boolean,
    private val pensjonsgivendeInntekter: List<PensjonsgivendeInntekt>?,
    private val fraværFørSykmelding: Boolean?,
    private val harOppgittNyIArbeidslivet: Boolean?,
    private val harOppgittVarigEndring: Boolean?,
    private val harOppgittAvvikling: Boolean?,
    private val harOppgittOpprettholdtInntekt: Boolean?,
    private val harOppgittOppholdIUtlandet: Boolean?
) : Hendelse {

    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SYKMELDT,
        innsendt = sendtTilNAVEllerArbeidsgiver,
        registrert = registrert,
        automatiskBehandling = false
    )

    private val kilde: Hendelseskilde = Hendelseskilde(this::class, metadata.meldingsreferanseId, sykmeldingSkrevet)
    private val sykdomsperiode: Periode
    var sykdomstidslinje: Sykdomstidslinje
        private set
    var delvisOverlappende: Boolean = false
        private set

    init {
        if (perioder.isEmpty()) error("Søknad må inneholde perioder")
        sykdomsperiode = Søknadsperiode.sykdomsperiode(perioder) ?: error("Søknad inneholder ikke sykdomsperioder")
        if (perioder.inneholderDagerEtter(sykdomsperiode.endInclusive)) error("Søknad inneholder dager utenfor søknadsperioden")

        sykdomstidslinje = perioder
            .map { it.sykdomstidslinje(avskjæringsdato(), kilde) }
            .merge(Dagturnering.SØKNAD::beste)
            .subset(sykdomsperiode)
    }

    fun erRelevant(other: Periode): Boolean {
        if (other.delvisOverlappMed(sykdomsperiode)) delvisOverlappende = true
        return other.overlapperMed(sykdomsperiode)
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, vilkårsgrunnlag: VilkårsgrunnlagElement?, refusjonstidslinje: Beløpstidslinje, subsumsjonslogg: Subsumsjonslogg, skjæringstidspunkt: LocalDate): IAktivitetslogg {
        valider(aktivitetslogg, subsumsjonslogg)
        validerInntektskilder(aktivitetslogg, skjæringstidspunkt)

        when (arbeidssituasjon) {
            Arbeidssituasjon.ARBEIDSTAKER,
            Arbeidssituasjon.FRILANSER -> {
                // ingen spesiell validering
            }

            Arbeidssituasjon.ARBEIDSLEDIG -> validerArbeidsledig(aktivitetslogg, vilkårsgrunnlag, sykdomstidslinje.periode(), refusjonstidslinje)
            Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE,
            Arbeidssituasjon.BARNEPASSER -> validerSelvstendig(aktivitetslogg, skjæringstidspunkt)

            Arbeidssituasjon.JORDBRUKER -> {
                if (Toggle.Jordbruker.enabled) validerSelvstendig(aktivitetslogg, skjæringstidspunkt)
                else {
                    aktivitetslogg.info("Har ikke støtte for søknadstypen $arbeidssituasjon")
                    aktivitetslogg.funksjonellFeil(`Støtter ikke søknadstypen`)
                }
            }

            Arbeidssituasjon.FISKER,
            Arbeidssituasjon.ANNET -> {
                aktivitetslogg.info("Har ikke støtte for søknadstypen $arbeidssituasjon")
                aktivitetslogg.funksjonellFeil(`Støtter ikke søknadstypen`)
            }
        }

        return aktivitetslogg
    }

    private fun validerArbeidsledig(aktivitetslogg: IAktivitetslogg, vilkårsgrunnlag: VilkårsgrunnlagElement?, periode: Periode?, refusjonstidslinje: Beløpstidslinje) {
        if (vilkårsgrunnlag == null) return aktivitetslogg.funksjonellFeil(`Støtter ikke førstegangsbehandlinger for arbeidsledigsøknader`)

        if (periode != null && refusjonstidslinje.kunIngenRefusjon()) {
            return aktivitetslogg.info("Arbeidsledigsøknad lagt til grunn og vi har ikke registrert refusjon i søknadstidsrommet")
        }
        aktivitetslogg.varsel(`Arbeidsledigsøknad er lagt til grunn`)
    }

    private fun validerSelvstendig(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate) {
        val næringsinntekter = pensjonsgivendeInntekter?.filter { it.erFerdigLignet }
        if (næringsinntekter == null || næringsinntekter.size < 3) aktivitetslogg.funksjonellFeil(Varselkode.RV_IV_12)

        if (harOppgittAvvikling == true) aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_47)
        if (harOppgittNyIArbeidslivet == true) aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_48)
        if (harOppgittVarigEndring == true) aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_49)

        if (harOppgittOpprettholdtInntekt == true) aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_51)
        if (harOppgittOppholdIUtlandet == true) aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_52)

        if (pensjonsgivendeInntekter?.harFlereTyperPensjonsgivendeInntekt() == true) aktivitetslogg.funksjonellFeil(`Selvstendigsøknad med flere typer pensjonsgivende inntekter`)

        vurderOpptjeningForSelvstendig(aktivitetslogg, skjæringstidspunkt)
    }

    private fun vurderOpptjeningForSelvstendig(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate) {
        if (skjæringstidspunkt !in sykdomsperiode) return // Da er det ikke aktuelt å sjekke
        when (fraværFørSykmelding) {
            true -> aktivitetslogg.funksjonellFeil(Varselkode.RV_SØ_46)
            null -> aktivitetslogg.funksjonellFeil(Varselkode.RV_OV_4)
            false -> {} // Da er opptjening OK
        }
    }

    private fun valider(aktivitetslogg: IAktivitetslogg, subsumsjonslogg: Subsumsjonslogg): IAktivitetslogg {
        val utlandsopphold = perioder.filterIsInstance<Søknadsperiode.Utlandsopphold>().map { it.periode }
        subsumsjonslogg.logg(`§ 8-9 ledd 1`(false, utlandsopphold, this.perioder.subsumsjonsFormat()))
        perioder.forEach { it.valider(this, aktivitetslogg) }
        if (permittert) aktivitetslogg.varsel(RV_SØ_1)
        validerInntekterFraNyeArbeidsforhold(aktivitetslogg)
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

    private fun validerInntekterFraNyeArbeidsforhold(aktivitetslogg: IAktivitetslogg) {
        if (!inntekterFraNyeArbeidsforhold) return
        aktivitetslogg.varsel(Varselkode.TilkommenInntekt.`Opplyst i søknaden om inntekter hen har hatt fra andre arbeidsgivere`)
    }

    private fun validerInntektskilder(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate) {
        if (ikkeJobbetIDetSisteFraAnnetArbeidsforhold) aktivitetslogg.varsel(Varselkode.TilkommenInntekt.`Opplyst i søknaden om at hen er arbeidstaker hos annen arbeidsgiver, men ikke jobbet der de siste 14 dagene før hen ble sykmeldt`)
        if (!andreInntektskilder) return
        if (skjæringstidspunkt in sykdomsperiode) return aktivitetslogg.funksjonellFeil(Varselkode.TilkommenInntekt.`Opplyst i søknaden om at hen har andre inntekskilder`)
        aktivitetslogg.varsel(Varselkode.TilkommenInntekt.`Opplyst i søknaden om at hen har andre inntekskilder`)
    }

    internal fun forUng(aktivitetslogg: IAktivitetslogg, alder: Alder) = alder.forUngForÅSøke(metadata.innsendt.toLocalDate()).also {
        if (it) aktivitetslogg.funksjonellFeil(RV_SØ_17)
    }

    private fun avskjæringsdato(): LocalDate =
        (opprinneligSendt ?: metadata.innsendt).toLocalDate().minusMonths(3).withDayOfMonth(1)

    internal fun lagVedtaksperiode(eventBus: EventBus, person: Person, yrkesaktivitet: Yrkesaktivitet, regelverkslogg: Regelverkslogg): Vedtaksperiode {
        requireNotNull(sykdomstidslinje.periode()) { "ugyldig søknad: tidslinjen er tom" }
        val faktaavklartInntekt = when (behandlingsporing) {
            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            is Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
            Behandlingsporing.Yrkesaktivitet.Frilans -> null

            Behandlingsporing.Yrkesaktivitet.Selvstendig -> {
                val anvendtGrunnbeløp = `1G`.beløp(sykdomsperiode.start)
                val avklartePensjonsgivendeInntekter = pensjonsgivendeInntekter?.map {
                    SelvstendigFaktaavklartInntekt.PensjonsgivendeInntekt(
                        årstall = it.inntektsår,
                        beløp = it.næringsinntekt
                    )
                } ?: emptyList()
                SelvstendigFaktaavklartInntekt(
                    id = UUID.randomUUID(),
                    inntektsdata = Inntektsdata(
                        hendelseId = metadata.meldingsreferanseId,
                        dato = sykdomsperiode.start,
                        beløp = SelvstendigFaktaavklartInntekt.beregnInntektsgrunnlag(avklartePensjonsgivendeInntekter, anvendtGrunnbeløp),
                        tidsstempel = LocalDateTime.now()
                    ),
                    pensjonsgivendeInntekter = avklartePensjonsgivendeInntekter,
                    anvendtGrunnbeløp = anvendtGrunnbeløp
                )
            }
        }

        val arbeidssituasjon = when (arbeidssituasjon) {
            Arbeidssituasjon.ARBEIDSTAKER -> Behandlinger.Behandling.Endring.Arbeidssituasjon.ARBEIDSTAKER
            Arbeidssituasjon.ARBEIDSLEDIG -> Behandlinger.Behandling.Endring.Arbeidssituasjon.ARBEIDSLEDIG
            Arbeidssituasjon.FRILANSER -> Behandlinger.Behandling.Endring.Arbeidssituasjon.FRILANSER
            Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE -> Behandlinger.Behandling.Endring.Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE
            Arbeidssituasjon.BARNEPASSER -> Behandlinger.Behandling.Endring.Arbeidssituasjon.BARNEPASSER
            Arbeidssituasjon.JORDBRUKER -> Behandlinger.Behandling.Endring.Arbeidssituasjon.JORDBRUKER
            Arbeidssituasjon.FISKER -> Behandlinger.Behandling.Endring.Arbeidssituasjon.FISKER
            Arbeidssituasjon.ANNET -> Behandlinger.Behandling.Endring.Arbeidssituasjon.ANNET
        }

        return Vedtaksperiode(
            eventBus = eventBus,
            egenmeldingsperioder = egenmeldinger,
            metadata = metadata,
            person = person,
            yrkesaktivitet = yrkesaktivitet,
            sykdomstidslinje = sykdomstidslinje,
            arbeidssituasjon = arbeidssituasjon,
            faktaavklartInntekt = faktaavklartInntekt,
            dokumentsporing = Dokumentsporing.søknad(metadata.meldingsreferanseId),
            sykmeldingsperiode = sykdomsperiode,
            regelverkslogg = regelverkslogg
        )
    }

    internal fun slettSykmeldingsperioderSomDekkes(arbeidsgiveren: Sykmeldingsperioder) {
        arbeidsgiveren.fjern(sykdomsperiode)
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
                    .periode()
        }

        internal abstract fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde): Sykdomstidslinje

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

            override fun valider(søknad: Søknad, aktivitetslogg: IAktivitetslogg) =
                aktivitetslogg.funksjonellFeil(RV_SØ_22)
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.permisjonsdager(periode.start, periode.endInclusive, kilde)
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.arbeidsdager(periode.start, periode.endInclusive, kilde)
        }

        class Utlandsopphold(fom: LocalDate, tom: LocalDate) : Søknadsperiode(fom, tom) {
            override fun sykdomstidslinje(avskjæringsdato: LocalDate, kilde: Hendelseskilde) =
                Sykdomstidslinje.ukjent(periode.start, periode.endInclusive, kilde)

            override fun valider(søknad: Søknad, aktivitetslogg: IAktivitetslogg) {
                if (alleUtlandsdagerErFerie(søknad)) return
                aktivitetslogg.varsel(RV_SØ_8)
            }

            private fun alleUtlandsdagerErFerie(søknad: Søknad): Boolean {
                val feriePerioder = søknad.perioder.filterIsInstance<Ferie>()
                return this.periode.all { utlandsdag -> feriePerioder.any { ferie -> ferie.periode.contains(utlandsdag) } }
            }
        }
    }

    private class ForeldetSubsumsjonsgrunnlag(sykdomstidslinje: Sykdomstidslinje) {
        private val foreldedeDager = sykdomstidslinje.filterIsInstance<Dag.ForeldetSykedag>().map { it.dato }

        fun build() = foreldedeDager.grupperSammenhengendePerioder()
    }

    data class PensjonsgivendeInntekt(
        val inntektsår: Year,
        val næringsinntekt: Inntekt,
        val lønnsinntekt: Inntekt,
        val lønnsinntektBarePensjonsdel: Inntekt,
        val næringsinntektFraFiskeFangstEllerFamiliebarnehage: Inntekt,
        val erFerdigLignet: Boolean
    ) {
        companion object {
            fun List<PensjonsgivendeInntekt>.harFlereTyperPensjonsgivendeInntekt() =
                any { pensjonsgivendeInntekt ->
                    listOf(pensjonsgivendeInntekt.næringsinntekt, pensjonsgivendeInntekt.næringsinntektFraFiskeFangstEllerFamiliebarnehage, pensjonsgivendeInntekt.lønnsinntekt, pensjonsgivendeInntekt.lønnsinntektBarePensjonsdel)
                        .count { it != Inntekt.INGEN } > 1
                }
        }
    }

    enum class Arbeidssituasjon {
        ARBEIDSTAKER,
        ARBEIDSLEDIG,
        FRILANSER,
        SELVSTENDIG_NÆRINGSDRIVENDE,
        BARNEPASSER,
        JORDBRUKER,
        FISKER,
        ANNET
    }
}
