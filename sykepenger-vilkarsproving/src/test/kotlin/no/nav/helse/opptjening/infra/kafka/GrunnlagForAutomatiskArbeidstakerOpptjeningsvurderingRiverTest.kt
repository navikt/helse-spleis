package no.nav.helse.opptjening.infra.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.opptjening.application.InMemoryVilkårsprøvingRepository
import no.nav.helse.opptjening.application.InMemoryVilkårsvurderingRepository
import no.nav.helse.opptjening.application.OpptjeningService
import no.nav.helse.opptjening.domain.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.opptjening.domain.Arbeidssituasjon
import no.nav.helse.opptjening.domain.Kodeverkkode.IKKE_OPPTJENING_ARBEID_ELLER_YTELSE
import no.nav.helse.opptjening.domain.Kodeverkkode.OPPTJENING_MINST_4_UKER
import no.nav.helse.opptjening.domain.Opptjeningsgrunnlag
import no.nav.helse.opptjening.domain.Opptjeningsprøving
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GrunnlagForAutomatiskArbeidstakerOpptjeningsvurderingRiverTest {

    private val vurderinger = InMemoryVilkårsvurderingRepository()
    private val prøvinger = InMemoryVilkårsprøvingRepository()
    private val rapid = TestRapid().apply {
        GrunnlagForAutomatiskArbeidstakerOpptjeningsvurderingRiver(this, OpptjeningService(vurderinger, prøvinger))
    }

    // Normalflyten: løsningen fullfører prøvingen, vurderingen oppstår, og det opprinnelige
    // behovet sendes videre med løsningen påført
    @Test
    fun `løsning fullfører prøvingen og besvarer opprinnelig behov`() {
        val påbegynt = påbegyntPrøving()

        rapid.sendTestMessage(arbeidsforholdløsning(arbeidsforhold(ansattSiden = "2018-01-01", ansattTil = "2018-01-31")), "123")

        assertTrue(påbegynt.erAvsluttet)
        val vurdering = vurderinger.alleVurderinger.single()
        assertEquals(påbegynt.id, vurdering.prøvingId)
        assertEquals(OPPTJENING_MINST_4_UKER, vurdering.kodeverkkode)

        assertEquals(1, rapid.inspektør.size)
        val svar = rapid.inspektør.message(0)
        val partisjonsnøkkel = rapid.inspektør.key(0)
        assertEquals("123", partisjonsnøkkel)
        assertEquals(OPPRINNELIG_BEHOV_ID, svar.path("@id").asText())
        assertEquals(listOf("Opptjeningsvurdering"), svar.path("@behov").map { it.asText() })
        assertEquals(vurdering.id.toString(), svar.path("@løsning").path("Opptjeningsvurdering").path("id").asText())
    }

    // Kodeverkkoden skal utledes av arbeidsforholdene som kom inn på løsningen
    @Test
    fun `for kort opptjening gir ikke oppfylt`() {
        påbegyntPrøving()

        rapid.sendTestMessage(arbeidsforholdløsning(arbeidsforhold(ansattSiden = "2018-01-05", ansattTil = "2018-01-31")))

        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, vurderinger.alleVurderinger.single().kodeverkkode)
    }

    // Løpende arbeidsforhold kommer uten ansattTil, og skal mappes til null
    @Test
    fun `arbeidsforhold uten ansattTil er løpende`() {
        påbegyntPrøving()

        rapid.sendTestMessage(arbeidsforholdløsning(arbeidsforhold(ansattSiden = "2018-01-01", ansattTil = null)))

        val arbeidsforhold = arbeidsforholdPåVurdering().single()
        assertEquals(1.januar, arbeidsforhold.ansettelseperiode.start)
        assertEquals(LocalDate.MAX, arbeidsforhold.ansettelseperiode.endInclusive)
        assertEquals(OPPTJENING_MINST_4_UKER, vurderinger.alleVurderinger.single().kodeverkkode)
    }

    // Aareg kan sende arbeidsforhold uten orgnummer; de skal filtreres bort
    @Test
    fun `arbeidsforhold uten orgnummer filtreres bort`() {
        påbegyntPrøving()

        rapid.sendTestMessage(
            arbeidsforholdløsning(
                arbeidsforhold(orgnummer = "", ansattSiden = "2018-01-01", ansattTil = "2018-01-31"),
                arbeidsforhold(orgnummer = ORGNUMMER, ansattSiden = "2018-01-01", ansattTil = "2018-01-31")
            )
        )

        assertEquals(listOf(ORGNUMMER), arbeidsforholdPåVurdering().map { it.orgnummer })
    }

    // Ugyldige perioder (ansattTil før ansattSiden) skal filtreres bort framfor å
    // krasje domenemodellen
    @Test
    fun `arbeidsforhold med ansattTil før ansattSiden filtreres bort`() {
        påbegyntPrøving()

        rapid.sendTestMessage(
            arbeidsforholdløsning(
                arbeidsforhold(orgnummer = "111111111", ansattSiden = "2018-01-31", ansattTil = "2018-01-01"),
                arbeidsforhold(orgnummer = ORGNUMMER, ansattSiden = "2018-01-01", ansattTil = "2018-01-31")
            )
        )

        assertEquals(listOf(ORGNUMMER), arbeidsforholdPåVurdering().map { it.orgnummer })
    }

    @Test
    fun `alle arbeidsforholdtyper mappes`() {
        påbegyntPrøving()

        rapid.sendTestMessage(
            arbeidsforholdløsning(
                arbeidsforhold(orgnummer = "111111111", type = "FORENKLET_OPPGJØRSORDNING", ansattSiden = "2018-01-01", ansattTil = "2018-01-31"),
                arbeidsforhold(orgnummer = "222222222", type = "FRILANSER", ansattSiden = "2018-01-01", ansattTil = "2018-01-31"),
                arbeidsforhold(orgnummer = "333333333", type = "MARITIMT", ansattSiden = "2018-01-01", ansattTil = "2018-01-31"),
                arbeidsforhold(orgnummer = "444444444", type = "ORDINÆRT", ansattSiden = "2018-01-01", ansattTil = "2018-01-31")
            )
        )

        assertEquals(
            listOf(
                Arbeidsforholdtype.FORENKLET_OPPGJØRSORDNING,
                Arbeidsforholdtype.FRILANSER,
                Arbeidsforholdtype.MARITIMT,
                Arbeidsforholdtype.ORDINÆRT
            ),
            arbeidsforholdPåVurdering().map { it.type }
        )
    }

    // Løsning uten treff på en påbegynt prøving skal ikke gi noe utgående svar
    @Test
    fun `løsning uten påbegynt prøving gir ingen melding`() {
        rapid.sendTestMessage(arbeidsforholdløsning(arbeidsforhold(ansattSiden = "2018-01-01", ansattTil = "2018-01-31")))

        assertEquals(0, rapid.inspektør.size)
        assertEquals(0, vurderinger.antallLagringer)
    }

    // Duplikate løsninger skal ikke gi dobbelt svar eller en ny vurdering
    @Test
    fun `duplikat løsning gir ingen ny melding`() {
        påbegyntPrøving()
        val melding = arbeidsforholdløsning(arbeidsforhold(ansattSiden = "2018-01-01", ansattTil = "2018-01-31"))

        rapid.sendTestMessage(melding)
        assertEquals(1, rapid.inspektør.size)

        rapid.sendTestMessage(melding)
        assertEquals(1, rapid.inspektør.size)
        assertEquals(1, vurderinger.antallLagringer)
        assertEquals(OPPTJENING_MINST_4_UKER, vurderinger.alleVurderinger.single().kodeverkkode)
    }

    // Riveren skal bare behandle det endelige svaret på behovet
    @Test
    fun `løsning som ikke er final ignoreres`() {
        val påbegynt = påbegyntPrøving()

        rapid.sendTestMessage(
            arbeidsforholdløsning(
                arbeidsforhold(ansattSiden = "2018-01-01", ansattTil = "2018-01-31"),
                erFinal = false
            )
        )

        assertEquals(0, rapid.inspektør.size)
        assertFalse(påbegynt.erAvsluttet)
    }

    // En ukjent arbeidsforholdtype skal stoppes av valideringen, ikke krasje mappingen
    @Test
    fun `ukjent arbeidsforholdtype ignoreres av valideringen`() {
        val påbegynt = påbegyntPrøving()

        rapid.sendTestMessage(
            arbeidsforholdløsning(arbeidsforhold(type = "NOE_HELT_ANNET", ansattSiden = "2018-01-01", ansattTil = "2018-01-31"))
        )

        assertEquals(0, rapid.inspektør.size)
        assertFalse(påbegynt.erAvsluttet)
    }

    // Prøvinger som allerede er avsluttet skal ikke behandles på nytt
    @Test
    fun `løsning på allerede avsluttet prøving gir ingen melding`() {
        val (prøving, vurdering) = Opptjeningsprøving.start(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.SelvstendigNæringsdrivende)
        prøvinger.opprett(prøving)
        vurderinger.lagre(vurdering!!)

        rapid.sendTestMessage(arbeidsforholdløsning(arbeidsforhold(ansattSiden = "2018-01-01", ansattTil = "2018-01-31")))

        assertEquals(0, rapid.inspektør.size)
        assertEquals(Opptjeningsgrunnlag.SelvstendigNæringsdrivende, vurderinger.alleVurderinger.single().grunnlag)
    }

    private fun påbegyntPrøving() =
        Opptjeningsprøving.start(FØDSELSNUMMER, 1.februar, Arbeidssituasjon.Arbeidstaker).prøving.also { prøvinger.opprett(it) }

    private fun arbeidsforholdPåVurdering() =
        (vurderinger.alleVurderinger.single().grunnlag as Opptjeningsgrunnlag.Arbeidstaker).arbeidsforhold

    private companion object {
        const val FØDSELSNUMMER = "12029240045"
        const val ORGNUMMER = "987654321"
        val OPPRINNELIG_BEHOV_ID: String = UUID.randomUUID().toString()

        fun arbeidsforhold(
            orgnummer: String = ORGNUMMER,
            type: String = "ORDINÆRT",
            ansattSiden: String,
            ansattTil: String? = null
        ) = """
        {
          "orgnummer": "$orgnummer",
          "type": "$type",
          "ansattSiden": "$ansattSiden",
          "ansattTil": ${ansattTil?.let { "\"$it\"" } ?: "null"}
        }
        """.trimIndent()

        @Language("JSON")
        fun arbeidsforholdløsning(vararg arbeidsforhold: String, erFinal: Boolean = true) = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["ArbeidsforholdV2"],
          "@final": $erFinal,
          "fødselsnummer": "$FØDSELSNUMMER",
          "skjæringstidspunkt": "2018-02-01",
          "opprinneligBehov": {
            "@event_name": "behov",
            "@id": "$OPPRINNELIG_BEHOV_ID",
            "@behov": ["Opptjeningsvurdering"],
            "fødselsnummer": "$FØDSELSNUMMER",
            "skjæringstidspunkt": "2018-02-01",
            "arbeidssituasjon": "Arbeidstaker"
          },
          "@løsning": {
            "ArbeidsforholdV2": [${arbeidsforhold.joinToString()}]
          }
        }
        """
    }
}
