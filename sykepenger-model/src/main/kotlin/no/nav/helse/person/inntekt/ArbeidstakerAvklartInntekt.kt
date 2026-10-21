package no.nav.helse.person.inntekt

import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.tilstandsmaskin.*

/**
 * I de tilfellene vi aldri mottar inntektsmelding (ingen perioder på skjæringstidspunktet har en faktaavklartInntekt)
 * så kan det allikevel være at vi har avklart inntektsspørsmålet, men da i form av å ha gitt opp ventingen - i påvente av å bruke skatt.
 * Da sjekker vi om minst én periode på skjæringstidspunktet har "kommet seg forbi" AvventerInntektsmelding
 */

internal fun List<Vedtaksperiode>.harAvklartArbeidstakerinntekt(): Boolean {
    val første = firstOrNull() ?: return false
    check(all { it.yrkesaktivitet === første.yrkesaktivitet && it.skjæringstidspunkt == første.skjæringstidspunkt }) { "Hva holder du på med?" }
    return any { it.tilstand.harAvklartArbeidstakerinntekt() }
}

private fun Vedtaksperiodetilstand.harAvklartArbeidstakerinntekt(): Boolean =
    when (this) {
        // Om vi ikke har kommet oss "forbi" AvventerInntektsmelding
        // så indikrerer tilstanden at vi _ikke_ har avklart inntekt
        Start,
        AvventerInfotrygdHistorikk,
        AvventerInntektsmelding,
        AvventerAvsluttetUtenUtbetaling,
        AvsluttetUtenUtbetaling,
        -> false

        // Det kan være en situasjon hvor "halen" på sykerfraværet er annullert/forkastet
        // Da kan ikke denne forkastede perioden i seg selv svare ut om vi har avklart inntekt
        TilInfotrygd -> false

        // Om perioden har kommet seg "forbi" AvventerInntektsmelding så indikrerer tilstanden at vi _har_ avklart inntekt
        Avsluttet,
        AvventerAnnullering,
        AvventerAnnulleringTilUtbetaling,
        AvventerBlokkerendePeriode,
        AvventerGodkjenning,
        AvventerGodkjenningRevurdering,
        AvventerHistorikk,
        AvventerHistorikkRevurdering,
        AvventerInntektsopplysningerForAnnenArbeidsgiver,
        AvventerRefusjonsopplysningerAnnenPeriode,
        AvventerRevurdering,
        AvventerRevurderingTilUtbetaling,
        AvventerSimulering,
        AvventerSimuleringRevurdering,
        AvventerSøknadForOverlappendePeriode,
        AvventerVilkårsprøving,
        AvventerVilkårsprøvingRevurdering,
        TilAnnullering,
        TilUtbetaling,
        -> true

        // Selvstendig
        SelvstendigAvsluttet,
        SelvstendigAvventerBlokkerendePeriode,
        SelvstendigAvventerGodkjenning,
        SelvstendigAvventerGodkjenningRevurdering,
        SelvstendigAvventerHistorikk,
        SelvstendigAvventerHistorikkRevurdering,
        SelvstendigAvventerInfotrygdHistorikk,
        SelvstendigAvventerRevurdering,
        SelvstendigAvventerRevurderingTilUtbetaling,
        SelvstendigAvventerSimulering,
        SelvstendigAvventerSimuleringRevurdering,
        SelvstendigAvventerVilkårsprøving,
        SelvstendigAvventerVilkårsprøvingRevurdering,
        SelvstendigStart,
        SelvstendigTilUtbetaling,
        -> error("Disse tilstandende hører til Selvstendig!! Hva er det du holder på med??")
    }
