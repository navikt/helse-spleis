package no.nav.helse.person

import no.nav.helse.hendelser.Arbeidsforhold
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class ArbeidsforholdhistorikkTest {

    @Test
    fun `Lagrer ikke duplikat av arbeidsforhold`() {
        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = "a1", fom = 31.januar(2010), tom = null),
            Arbeidsforhold(orgnummer = "a1", fom = 31.januar, tom = null)
        )

        val arbeidsforholdhistorikk = Arbeidsforholdhistorikk()

        arbeidsforholdhistorikk.lagre(arbeidsforhold)
        val arbeidsforhold1 = arbeidsforholdhistorikk.tellArbeidsforholdhistorikkinnslag()

        arbeidsforholdhistorikk.lagre(arbeidsforhold.reversed())
        val arbeidsforhold2 = arbeidsforholdhistorikk.tellArbeidsforholdhistorikkinnslag()

        assertEquals(arbeidsforhold1, arbeidsforhold2)
        assertEquals(1, arbeidsforhold1.size)
    }

    @Test
    fun `Sammeligner to arbeidsforhold korrekt`() {

        val arbeidsforholdhistorikk = Arbeidsforholdhistorikk()

        arbeidsforholdhistorikk.lagre(listOf(Arbeidsforhold(orgnummer = "a1", fom = 31.januar, tom = null)))
        val arbeidsforhold1 = arbeidsforholdhistorikk.tellArbeidsforholdhistorikkinnslag()

        arbeidsforholdhistorikk.lagre(listOf(Arbeidsforhold(orgnummer = "a1", fom = 31.januar(2010), tom = null)))
        val arbeidsforhold2 = arbeidsforholdhistorikk.tellArbeidsforholdhistorikkinnslag()

        assertNotEquals(arbeidsforhold1, arbeidsforhold2)
        assertEquals(2, arbeidsforhold2.size)
    }

    private fun Arbeidsforholdhistorikk.tellArbeidsforholdhistorikkinnslag(): MutableList<UUID> {
        val arbeidsforholdIder = mutableListOf<UUID>()
        accept(object : ArbeidsforholdhistorikkVisitor {

            override fun preVisitArbeidsforholdinnslag(arbeidsforholdinnslag: Arbeidsforholdhistorikk.Innslag, id: UUID) {
                arbeidsforholdIder.add(id)
            }
        })

        return arbeidsforholdIder
    }
}
