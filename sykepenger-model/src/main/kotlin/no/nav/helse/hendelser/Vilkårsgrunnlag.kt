package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Person
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import java.time.LocalDate
import java.util.*

class Vilkårsgrunnlag(
    private val meldingsreferanseId: UUID,
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
    fun meldingsreferanseId() = meldingsreferanseId

    internal fun valider(
        beregnetInntekt: Inntekt,
        førsteFraværsdag: LocalDate,
        periodetype: Periodetype
    ): Aktivitetslogg {
        inntektsvurdering.valider(aktivitetslogg, beregnetInntekt, periodetype)
        opptjeningvurdering.valider(aktivitetslogg, orgnummer, førsteFraværsdag)
        medlemskapsvurdering.valider(aktivitetslogg, periodetype)
        if (erEgenAnsatt) error("Støtter ikke behandling av NAV-ansatte eller familiemedlemmer av NAV-ansatte")
        else info("er ikke egen ansatt")
        grunnlagsdata = Grunnlagsdata(
            erEgenAnsatt = erEgenAnsatt,
            beregnetÅrsinntektFraInntektskomponenten = inntektsvurdering.sammenligningsgrunnlag(),
            avviksprosent = inntektsvurdering.avviksprosent(),
            antallOpptjeningsdagerErMinst = opptjeningvurdering.opptjeningsdager(orgnummer),
            harOpptjening = opptjeningvurdering.harOpptjening(orgnummer),
            medlemskapstatus = medlemskapsvurdering.medlemskapstatus
        )
        dagpenger.valider(aktivitetslogg, førsteFraværsdag)
        arbeidsavklaringspenger.valider(aktivitetslogg, førsteFraværsdag)
        return aktivitetslogg
    }

    internal fun lagreInntekter(person: Person, førsteFraværsdag: LocalDate) {
        inntektsvurdering.lagreInntekter(person, førsteFraværsdag, this)
    }

    internal fun grunnlagsdata() = requireNotNull(grunnlagsdata) { "Må kalle valider() først" }

    internal class Grunnlagsdata(
        internal val erEgenAnsatt: Boolean,
        internal val beregnetÅrsinntektFraInntektskomponenten: Inntekt,
        internal val avviksprosent: Prosent?,
        internal val antallOpptjeningsdagerErMinst: Int,
        internal val harOpptjening: Boolean,
        internal val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
    )
}
