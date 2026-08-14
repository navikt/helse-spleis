package no.nav.helse.opptjening.domain

import java.time.LocalDate

/**
 * Starter en prøving av medlemskapsvilkåret.
 *
 * Medlemskap må alltid slås opp utenfor, så prøvingen venter alltid — i motsetning til opptjening,
 * som for selvstendig næringsdrivende kan avgjøres uten innhenting.
 */
internal object Medlemskapsprøving {
    fun start(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate
    ) = Vilkårsprøving.start(
        vilkår = Vilkår.Medlemskap,
        fødselsnummer = fødselsnummer,
        skjæringstidspunkt = skjæringstidspunkt,
        behov = Grunnlagsbehov.Medlemskap,
        umiddelbartGrunnlag = null
    )
}
