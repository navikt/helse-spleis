package no.nav.helse.hendelser

import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.avklarSykepengegrunnlag
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.harInntektFor
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.harInntektI
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Companion.opptjeningsgrunnlag
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_3
import no.nav.helse.person.inntekt.AnsattPeriode
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.yearMonth
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class Vilkårsgrunnlag(
    meldingsreferanseId: UUID,
    private val vedtaksperiodeId: String,
    private val skjæringstidspunkt: LocalDate,
    orgnummer: String,
    private val medlemskapsvurdering: Medlemskapsvurdering,
    private val inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
    inntekterForOpptjeningsvurdering: InntekterForOpptjeningsvurdering,
    private val arbeidsforhold: List<Arbeidsforhold>
) : Hendelse {
    override val behandlingsporing =
        Behandlingsporing.Arbeidsgiver(
            organisasjonsnummer = orgnummer
        )
    override val metadata =
        LocalDateTime.now().let { nå ->
            HendelseMetadata(
                meldingsreferanseId = meldingsreferanseId,
                avsender = SYSTEM,
                innsendt = nå,
                registrert = nå,
                automatiskBehandling = true
            )
        }

    private var grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata? = null

    private val opptjeningsgrunnlag = arbeidsforhold.opptjeningsgrunnlag()
    private val harInntektMånedenFørSkjæringstidspunkt = inntekterForOpptjeningsvurdering.harInntektI(YearMonth.from(skjæringstidspunkt).minusMonths(1))

    internal fun erRelevant(
        aktivitetslogg: IAktivitetslogg,
        other: UUID,
        skjæringstidspunktVedtaksperiode: LocalDate
    ): Boolean {
        if (other.toString() != vedtaksperiodeId) return false
        if (skjæringstidspunktVedtaksperiode == skjæringstidspunkt) return true
        aktivitetslogg.info("Vilkårsgrunnlag var relevant for Vedtaksperiode, men skjæringstidspunktene var ulikte: [$skjæringstidspunkt, $skjæringstidspunktVedtaksperiode]")
        return false
    }

    private fun opptjening(): Opptjening =
        Opptjening.nyOpptjening(
            grunnlag =
                opptjeningsgrunnlag.map { (orgnummer, ansattPerioder) ->
                    Opptjening.ArbeidsgiverOpptjeningsgrunnlag(orgnummer, ansattPerioder.map { it.tilDomeneobjekt() })
                },
            skjæringstidspunkt = skjæringstidspunkt
        )

    internal fun avklarSykepengegrunnlag(
        person: Person,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonslogg: Subsumsjonslogg
    ): Inntektsgrunnlag {
        val rapporterteArbeidsforhold =
            opptjeningsgrunnlag.mapValues { (_, ansattPerioder) ->
                SkattSykepengegrunnlag(
                    hendelseId = metadata.meldingsreferanseId,
                    dato = skjæringstidspunkt,
                    inntektsopplysninger = emptyList(),
                    ansattPerioder = ansattPerioder.map { it.somAnsattPeriode() }
                )
            }
        return inntektsvurderingForSykepengegrunnlag.inntekter.avklarSykepengegrunnlag(aktivitetslogg, person, rapporterteArbeidsforhold, skjæringstidspunkt, metadata.meldingsreferanseId, subsumsjonslogg)
    }

    internal fun valider(
        aktivitetslogg: IAktivitetslogg,
        inntektsgrunnlag: Inntektsgrunnlag,
        subsumsjonslogg: Subsumsjonslogg
    ): IAktivitetslogg {
        val sykepengegrunnlagOk = inntektsgrunnlag.valider(aktivitetslogg)
        arbeidsforhold.forEach { it.validerFrilans(aktivitetslogg, skjæringstidspunkt, arbeidsforhold, inntektsvurderingForSykepengegrunnlag) }
        val opptjening = opptjening()
        subsumsjonslogg.logg(opptjening.subsumsjon)

        if (!harInntektMånedenFørSkjæringstidspunkt) {
            aktivitetslogg.varsel(Varselkode.RV_OV_3)
        } else if (!inntektsvurderingForSykepengegrunnlag.inntekter.harInntektI(YearMonth.from(skjæringstidspunkt.minusMonths(1)))) {
            aktivitetslogg.info("Har inntekt måneden før skjæringstidspunkt med inntekter for opptjeningsvurdering, men ikke med inntekter for sykepengegrunnlag")
        }

        val opptjeningvurderingOk = opptjening.validerOpptjeningsdager(aktivitetslogg)
        val medlemskapsvurderingOk = medlemskapsvurdering.valider(aktivitetslogg)
        grunnlagsdata =
            VilkårsgrunnlagHistorikk.Grunnlagsdata(
                skjæringstidspunkt = skjæringstidspunkt,
                inntektsgrunnlag = inntektsgrunnlag,
                opptjening = opptjening,
                medlemskapstatus = medlemskapsvurdering.medlemskapstatus,
                vurdertOk = sykepengegrunnlagOk && opptjeningvurderingOk && medlemskapsvurderingOk,
                meldingsreferanseId = metadata.meldingsreferanseId,
                vilkårsgrunnlagId = UUID.randomUUID()
            )
        return aktivitetslogg
    }

    internal fun grunnlagsdata() = requireNotNull(grunnlagsdata) { "Må kalle valider() først" }

    class Arbeidsforhold(
        private val orgnummer: String,
        private val ansettelseperiode: Periode,
        private val type: Arbeidsforholdtype
    ) {
        enum class Arbeidsforholdtype {
            FORENKLET_OPPGJØRSORDNING,
            FRILANSER,
            MARITIMT,
            ORDINÆRT
        }

        constructor(orgnummer: String, ansattFom: LocalDate, ansattTom: LocalDate? = null, type: Arbeidsforholdtype) : this(orgnummer, ansattFom til (ansattTom ?: LocalDate.MAX), type)

        init {
            check(orgnummer.isNotBlank())
        }

        fun validerFrilans(
            aktivitetslogg: IAktivitetslogg,
            skjæringstidspunkt: LocalDate,
            andre: List<Arbeidsforhold>,
            inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag
        ) {
            if (type != Arbeidsforholdtype.FRILANSER) return
            harFrilansinntekterDeSiste3Månedene(aktivitetslogg, skjæringstidspunkt, inntektsvurderingForSykepengegrunnlag)
            sjekkFrilansArbeidsforholdMotAndreArbeidsforhold(aktivitetslogg, skjæringstidspunkt, andre)
        }

        private fun harFrilansinntekterDeSiste3Månedene(
            aktivitetslogg: IAktivitetslogg,
            skjæringstidspunkt: LocalDate,
            inntektForSykepengegrunnlag: InntektForSykepengegrunnlag
        ) {
            val finnerFrilansinntektDeSiste3Månedene =
                (1..3).any { antallMånederFør ->
                    val måned = skjæringstidspunkt.yearMonth.minusMonths(antallMånederFør.toLong())
                    val månedenSomPeriode = måned.atDay(1) til måned.atEndOfMonth()
                    val ansattIMåneden = ansettelseperiode.overlapperMed(månedenSomPeriode)
                    ansattIMåneden && inntektForSykepengegrunnlag.inntekter.harInntektFor(orgnummer, måned)
                }
            if (finnerFrilansinntektDeSiste3Månedene) aktivitetslogg.funksjonellFeil(RV_IV_3)
        }

        private fun sjekkFrilansArbeidsforholdMotAndreArbeidsforhold(
            aktivitetslogg: IAktivitetslogg,
            skjæringstidspunkt: LocalDate,
            andre: List<Arbeidsforhold>
        ) {
            if (skjæringstidspunkt !in ansettelseperiode) return
            aktivitetslogg.info("Vedkommende har et aktivt frilansoppdrag på skjæringstidspunktet")

            if (andre.count { it.orgnummer == this.orgnummer && it.ansettelseperiode.overlapperMed(this.ansettelseperiode) } > 1) {
                aktivitetslogg.info("Vedkommende har andre overlappende arbeidsforhold i samme virksomhet hvor vedkommende har frilansoppdrag")
            }
        }

        internal fun tilDomeneobjekt() =
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold(
                ansattFom = ansettelseperiode.start,
                ansattTom = ansettelseperiode.endInclusive.takeUnless { it == LocalDate.MAX },
                deaktivert = false
            )

        internal fun somAnsattPeriode() = AnsattPeriode(ansattFom = ansettelseperiode.start, ansattTom = ansettelseperiode.endInclusive.takeUnless { it == LocalDate.MAX })

        private fun kanBrukes() = type != Arbeidsforholdtype.FRILANSER // filtrerer ut frilans-arbeidsforhold enn så lenge

        internal companion object {
            internal fun Iterable<Arbeidsforhold>.opptjeningsgrunnlag(): Map<String, List<Arbeidsforhold>> =
                this
                    .filter { it.kanBrukes() }
                    .groupBy { it.orgnummer }
        }
    }
}
