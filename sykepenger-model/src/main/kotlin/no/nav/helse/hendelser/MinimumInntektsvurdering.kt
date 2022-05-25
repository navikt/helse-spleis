package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.Fødselsnummer
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.etterlevelse.SubsumsjonObserver

internal fun validerMinimumInntekt(
    aktivitetslogg: IAktivitetslogg,
    fødselsnummer: Fødselsnummer,
    skjæringstidspunkt: LocalDate,
    grunnlagForSykepengegrunnlag: Sykepengegrunnlag,
    subsumsjonObserver: SubsumsjonObserver
): Boolean {
    val alder = fødselsnummer.alder()
    val oppfylt = grunnlagForSykepengegrunnlag.oppfyllerKravTilMinimumInntekt(alder, skjæringstidspunkt, subsumsjonObserver)

    if (oppfylt) aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")
    else aktivitetslogg.warn("Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag")

    return oppfylt
}
