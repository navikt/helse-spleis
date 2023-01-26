package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Varselkode.RV_SØ_1
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.AktivitetsloggObserver
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import org.junit.jupiter.api.Assertions.assertFalse
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
        aktivitetslogg.varsel(RV_SØ_1)
        aktivitetslogg.funksjonellFeil(RV_SØ_1)
        try {
            aktivitetslogg.logiskFeil("Dette er en severe")
        } catch (_: Exception) {}

        testObserver.assertAktivitet('I', melding = "Dette er en info-melding", kontekster = listOf(personkontekst, arbeidsgiverkontekst, vedtaksperiodekontekst))
        testObserver.assertAktivitet('W', kode = RV_SØ_1, melding = "Søknaden inneholder permittering. Vurder om permittering har konsekvens for rett til sykepenger", kontekster = listOf(personkontekst, arbeidsgiverkontekst, vedtaksperiodekontekst))
        testObserver.assertAktivitet('E', melding = "Søknaden inneholder permittering. Vurder om permittering har konsekvens for rett til sykepenger", kontekster = listOf(personkontekst, arbeidsgiverkontekst, vedtaksperiodekontekst))
        testObserver.assertAktivitet('S', melding = "Dette er en severe", kontekster = listOf(personkontekst, arbeidsgiverkontekst, vedtaksperiodekontekst))
    }

    @Test
    fun `fanger opp varsler som blir opprettet av varselkode`() {
        val personkontekst = TestKontekst("Person", "Person 1")

        aktivitetslogg.register(testObserver)
        aktivitetslogg.kontekst(personkontekst)
        aktivitetslogg.varsel(RV_SØ_1)

        testObserver.assertAktivitet('W', RV_SØ_1, melding = "Søknaden inneholder permittering. Vurder om permittering har konsekvens for rett til sykepenger", kontekster = listOf(personkontekst))
    }

    @Test
    fun `fanger opp behov`() {
        val personkontekst = TestKontekst("Person", "Person 1")

        aktivitetslogg.register(testObserver)
        aktivitetslogg.kontekst(personkontekst)
        aktivitetslogg.behov(Aktivitet.Behov.Behovtype.Godkjenning, "Dette er et behov", emptyMap())
        assertFalse(testObserver.isEmpty())
    }

    private val testObserver = object : AktivitetsloggObserver {
        private val aktiviteter = mutableListOf<Map<String, Any>>()
        override fun aktivitet(id: UUID, label: Char, melding: String, kontekster: List<SpesifikkKontekst>, tidsstempel: LocalDateTime) {
            aktiviteter.add(mapOf("type" to label, "melding" to melding, "kontekster" to kontekster))
        }

        override fun varsel(id: UUID, label: Char, kode: Varselkode?, melding: String, kontekster: List<SpesifikkKontekst>, tidsstempel: LocalDateTime) {
            val actual = mutableMapOf(
                "type" to label,
                "melding" to melding,
                "kontekster" to kontekster
            ).apply {
                if (kode != null) put("varselkode", kode)
            }.toMap()
            aktiviteter.add(actual)
        }

        fun assertAktivitet(type: Char, kode: Varselkode? = null, melding: String, kontekster: List<Aktivitetskontekst>) {
            val expected = mutableMapOf(
                    "type" to type,
                    "melding" to melding,
                    "kontekster" to kontekster.map { it.toSpesifikkKontekst() }
                ).apply {
                    if (kode != null) put("varselkode", kode)
                }.toMap()
            assertTrue(aktiviteter.contains(expected))
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
