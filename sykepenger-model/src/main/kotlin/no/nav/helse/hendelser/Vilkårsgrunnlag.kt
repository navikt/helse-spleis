package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Companion.opptjeningsgrunnlag
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.inntekt.AnsattPeriode
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag

class Vilkårsgrunnlag(
    meldingsreferanseId: UUID,
    private val vedtaksperiodeId: String,
    private val skjæringstidspunkt: LocalDate,
    aktørId: String,
    personidentifikator: Personidentifikator,
    orgnummer: String,
    private val medlemskapsvurdering: Medlemskapsvurdering,
    private val inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag,
    inntekterForOpptjeningsvurdering: InntekterForOpptjeningsvurdering,
    private val arbeidsforhold: List<Arbeidsforhold>
) : PersonHendelse() {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        fødselsnummer = personidentifikator.toString(),
        aktørId = aktørId,
        organisasjonsnummer = orgnummer
    )
    override val metadata = LocalDateTime.now().let { nå ->
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

    internal fun erRelevant(aktivitetslogg: IAktivitetslogg, other: UUID, skjæringstidspunktVedtaksperiode: LocalDate): Boolean {
        if (other.toString() != vedtaksperiodeId) return false
        if (skjæringstidspunktVedtaksperiode == skjæringstidspunkt) return true
        aktivitetslogg.info("Vilkårsgrunnlag var relevant for Vedtaksperiode, men skjæringstidspunktene var ulikte: [$skjæringstidspunkt, $skjæringstidspunktVedtaksperiode]")
        return false
    }

    private fun opptjening(): Opptjening {
        return Opptjening.nyOpptjening(
            grunnlag = opptjeningsgrunnlag.map { (orgnummer, ansattPerioder) ->
                Opptjening.ArbeidsgiverOpptjeningsgrunnlag(orgnummer, ansattPerioder.map { it.tilDomeneobjekt() })
            },
            skjæringstidspunkt = skjæringstidspunkt,
            harInntektMånedenFørSkjæringstidspunkt = harInntektMånedenFørSkjæringstidspunkt
        )
    }

    internal fun avklarSykepengegrunnlag(person: Person, aktivitetslogg: IAktivitetslogg, subsumsjonslogg: Subsumsjonslogg): Inntektsgrunnlag {
        val rapporterteArbeidsforhold = opptjeningsgrunnlag.mapValues { (_, ansattPerioder) ->
            SkattSykepengegrunnlag(
                hendelseId = metadata.meldingsreferanseId,
                dato = skjæringstidspunkt,
                inntektsopplysninger = emptyList(),
                ansattPerioder = ansattPerioder.map { it.somAnsattPeriode() }
            )
        }
        return inntektsvurderingForSykepengegrunnlag.avklarSykepengegrunnlag(aktivitetslogg, person, rapporterteArbeidsforhold, skjæringstidspunkt, metadata.meldingsreferanseId, subsumsjonslogg)
    }

    internal fun valider(aktivitetslogg: IAktivitetslogg, inntektsgrunnlag: Inntektsgrunnlag, subsumsjonslogg: Subsumsjonslogg): IAktivitetslogg {
        val sykepengegrunnlagOk = inntektsgrunnlag.valider(aktivitetslogg)
        inntektsvurderingForSykepengegrunnlag.valider(aktivitetslogg)
        arbeidsforhold.forEach { it.loggFrilans(aktivitetslogg, skjæringstidspunkt, arbeidsforhold) }
        val opptjening = opptjening()
        subsumsjonslogg.logg(opptjening.subsumsjon)
        opptjening.validerInntektMånedenFørSkjæringstidspunkt(aktivitetslogg).also {
            if (harInntektMånedenFørSkjæringstidspunkt && !inntektsvurderingForSykepengegrunnlag.harInntektI(YearMonth.from(skjæringstidspunkt.minusMonths(1)))) {
                // Varsel spart
                aktivitetslogg.info("Har inntekt måneden før skjæringstidspunkt med inntekter for opptjeningsvurdering, men ikke med inntekter for sykepengegrunnlag")
            }
        }
        val opptjeningvurderingOk = opptjening.validerOpptjeningsdager(aktivitetslogg)
        val medlemskapsvurderingOk = medlemskapsvurdering.valider(aktivitetslogg)
        grunnlagsdata = VilkårsgrunnlagHistorikk.Grunnlagsdata(
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

        fun loggFrilans(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate, andre: List<Arbeidsforhold>) {
            if (type != Arbeidsforholdtype.FRILANSER) return
            if (skjæringstidspunkt !in ansettelseperiode) return
            aktivitetslogg.info("Vedkommende har et aktivt frilansoppdrag på skjæringstidspunktet")

            if (andre.count { it.orgnummer == this.orgnummer && it.ansettelseperiode.overlapperMed(this.ansettelseperiode) } > 1) {
                aktivitetslogg.info("Vedkommende har andre overlappende arbeidsforhold i samme virksomhet hvor vedkommende har frilansoppdrag")
            }
        }

        internal fun tilDomeneobjekt() = Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold(
            ansattFom = ansettelseperiode.start,
            ansattTom = ansettelseperiode.endInclusive.takeUnless { it == LocalDate.MAX },
            deaktivert = false
        )

        internal fun somAnsattPeriode() =
            AnsattPeriode(ansattFom = ansettelseperiode.start, ansattTom = ansettelseperiode.endInclusive.takeUnless { it == LocalDate.MAX })

        private fun kanBrukes() = type != Arbeidsforholdtype.FRILANSER // filtrerer ut frilans-arbeidsforhold enn så lenge

        internal companion object {
            internal fun Iterable<Arbeidsforhold>.opptjeningsgrunnlag(): Map<String, List<Arbeidsforhold>> {
                return this
                    .filter { it.kanBrukes() }
                    .groupBy { it.orgnummer }
            }
        }
    }
}
