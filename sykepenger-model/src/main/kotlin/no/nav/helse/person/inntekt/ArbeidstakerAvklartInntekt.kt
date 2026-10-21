package no.nav.helse.person.inntekt

import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Selvstendig
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.tilstandsmaskin.Avsluttet
import no.nav.helse.person.tilstandsmaskin.AvsluttetUtenUtbetaling
import no.nav.helse.person.tilstandsmaskin.AvventerAnnullering
import no.nav.helse.person.tilstandsmaskin.AvventerAnnulleringTilUtbetaling
import no.nav.helse.person.tilstandsmaskin.AvventerAvsluttetUtenUtbetaling
import no.nav.helse.person.tilstandsmaskin.AvventerBlokkerendePeriode
import no.nav.helse.person.tilstandsmaskin.AvventerGodkjenning
import no.nav.helse.person.tilstandsmaskin.AvventerGodkjenningRevurdering
import no.nav.helse.person.tilstandsmaskin.AvventerHistorikk
import no.nav.helse.person.tilstandsmaskin.AvventerHistorikkRevurdering
import no.nav.helse.person.tilstandsmaskin.AvventerInfotrygdHistorikk
import no.nav.helse.person.tilstandsmaskin.AvventerInntektsmelding
import no.nav.helse.person.tilstandsmaskin.AvventerInntektsopplysningerForAnnenArbeidsgiver
import no.nav.helse.person.tilstandsmaskin.AvventerRefusjonsopplysningerAnnenPeriode
import no.nav.helse.person.tilstandsmaskin.AvventerRevurdering
import no.nav.helse.person.tilstandsmaskin.AvventerRevurderingTilUtbetaling
import no.nav.helse.person.tilstandsmaskin.AvventerSimulering
import no.nav.helse.person.tilstandsmaskin.AvventerSimuleringRevurdering
import no.nav.helse.person.tilstandsmaskin.AvventerSøknadForOverlappendePeriode
import no.nav.helse.person.tilstandsmaskin.AvventerVilkårsprøving
import no.nav.helse.person.tilstandsmaskin.AvventerVilkårsprøvingRevurdering
import no.nav.helse.person.tilstandsmaskin.Start
import no.nav.helse.person.tilstandsmaskin.TilAnnullering
import no.nav.helse.person.tilstandsmaskin.TilInfotrygd
import no.nav.helse.person.tilstandsmaskin.TilUtbetaling
import no.nav.helse.person.tilstandsmaskin.Vedtaksperiodetilstand

/**
 * I de tilfellene vi aldri mottar inntektsmelding (ingen perioder på skjæringstidspunktet har en faktaavklartInntekt)
 * så kan det allikevel være at vi har avklart inntektsspørsmålet, men da i form av å ha gitt opp ventingen - i påvente av å bruke skatt.
 * Da sjekker vi om minst én periode på skjæringstidspunktet har "kommet seg forbi" AvventerInntektsmelding
 */

internal fun List<Vedtaksperiode>.harAvklartArbeidstakerinntekt(): Boolean {
    val første = firstOrNull() ?: return false
    check(all { it.yrkesaktivitet === første.yrkesaktivitet && it.skjæringstidspunkt == første.skjæringstidspunkt }) { "Hva holder du på med?" }
    check(første.yrkesaktivitet.yrkesaktivitetstype != Selvstendig) { "Selvstendig næringsdrivende har ikke arbeidstakerinntekt!! Hva er det du holder på med??" }
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
    }
