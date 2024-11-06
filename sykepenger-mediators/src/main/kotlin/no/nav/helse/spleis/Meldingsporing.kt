package no.nav.helse.spleis

import java.util.UUID

/** verdier som innkommende melding har i seg, ikke nødvendigvis de verdier Person-objektet i modellen har */
data class Meldingsporing(
    val id: UUID, // alle meldinger skal ha en identifikator
    val fødselsnummer: String, // alle meldinger skal gjelde en person
    @Deprecated("vi prøver å jobbe oss ut av bruken av dette feltet")
    val aktørId: String
)
