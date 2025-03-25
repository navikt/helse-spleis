package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class AktivitetsloggTest {

    private lateinit var aktivitetslogg: Aktivitetslogg
    private lateinit var person: TestKontekst

    @BeforeEach
    internal fun setUp() {
        person = TestKontekst("Person", "Person 1")
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `toString av aktivitet`() {
        val infomelding = "info message"
        val hendelse1 = aktivitetslogg.kontekst(person)
        hendelse1.info(infomelding)
        val expected = "$infomelding (Person Person: Person 1)"
        assertTrue(aktivitetslogg.toString().contains(expected)) { "Expected $aktivitetslogg to contain <$expected>" }
    }

    @Test
    fun `inneholder original melding`() {
        val infomelding = "info message"
        aktivitetslogg.info(infomelding)
        assertInfo(infomelding)
    }

    @Test
    fun `har ingen feil ved default`() {
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `severe oppdaget og kaster exception`() {
        val melding = "Severe error"
        assertThrows<Aktivitetslogg.AktivitetException> { aktivitetslogg.logiskFeil(melding) }
        assertTrue(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertTrue(aktivitetslogg.toString().contains(melding))
        assertLogiskFeil(melding)
    }

    @Test
    fun `logge noe som inneholder % uten parametre`() {
        assertDoesNotThrow { aktivitetslogg.info("Jeg er 100 % overrasket over at dette feilet") }
    }

    @Test
    fun `overskriver like kontekster`() {
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        val vedtaksperiode2 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 2")

        val hendelse = aktivitetslogg
            .kontekst(person)
            .kontekst(arbeidsgiver1)
            .kontekst(vedtaksperiode1)
            .kontekst(vedtaksperiode2)
        hendelse.info("Hei på deg")
        assertEquals(1, aktivitetslogg.aktiviteter.size)
        val aktivitet = aktivitetslogg.aktiviteter.first()
        assertEquals(3, aktivitet.kontekster.size)
        assertEquals(1, aktivitet.kontekster.filter { it.kontekstType == "Vedtaksperiode" }.size)
        assertEquals("Vedtaksperiode 2", aktivitet.kontekster.first { it.kontekstType == "Vedtaksperiode" }.kontekstMap.getValue("Vedtaksperiode"))
    }

    @Test
    fun `fjerner kontekster etter overskriving`() {
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        val arbeidsgiver2 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 2")
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")

        val hendelse = aktivitetslogg
            .kontekst(person)
            .kontekst(arbeidsgiver1)
            .kontekst(vedtaksperiode1)
            .kontekst(arbeidsgiver2) // arbeidsgiver 2 overskriver arbeidsgiver 1 over. Vedtaksperiode-kontekst forsvinner, siden de er lagt på etter arbeidsgiver-typen
        hendelse.info("Hei på deg")
        assertEquals(1, aktivitetslogg.aktiviteter.size)
        val aktivitet = aktivitetslogg.aktiviteter.first()
        assertEquals("Person 1, Arbeidsgiver 2", aktivitet.kontekster.joinToString { it.kontekstMap[it.kontekstType] ?: it.kontekstType })
    }

    @Test
    fun `error oppdaget`() {
        aktivitetslogg.funksjonellFeil(RV_VT_1)
        assertTrue(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertTrue(aktivitetslogg.toString().contains("Gir opp fordi tilstanden er nådd makstid"))
        assertFunksjonellFeil("Gir opp fordi tilstanden er nådd makstid")
    }

    @Test
    fun `warning oppdaget`() {
        aktivitetslogg.varsel(RV_SØ_1)
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertVarsel(RV_SØ_1)
    }

    @Test
    fun `Melding sendt til forelder`() {
        val hendelse = Aktivitetslogg(aktivitetslogg)
        "info message".also {
            hendelse.info(it)
            assertInfo(it, aktivitetslogg = hendelse)
            assertInfo(it, aktivitetslogg = aktivitetslogg)
        }
        hendelse.funksjonellFeil(RV_VT_1)
        assertFunksjonellFeil("Gir opp fordi tilstanden er nådd makstid", hendelse)
        assertFunksjonellFeil("Gir opp fordi tilstanden er nådd makstid", aktivitetslogg)
    }

    @Test
    fun `Melding sendt fra barnebarn til forelder`() {
        val arbeidsgiver = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        val vedtaksperiode = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        val hendelse = Aktivitetslogg(aktivitetslogg)
            .kontekst(person)
            .kontekst(arbeidsgiver)
            .kontekst(vedtaksperiode)
        "info message".also {
            hendelse.info(it)
            assertInfo(it, aktivitetslogg = hendelse)
            assertInfo(it, aktivitetslogg = aktivitetslogg)
        }
        hendelse.funksjonellFeil(RV_VT_1)
        assertFunksjonellFeil("Gir opp fordi tilstanden er nådd makstid", hendelse)
        assertFunksjonellFeil("Gir opp fordi tilstanden er nådd makstid", aktivitetslogg)
        assertFunksjonellFeil("Vedtaksperiode", aktivitetslogg)
        assertFunksjonellFeil("Arbeidsgiver", aktivitetslogg)
        assertFunksjonellFeil("Person", aktivitetslogg)
    }

    @Test
    fun `Vis bare arbeidsgiveraktivitet`() {
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        val hendelse1 = aktivitetslogg
            .kontekst(person)
            .kontekst(arbeidsgiver1)
            .kontekst(vedtaksperiode1)
        hendelse1.info("info message")
        hendelse1.varsel(RV_SØ_1)
        hendelse1.funksjonellFeil(RV_VT_1)

        val arbeidsgiver2 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 2")
        val vedtaksperiode2 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 2")
        val hendelse2 = aktivitetslogg
            .kontekst(person)
            .kontekst(arbeidsgiver2)
            .kontekst(vedtaksperiode2)
        hendelse2.info("info message")
        hendelse2.funksjonellFeil(RV_VT_1)
        assertEquals(5, aktivitetslogg.aktiviteter.size)
    }

    @Test
    fun `Behov kan ha detaljer`() {
        val hendelse1 = aktivitetslogg
            .kontekst(person)
        val param1 = "value"
        val param2 = LocalDate.now()
        hendelse1.behov(
            Aktivitet.Behov.Behovtype.Godkjenning, "Trenger godkjenning", mapOf(
            "param1" to param1,
            "param2" to param2
        )
        )

        assertEquals(1, aktivitetslogg.behov.size)
        assertEquals(1, aktivitetslogg.behov.first().alleKontekster.size)
        assertEquals(2, aktivitetslogg.behov.first().detaljer().size)
        assertEquals("Person 1", aktivitetslogg.behov.first().alleKontekster["Person"])
        assertEquals(param1, aktivitetslogg.behov.first().detaljer()["param1"])
        assertEquals(param2, aktivitetslogg.behov.first().detaljer()["param2"])
    }

    @Test
    fun `varselkode blir til varsel`() {
        val hendelse = aktivitetslogg
            .kontekst(person)
        hendelse.varsel(RV_SØ_1)
        assertEquals(1, aktivitetslogg.varsel.size)
        assertVarsel(RV_SØ_1)
    }

    private fun assertInfo(message: String, forventetKonteksttyper: List<String>? = null, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        val aktivitet = aktivitetslogg.aktiviteter.filter { it is Aktivitet.Info && message in it.toString() }
        assertEquals(1, aktivitet.size)
        if (forventetKonteksttyper != null) {
            assertEquals(forventetKonteksttyper, aktivitet.single().kontekster.map { it.kontekstType })
        }
    }

    private fun assertVarsel(forventetKode: Varselkode, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        val aktivitet = aktivitetslogg.aktiviteter.filterIsInstance<Aktivitet.Varsel>()
        assertEquals(1, aktivitet.size)
        assertEquals(forventetKode, aktivitet.single().kode)
    }

    private fun assertFunksjonellFeil(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        assertEquals(1, aktivitetslogg.aktiviteter.count { it is Aktivitet.FunksjonellFeil && message in it.toString() })
    }

    private fun assertLogiskFeil(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        assertEquals(1, aktivitetslogg.aktiviteter.count { it is Aktivitet.LogiskFeil && message in it.toString() })
    }

    private class TestKontekst(
        private val type: String,
        private val melding: String
    ) : Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(type, mapOf(type to melding))
    }
}
