package no.nav.helse.person.etterlevelse

import no.nav.helse.person.*
import no.nav.helse.somFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class MaskinellJuristTest {

    @Test
    fun `jurist lytter på endringer av kontekst`() {
        val jurist = MaskinellJurist("10052088033".somFødselsnummer())

        jurist.nyKontekst(Kontekst("Arbeidsgiver", mapOf("organisasjonsnummer" to "123456789")))
        jurist.nyKontekst(Kontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to "6bce6c83-28ab-4a8c-b7f6-8402988bc8fc")))
        jurist.nyKontekst(Kontekst("Søknad", mapOf("søknadId" to "44ad28ba-5841-426a-a060-d0f54348f20e")))

        jurist.`§8-2 ledd 1`(true, LocalDate.now(), 1, emptyList(), 1)

        assertKontekster(
            jurist.vurderinger()[0],
            "fødselsnummer" to "10052088033",
            "organisasjonsnummer" to "123456789",
            "vedtaksperiodeId" to "6bce6c83-28ab-4a8c-b7f6-8402988bc8fc",
            "søknadId" to "44ad28ba-5841-426a-a060-d0f54348f20e"
        )
    }

    @Test
    fun `alltid nyeste kontekst som gjelder`() {
        val jurist = MaskinellJurist("10052088033".somFødselsnummer())

        jurist.nyKontekst(Kontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to "6bce6c83-28ab-4a8c-b7f6-8402988bc8fc")))
        jurist.nyKontekst(Kontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to "cf7fafd2-b7f3-43dc-95d0-dd66f0d20c35")))

        jurist.`§8-2 ledd 1`(true, LocalDate.now(), 1, emptyList(), 1)

        assertKontekster(
            jurist.vurderinger()[0],
            "fødselsnummer" to "10052088033",
            "vedtaksperiodeId" to "cf7fafd2-b7f3-43dc-95d0-dd66f0d20c35",
        )
    }

    private fun assertKontekster(juridiskVurdering: JuridiskVurdering, vararg kontekster: Pair<String, String>) {
        val inspektør = object : JuridiskVurderingVisitor {
            lateinit var kontekster: Map<String, String>

            init {
                juridiskVurdering.accept(this)
            }

            override fun preVisitVurdering(
                oppfylt: Boolean,
                versjon: LocalDate,
                paragraf: Paragraf,
                ledd: Ledd,
                punktum: List<Punktum>,
                bokstaver: List<Bokstav>,
                input: Map<String, Any>,
                output: Map<String, Any>,
                kontekster: Map<String, String>
            ) {
                this.kontekster = kontekster
            }
        }

        assertEquals(
            kontekster.toList().sortedBy { it.first },
            inspektør.kontekster.toList().sortedBy { it.first }
        )
    }

    private class Kontekst(private val kontekstType: String, private val verdier: Map<String, String> = emptyMap()): Aktivitetskontekst {
        override fun toSpesifikkKontekst(): SpesifikkKontekst = SpesifikkKontekst(kontekstType, verdier)
    }
}
