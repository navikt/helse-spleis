package no.nav.helse.person

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.Arbeidsforhold
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class ArbeidsforholdhistorikkTest {

    @Test
    fun `Lagrer ikke duplikat av arbeidsforhold`() {
        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = "a1", fom = 31.januar(2010), tom = null),
            Arbeidsforhold(orgnummer = "a1", fom = 31.januar, tom = null)
        )

        val arbeidsforholdhistorikk = Arbeidsforholdhistorikk()

        arbeidsforholdhistorikk.lagre(arbeidsforhold, 1.januar)
        val arbeidsforhold1 = arbeidsforholdhistorikk.tellArbeidsforholdhistorikkinnslag()

        arbeidsforholdhistorikk.lagre(arbeidsforhold.reversed(), 1.januar)
        val arbeidsforhold2 = arbeidsforholdhistorikk.tellArbeidsforholdhistorikkinnslag()

        assertEquals(arbeidsforhold1, arbeidsforhold2)
        assertEquals(1, arbeidsforhold1.size)
    }

    @Test
    fun `Sammeligner to arbeidsforhold korrekt`() {

        val arbeidsforholdhistorikk = Arbeidsforholdhistorikk()

        arbeidsforholdhistorikk.lagre(listOf(Arbeidsforhold(orgnummer = "a1", fom = 31.januar, tom = null)), 1.januar)
        val arbeidsforhold1 = arbeidsforholdhistorikk.tellArbeidsforholdhistorikkinnslag()

        arbeidsforholdhistorikk.lagre(listOf(Arbeidsforhold(orgnummer = "a1", fom = 31.januar(2010), tom = null)), 1.januar)
        val arbeidsforhold2 = arbeidsforholdhistorikk.tellArbeidsforholdhistorikkinnslag()

        assertNotEquals(arbeidsforhold1, arbeidsforhold2)
        assertEquals(2, arbeidsforhold2.size)
    }

    private fun Arbeidsforholdhistorikk.tellArbeidsforholdhistorikkinnslag(): MutableList<UUID> {
        val arbeidsforholdIder = mutableListOf<UUID>()
        accept(object : ArbeidsforholdhistorikkVisitor {

            override fun preVisitArbeidsforholdinnslag(arbeidsforholdinnslag: Arbeidsforholdhistorikk.Innslag, id: UUID, skjæringstidspunkt: LocalDate) {
                arbeidsforholdIder.add(id)
            }
        })

        return arbeidsforholdIder
    }

    @Test
    fun `To like arbeidsforhold hentes for to forskjellig skjæringstidspunkt, skal lage to historikkinnslag`() {
        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = "a1", fom = 31.januar(2010), tom = null),
            Arbeidsforhold(orgnummer = "a1", fom = 31.januar, tom = null)
        )

        val arbeidsforholdhistorikk = Arbeidsforholdhistorikk()

        arbeidsforholdhistorikk.lagre(arbeidsforhold, 1.januar)
        val arbeidsforhold1 = arbeidsforholdhistorikk.tellArbeidsforholdhistorikkinnslag()

        arbeidsforholdhistorikk.lagre(arbeidsforhold.reversed(), 11.januar)
        val arbeidsforhold2 = arbeidsforholdhistorikk.tellArbeidsforholdhistorikkinnslag()

        assertNotEquals(arbeidsforhold1, arbeidsforhold2)
        assertEquals(2, arbeidsforhold2.size)
    }

    @ForventetFeil("parprogrammering er ping-pong-aktivitet :)")
    @Test
    fun `skal kunne markere et arbeidsforhold som ikke relevant for et skjæringstidspunkt`() {
        val arbeidsforhold = listOf(Arbeidsforhold(orgnummer = "a1", fom = 31.januar(2010), tom = null))
        val historikk = Arbeidsforholdhistorikk()
        historikk.lagre(arbeidsforhold, 1.januar)
        assertTrue(historikk.harRelevantArbeidsforhold(1.januar))
        historikk.gjørArbeidsforholdInaktivt(1.januar)
        assertTrue(historikk.harInaktivtArbeidsforhold(1.januar))
        assertFalse(historikk.harRelevantArbeidsforhold(1.januar))
    }

}
