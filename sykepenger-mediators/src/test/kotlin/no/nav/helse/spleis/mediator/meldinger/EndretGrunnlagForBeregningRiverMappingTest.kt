package no.nav.helse.spleis.mediator.meldinger

import no.nav.helse.Toggle
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Grunnlag
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.spleis.meldinger.EndretGrunnlagForBeregningRiver
import no.nav.helse.spleis.meldinger.model.EndretGrunnlagForBeregningMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class EndretGrunnlagForBeregningRiverMappingTest :
    RiverMappingTest<EndretGrunnlagForBeregningMessage>(
        hendelse = EndretGrunnlagForBeregningMessage::class,
        registrer = { rapid, mediator -> EndretGrunnlagForBeregningRiver(rapid, mediator) },
    ) {
    @Test
    fun `avslagsdager endret gir revurdering fra den tidligste dagen`() =
        Toggle.Avslagsdagerbehov.enable {
            val message = sendJson(avslagsdagerEndret(perioder = """[${periode(10.mars, 12.mars)}, ${periode(5.januar, 6.januar)}]"""))
            assertEquals(Grunnlag.Avslagsdager, message.endretGrunnlag)
            assertEquals(5.januar, message.fom)
        }

    @Test
    fun `avslagsdager endret ved manuell vurdering har saksbehandler som avsender`() =
        Toggle.Avslagsdagerbehov.enable {
            assertEquals(Avsender.SAKSBEHANDLER, sendJson(avslagsdagerEndret(manuellVurdering = true)).avsender)
            assertEquals(Avsender.SYSTEM, sendJson(avslagsdagerEndret(manuellVurdering = false)).avsender)
            assertEquals(Avsender.SYSTEM, sendJson(avslagsdagerEndret()).avsender)
        }

    @Test
    fun `avslagsdager endret ignoreres når togglen er av`() =
        Toggle.Avslagsdagerbehov.disable {
            assertThrows<IllegalStateException> { sendJson(avslagsdagerEndret()) }
        }

    @Test
    fun `avslagsdager endret uten gyldige perioder avvises`() =
        Toggle.Avslagsdagerbehov.enable {
            listOf(
                null,
                "[]",
                """[{ "fom": "2018-01-05" }]""",
                """[{ "tom": "2018-01-05" }]""",
                """[{ "fom": "ikke en dato", "tom": "2018-01-05" }]""",
                """[{ "fom": "2018-01-06", "tom": "2018-01-05" }]""",
            ).forEach { perioder ->
                assertThrows<IllegalStateException>("perioder: $perioder") { sendJson(avslagsdagerEndret(perioder = perioder)) }
            }
        }

    @Test
    fun `graderte andre ytelser endret leser fom`() {
        val message = sendJson(melding("graderte_andre_ytelser_endret", """"fom": "2018-01-05""""))
        assertEquals(Grunnlag.GraderteAndreYtelser, message.endretGrunnlag)
        assertEquals(5.januar, message.fom)
    }

    @Test
    fun `inntektsendringer leser inntektsendringFom`() {
        val message = sendJson(melding("inntektsendringer", """"inntektsendringFom": "2018-01-07""""))
        assertEquals(Grunnlag.Inntektsendringer, message.endretGrunnlag)
        assertEquals(7.januar, message.fom)
    }

    private fun periode(
        fom: LocalDate,
        tom: LocalDate,
    ) = """{ "fom": "$fom", "tom": "$tom" }"""

    private fun avslagsdagerEndret(
        perioder: String? = "[${periode(5.januar, 6.januar)}]",
        manuellVurdering: Boolean? = null,
    ) = melding(
        "avslagsdager_endret",
        listOfNotNull(
            """"vurderingId": "${UUID.randomUUID()}"""",
            perioder?.let { """"perioder": $it""" },
            manuellVurdering?.let { """"manuellVurdering": $it""" },
        ).joinToString(),
    )

    private fun melding(
        eventName: String,
        felter: String,
    ) = """{
        "@event_name": "$eventName",
        "@id": "${UUID.randomUUID()}",
        "@opprettet": "${LocalDateTime.now()}",
        "fødselsnummer": "12029240045",
        $felter
    }"""
}
