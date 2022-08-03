package no.nav.helse.spleis.meldinger

import no.nav.helse.januar
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.TestMessageFactory
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.AKTØRID
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.INNTEKT
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.ORGNUMMER
import no.nav.helse.spleis.e2e.AbstractEndToEndMediatorTest.Companion.UNG_PERSON_FNR_2018
import org.junit.jupiter.api.Test

internal class OverstyrInntektRiverTest: RiverTest() {
    override fun river(rapidsConnection: RapidsConnection, mediator: IMessageMediator) {
        OverstyrInntektRiver(rapidsConnection, mediator)
    }

    private val testMessageFactory = TestMessageFactory(
        UNG_PERSON_FNR_2018,
        AKTØRID,
        ORGNUMMER,
        INNTEKT
    )

    @Test
    fun `kan mappe melding om overstyring av inntekt til modell uten feil`() {
        assertNoErrors(
            testMessageFactory.lagOverstyrInntekt(
                1.januar,
                forklaringMap = mapOf("forklaring" to "forklaring"),
                subsumsjonsMap = emptyMap()
            )
        )

        assertNoErrors(
            testMessageFactory.lagOverstyrInntekt(
                1.januar,
                forklaringMap = mapOf("forklaring" to "forklaring"),
                subsumsjonsMap = mapOf("subsumsjon" to mapOf(
                    "paragraf" to "8-28"
                ))
            )
        )

        assertNoErrors(
            testMessageFactory.lagOverstyrInntekt(
                1.januar,
                forklaringMap = mapOf("forklaring" to "forklaring"),
                subsumsjonsMap = mapOf("subsumsjon" to mapOf(
                    "paragraf" to "8-28",
                    "ledd" to "3",
                    "bokstav" to "b"
                )
                ))
        )
    }

    @Test
    fun `skal feile hvis vi har en subsumsjon uten paragraf`() {
        assertErrors(
            testMessageFactory.lagOverstyrInntekt(
                1.januar,
                forklaringMap = mapOf("forklaring" to "forklaring"),
                subsumsjonsMap = mapOf("subsumsjon" to mapOf(
                    "ledd" to "3",
                    "bokstav" to "b"
                ))
            )
        )
    }

    @Test
    fun `skal feile hvis vi mangler forklaring fra saksbehandler`() {
        assertErrors(
            testMessageFactory.lagOverstyrInntekt(
                1.januar,
                subsumsjonsMap = mapOf("subsumsjon" to mapOf(
                    "paragraf" to "8-28"
                ))
            )
        )
    }
}
