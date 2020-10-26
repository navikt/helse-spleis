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
    meldingsreferanseId: UUID,
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val inntektsvurdering: Inntektsvurdering,
    private val opptjeningvurdering: Opptjeningvurdering,
    private val medlemskapsvurdering: Medlemskapsvurdering,
    private val dagpenger: Dagpenger,
    private val arbeidsavklaringspenger: Arbeidsavklaringspenger
) : ArbeidstakerHendelse(meldingsreferanseId) {
    private var grunnlagsdata: Grunnlagsdata? = null

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    internal fun valider(
        beregnetInntekt: Inntekt,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype
    ): Aktivitetslogg {
        inntektsvurdering.valider(aktivitetslogg, beregnetInntekt, periodetype)
        opptjeningvurdering.valider(aktivitetslogg, orgnummer, skjæringstidspunkt)
        medlemskapsvurdering.valider(aktivitetslogg, periodetype)
        grunnlagsdata = Grunnlagsdata(
            beregnetÅrsinntektFraInntektskomponenten = inntektsvurdering.sammenligningsgrunnlag(),
            avviksprosent = inntektsvurdering.avviksprosent(),
            antallOpptjeningsdagerErMinst = opptjeningvurdering.opptjeningsdager(orgnummer),
            harOpptjening = opptjeningvurdering.harOpptjening(orgnummer),
            medlemskapstatus = medlemskapsvurdering.medlemskapstatus
        )
        dagpenger.valider(aktivitetslogg, skjæringstidspunkt)
        arbeidsavklaringspenger.valider(aktivitetslogg, skjæringstidspunkt)
        return aktivitetslogg
    }

    internal fun lagreInntekter(person: Person, skjæringstidspunkt: LocalDate) {
        inntektsvurdering.lagreInntekter(person, skjæringstidspunkt, this)
    }

    internal fun grunnlagsdata() = requireNotNull(grunnlagsdata) { "Må kalle valider() først" }

    internal class Grunnlagsdata(
        internal val beregnetÅrsinntektFraInntektskomponenten: Inntekt,
        internal val avviksprosent: Prosent?,
        internal val antallOpptjeningsdagerErMinst: Int,
        internal val harOpptjening: Boolean,
        internal val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
    )
}
