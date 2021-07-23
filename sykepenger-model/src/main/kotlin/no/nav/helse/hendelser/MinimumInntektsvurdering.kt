package no.nav.helse.hendelser

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate

internal fun validerMinimumInntekt(
    aktivitetslogg: IAktivitetslogg,
    fødselsnummer: String,
    skjæringstidspunkt: LocalDate,
    grunnlagForSykepengegrunnlag: Inntekt,
): Boolean {
    val alder = Alder(fødselsnummer)
    val minimumInntekt = alder.minimumInntekt(skjæringstidspunkt)
    val oppfylt = grunnlagForSykepengegrunnlag > minimumInntekt


    when (alder.forhøyetInntektskrav(skjæringstidspunkt)) {
        true -> aktivitetslogg.etterlevelse.`§8-51 ledd 2`(oppfylt, skjæringstidspunkt, grunnlagForSykepengegrunnlag, minimumInntekt)
        else -> aktivitetslogg.etterlevelse.`§8-3 ledd 2`(oppfylt, skjæringstidspunkt, grunnlagForSykepengegrunnlag, minimumInntekt)
    }

    if (oppfylt) aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")
    else aktivitetslogg.warn("Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag")

    return oppfylt
}
