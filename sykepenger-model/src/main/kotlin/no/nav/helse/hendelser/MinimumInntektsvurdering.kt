package no.nav.helse.hendelser

import no.nav.helse.Fødselsnummer
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import java.time.LocalDate

internal fun validerMinimumInntekt(
    aktivitetslogg: IAktivitetslogg,
    fødselsnummer: Fødselsnummer,
    skjæringstidspunkt: LocalDate,
    grunnlagForSykepengegrunnlag: Sykepengegrunnlag,
    subsumsjonObserver: SubsumsjonObserver
): Boolean {
    val alder = fødselsnummer.alder()
    val minimumInntekt = alder.minimumInntekt(skjæringstidspunkt)
    val oppfylt = grunnlagForSykepengegrunnlag.oppfyllerKravTilMinimumInntekt(minimumInntekt)
    val grunnlag = grunnlagForSykepengegrunnlag.grunnlagForSykepengegrunnlag
    val alderPåSkjæringstidspunkt = alder.alderPåDato(skjæringstidspunkt)

    if (alder.forhøyetInntektskrav(skjæringstidspunkt))
        subsumsjonObserver.`§8-51 ledd 2`(oppfylt, skjæringstidspunkt, alderPåSkjæringstidspunkt, grunnlag, minimumInntekt)
    else
        subsumsjonObserver.`§8-3 ledd 2 punktum 1`(oppfylt, skjæringstidspunkt, grunnlag, minimumInntekt)

    if (oppfylt) aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")
    else aktivitetslogg.warn("Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag")

    return oppfylt
}
