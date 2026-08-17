package no.nav.helse.opptjening.domain

import java.time.LocalDate

/**
 * Starter en prøving av opptjeningsvilkåret.
 *
 * Arbeidstakere må vi hente arbeidsforhold for, mens selvstendig næringsdrivende kan vurderes
 * med en gang. Det er den eneste vilkårsspesifikke delen av oppstarten — resten er [Vilkårsprøving].
 */
internal object Opptjeningsprøving {
    fun start(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        arbeidssituasjon: Arbeidssituasjon
    ) = Vilkårsprøving.start(
        vilkår = Vilkår.Opptjening,
        fødselsnummer = fødselsnummer,
        skjæringstidspunkt = skjæringstidspunkt,
        behov = Grunnlagsbehov.Arbeidsforhold,
        umiddelbartGrunnlag = when (arbeidssituasjon) {
            Arbeidssituasjon.Arbeidstaker -> null
            Arbeidssituasjon.SelvstendigNæringsdrivende -> Opptjeningsgrunnlag.SelvstendigNæringsdrivende
        }
    )
}
