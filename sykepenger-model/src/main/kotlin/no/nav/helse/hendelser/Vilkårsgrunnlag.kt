package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import java.math.BigDecimal
import java.time.LocalDate

@Deprecated("inntektsvurdering, opptjeningvurdering, medlemskapsvurdering og erEgenAnsatt sendes som tre parametre til modellen")
class Vilkårsgrunnlag(
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val inntektsvurdering: Inntektsvurdering,
    private val opptjeningvurdering: Opptjeningvurdering,
    private val erEgenAnsatt: Boolean,
    private val medlemskapsvurdering: Medlemskapsvurdering,
    private val dagpenger: Dagpenger,
    private val arbeidsavklaringspenger: Arbeidsavklaringspenger
) : ArbeidstakerHendelse() {
    private var grunnlagsdata: Grunnlagsdata? = null

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    internal fun valider(beregnetInntekt: BigDecimal, førsteFraværsdag: LocalDate): Aktivitetslogg {
        inntektsvurdering.valider(aktivitetslogg, beregnetInntekt)
        opptjeningvurdering.valider(aktivitetslogg, orgnummer, førsteFraværsdag)
        medlemskapsvurdering.valider(aktivitetslogg)
        if (erEgenAnsatt) error("Støtter ikke behandling av NAV-ansatte eller familiemedlemmer av NAV-ansatte")
        else info("er ikke egen ansatt")
        grunnlagsdata = Grunnlagsdata(
            erEgenAnsatt = erEgenAnsatt,
            beregnetÅrsinntektFraInntektskomponenten = inntektsvurdering.sammenligningsgrunnlag(),
            avviksprosent = inntektsvurdering.avviksprosent(),
            antallOpptjeningsdagerErMinst = opptjeningvurdering.opptjeningsdager(orgnummer),
            harOpptjening = opptjeningvurdering.harOpptjening(orgnummer)
        )
        dagpenger.valider(aktivitetslogg, førsteFraværsdag)
        arbeidsavklaringspenger.valider(aktivitetslogg, førsteFraværsdag)
        return aktivitetslogg
    }

    internal fun grunnlagsdata() = requireNotNull(grunnlagsdata) { "Må kalle valider() først" }

    internal class Grunnlagsdata(
        internal val erEgenAnsatt: Boolean,
        internal val beregnetÅrsinntektFraInntektskomponenten: Double,
        internal val avviksprosent: Double,
        internal val antallOpptjeningsdagerErMinst: Int,
        internal val harOpptjening: Boolean
    )
}
