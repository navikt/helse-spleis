package no.nav.helse.person

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AktivitetsloggObserverTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun setUp() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `fanger opp nye aktiviteter`() {
        val personkontekst = TestKontekst("Person", "Person 1")
        val arbeidsgiverkontekst = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        val vedtaksperiodekontekst = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")

        aktivitetslogg.register(testObserver)
        aktivitetslogg.kontekst(personkontekst)
        aktivitetslogg.kontekst(arbeidsgiverkontekst)
        aktivitetslogg.kontekst(vedtaksperiodekontekst)

        aktivitetslogg.info("Dette er en info-melding")
        aktivitetslogg.warn("Dette er et varsel")
        aktivitetslogg.funksjonellFeil("Dette er en error")
        try {
            aktivitetslogg.severe("Dette er en severe")
        } catch (_: Exception) {}

        testObserver.assertAktivitet('I', "Dette er en info-melding", listOf(personkontekst, arbeidsgiverkontekst, vedtaksperiodekontekst))
        testObserver.assertAktivitet('W', "Dette er et varsel", listOf(personkontekst, arbeidsgiverkontekst, vedtaksperiodekontekst))
        testObserver.assertAktivitet('E', "Dette er en error", listOf(personkontekst, arbeidsgiverkontekst, vedtaksperiodekontekst))
        testObserver.assertAktivitet('S', "Dette er en severe", listOf(personkontekst, arbeidsgiverkontekst, vedtaksperiodekontekst))
    }

    @Test
    fun `fanger ikke opp behov`() {
        val personkontekst = TestKontekst("Person", "Person 1")

        aktivitetslogg.register(testObserver)
        aktivitetslogg.kontekst(personkontekst)
        aktivitetslogg.behov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning, "Dette er et behov", emptyMap())
        assertTrue(testObserver.isEmpty())
    }

    private val testObserver = object : AktivitetsloggObserver {
        private val aktiviteter = mutableListOf<Map<String, Any>>()
        override fun aktivitet(label: Char, melding: String, kontekster: List<SpesifikkKontekst>, tidsstempel: String) {
            aktiviteter.add(mapOf("type" to label, "melding" to melding, "kontekster" to kontekster))
        }

        fun assertAktivitet(type: Char, melding: String, kontekster: List<Aktivitetskontekst>) {
            assertTrue(aktiviteter.contains(mapOf("type" to type, "melding" to melding, "kontekster" to kontekster.map { it.toSpesifikkKontekst() })))
        }

        fun isEmpty() = aktiviteter.isEmpty()
    }

    private class TestKontekst(
        private val type: String,
        private val melding: String
    ): Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(type, mapOf(type to melding))
    }

}
