package no.nav.helse.spleis

import no.nav.helse.hendelser.MeldingsreferanseId

/** verdier som innkommende melding har i seg, ikke nødvendigvis de verdier Person-objektet i modellen har */
data class Meldingsporing(
    val id: MeldingsreferanseId, // alle meldinger skal ha en identifikator
    val fødselsnummer: String // alle meldinger skal gjelde en person
)
