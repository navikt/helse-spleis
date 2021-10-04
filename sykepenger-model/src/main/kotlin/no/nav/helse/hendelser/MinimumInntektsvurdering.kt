package no.nav.helse.hendelser

import no.nav.helse.Fødselsnummer
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.somFødselsnummer
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate

internal fun validerMinimumInntekt(
    aktivitetslogg: IAktivitetslogg,
    fødselsnummer: Fødselsnummer,
    skjæringstidspunkt: LocalDate,
    grunnlagForSykepengegrunnlag: Sykepengegrunnlag,
): Boolean {
    val alder = fødselsnummer.alder()
    val minimumInntekt = alder.minimumInntekt(skjæringstidspunkt)
    val oppfylt = grunnlagForSykepengegrunnlag.grunnlagForSykepengegrunnlag > minimumInntekt


    if (alder.forhøyetInntektskrav(skjæringstidspunkt))
        aktivitetslogg.etterlevelse.`§8-51 ledd 2`(oppfylt, skjæringstidspunkt, grunnlagForSykepengegrunnlag.grunnlagForSykepengegrunnlag, minimumInntekt)
    else
        aktivitetslogg.etterlevelse.`§8-3 ledd 2`(oppfylt, skjæringstidspunkt, grunnlagForSykepengegrunnlag.grunnlagForSykepengegrunnlag, minimumInntekt)

    if (oppfylt) aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")
    else aktivitetslogg.warn("Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag")

    return oppfylt
}
