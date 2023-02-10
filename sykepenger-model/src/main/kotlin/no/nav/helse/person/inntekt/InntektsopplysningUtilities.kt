package no.nav.helse.person.inntekt

import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

/*
Util? Really? Denne flytter vi, tenker jeg.

Inneholder ting som lå i companion objektet til Inntektopplysning som hindret flytting av den klassen.
*/
internal fun List<Inntektsopplysning>.valider(aktivitetslogg: IAktivitetslogg) {
    if (all { it is SkattSykepengegrunnlag }) {
        aktivitetslogg.funksjonellFeil(Varselkode.RV_VV_5)
    }
}

internal fun List<Inntektsopplysning>.validerStartdato(aktivitetslogg: IAktivitetslogg) {
    if (distinctBy { it.dato }.size <= 1 && none { it is SkattSykepengegrunnlag || it is IkkeRapportert }) return
    aktivitetslogg.varsel(Varselkode.RV_VV_2)
}