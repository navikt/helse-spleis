package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
        val hendelse1 = TestHendelse(aktivitetslogg.barn())
        hendelse1.kontekst(person)
        hendelse1.info(infomelding)
        val expected = "$infomelding (TestHendelse) (Person Person: Person 1)"
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
    fun `overskriver like kontekster`() {
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        val vedtaksperiode2 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 2")

        val hendelse = TestHendelse(aktivitetslogg.barn())
        hendelse.kontekst(person)
        hendelse.kontekst(arbeidsgiver1)
        hendelse.kontekst(vedtaksperiode1)
        hendelse.kontekst(vedtaksperiode2)
        hendelse.info("Hei på deg")
        assertEquals(1, aktivitetslogg.aktiviteter.size)
        val aktivitet = aktivitetslogg.aktiviteter.first()
        assertEquals(4, aktivitet.kontekster.size)
        assertEquals(1, aktivitet.kontekster.filter { it.kontekstType == "Vedtaksperiode" }.size)
        assertEquals("Vedtaksperiode 2", aktivitet.kontekster.first { it.kontekstType == "Vedtaksperiode" }.kontekstMap.getValue("Vedtaksperiode"))
    }

    @Test
    fun `fjerner kontekster etter overskriving`() {
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        val arbeidsgiver2 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 2")
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")

        val hendelse = TestHendelse(aktivitetslogg.barn())
        hendelse.kontekst(person)
        hendelse.kontekst(arbeidsgiver1)
        hendelse.kontekst(vedtaksperiode1)
        hendelse.kontekst(arbeidsgiver2) // arbeidsgiver 2 overskriver arbeidsgiver 1 over. Vedtaksperiode-kontekst forsvinner, siden de er lagt på etter arbeidsgiver-typen
        hendelse.info("Hei på deg")
        assertEquals(1, aktivitetslogg.aktiviteter.size)
        val aktivitet = aktivitetslogg.aktiviteter.first()
        assertEquals("TestHendelse, Person 1, Arbeidsgiver 2", aktivitet.kontekster.joinToString { it.kontekstMap[it.kontekstType] ?: it.kontekstType })
    }

    @Test
    fun kontekster() {
        val hendelse1 = TestHendelse(aktivitetslogg.barn())
        hendelse1.kontekst(person)
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        hendelse1.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        hendelse1.kontekst(vedtaksperiode1)
        hendelse1.behov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning, "Trenger godkjenning")
        hendelse1.varsel("Advarsel")
        val hendelse2 = TestHendelse(aktivitetslogg.barn())
        hendelse2.kontekst(person)
        val arbeidsgiver2 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 2")
        hendelse2.kontekst(arbeidsgiver2)
        val tilstand = TestKontekst("Tilstand", "Tilstand 1")
        hendelse2.kontekst(tilstand)
        hendelse2.behov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling, "Skal utbetale")
        hendelse2.info("Infomelding")

        assertEquals(2, aktivitetslogg.kontekster().size)
        assertTrue(aktivitetslogg.kontekster().first().behov().isNotEmpty())
        assertTrue(aktivitetslogg.kontekster().last().behov().isNotEmpty())
    }

    @Test
    fun `error oppdaget`() {
        val melding = "Error"
        aktivitetslogg.funksjonellFeil(melding)
        assertTrue(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertTrue(aktivitetslogg.toString().contains(melding))
        assertFunksjonellFeil(melding)
    }

    @Test
    fun `warning oppdaget`() {
        val melding = "Warning explanation"
        aktivitetslogg.varsel(melding)
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertTrue(aktivitetslogg.toString().contains(melding))
        assertVarsel(melding)
    }

    @Test
    fun `Melding sendt til forelder`(){
        val hendelse = TestHendelse(aktivitetslogg.barn())
        "info message".also {
            hendelse.info(it)
            assertInfo(it, hendelse.logg)
            assertInfo(it, aktivitetslogg)
        }
        "error message".also {
            hendelse.funksjonellFeil(it)
            assertFunksjonellFeil(it, hendelse.logg)
            assertFunksjonellFeil(it, aktivitetslogg)
        }
    }

    @Test
    fun `Melding sendt fra barnebarn til forelder`(){
        val hendelse = TestHendelse(aktivitetslogg.barn())
        hendelse.kontekst(person)
        val arbeidsgiver = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        hendelse.kontekst(arbeidsgiver)
        val vedtaksperiode = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        hendelse.kontekst(vedtaksperiode)
        "info message".also {
            hendelse.info(it)
            assertInfo(it, hendelse.logg)
            assertInfo(it, aktivitetslogg)
        }
        "error message".also {
            hendelse.funksjonellFeil(it)
            assertFunksjonellFeil(it, hendelse.logg)
            assertFunksjonellFeil(it, aktivitetslogg)
            assertFunksjonellFeil("Hendelse", aktivitetslogg)
            assertFunksjonellFeil("Vedtaksperiode", aktivitetslogg)
            assertFunksjonellFeil("Arbeidsgiver", aktivitetslogg)
            assertFunksjonellFeil("Person", aktivitetslogg)
        }
    }

    @Test
    fun `Vis bare arbeidsgiveraktivitet`(){
        val hendelse1 = TestHendelse(aktivitetslogg.barn())
        hendelse1.kontekst(person)
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        hendelse1.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        hendelse1.kontekst(vedtaksperiode1)
        hendelse1.info("info message")
        hendelse1.varsel("warn message")
        hendelse1.funksjonellFeil("error message")
        val hendelse2 = TestHendelse(aktivitetslogg.barn())
        hendelse2.kontekst(person)
        val arbeidsgiver2 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 2")
        hendelse2.kontekst(arbeidsgiver2)
        val vedtaksperiode2 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 2")
        hendelse2.kontekst(vedtaksperiode2)
        hendelse2.info("info message")
        hendelse2.funksjonellFeil("error message")
        assertEquals(5, aktivitetslogg.aktivitetsteller())
        assertEquals(3, aktivitetslogg.logg(vedtaksperiode1).aktivitetsteller())
        assertEquals(2, aktivitetslogg.logg(arbeidsgiver2).aktivitetsteller())
    }

    @Test
    fun `Behov kan ha detaljer`() {
        val hendelse1 = TestHendelse(aktivitetslogg.barn())
        hendelse1.kontekst(person)
        val param1 = "value"
        val param2 = LocalDate.now()
        hendelse1.behov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning, "Trenger godkjenning", mapOf(
            "param1" to param1,
            "param2" to param2
        ))

        assertEquals(1, aktivitetslogg.behov().size)
        assertEquals(1, aktivitetslogg.behov().first().kontekst().size)
        assertEquals(2, aktivitetslogg.behov().first().detaljer().size)
        assertEquals("Person 1", aktivitetslogg.behov().first().kontekst()["Person"])
        assertEquals(param1, aktivitetslogg.behov().first().detaljer()["param1"])
        assertEquals(param2, aktivitetslogg.behov().first().detaljer()["param2"])
    }

    private fun assertInfo(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitInfo(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Info, melding: String, tidsstempel: String) {
                visitorCalled = true
                assertEquals(message, melding)
            }
        })
        assertTrue(visitorCalled)
    }

    private fun assertVarsel(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitVarsel(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Varsel, melding: String, tidsstempel: String) {
                visitorCalled = true
                assertEquals(message, melding)
            }
        })
        assertTrue(visitorCalled)
    }

    private fun assertFunksjonellFeil(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitFunksjonellFeil(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.FunksjonellFeil, melding: String, tidsstempel: String) {
                visitorCalled = true
                assertTrue(message in aktivitet.toString(), aktivitetslogg.toString())
            }
        })
        assertTrue(visitorCalled)
    }

    private fun assertLogiskFeil(message: String, aktivitetslogg: Aktivitetslogg = this.aktivitetslogg) {
        var visitorCalled = false
        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitLogiskFeil(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.LogiskFeil, melding: String, tidsstempel: String) {
                visitorCalled = true
                assertEquals(message, melding)
            }
        })
        assertTrue(visitorCalled)
    }

    private class TestKontekst(
        private val type: String,
        private val melding: String
    ): Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(type, mapOf(type to melding))
    }

    private class TestHendelse(val logg: Aktivitetslogg): Aktivitetskontekst, IAktivitetslogg by logg {
        init {
            logg.kontekst(this)
        }
        override fun toSpesifikkKontekst() = SpesifikkKontekst("TestHendelse")
        override fun kontekst(kontekst: Aktivitetskontekst) {
            logg.kontekst(kontekst)
        }

    }
}
