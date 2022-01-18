package no.nav.helse.person

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.Arbeidsforhold
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juni
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
        val arbeidsforhold1 = arbeidsforholdhistorikk.hentArbeidsforholdhistorikkinnslagIder()

        arbeidsforholdhistorikk.lagre(arbeidsforhold.reversed(), 1.januar)
        val arbeidsforhold2 = arbeidsforholdhistorikk.hentArbeidsforholdhistorikkinnslagIder()

        assertEquals(arbeidsforhold1, arbeidsforhold2)
        assertEquals(1, arbeidsforhold1.size)
    }

    @Test
    fun `Sammeligner to arbeidsforhold korrekt`() {

        val arbeidsforholdhistorikk = Arbeidsforholdhistorikk()

        arbeidsforholdhistorikk.lagre(listOf(Arbeidsforhold(orgnummer = "a1", fom = 31.januar, tom = null)), 1.januar)
        val arbeidsforhold1 = arbeidsforholdhistorikk.hentArbeidsforholdhistorikkinnslagIder()

        arbeidsforholdhistorikk.lagre(listOf(Arbeidsforhold(orgnummer = "a1", fom = 31.januar(2010), tom = null)), 1.januar)
        val arbeidsforhold2 = arbeidsforholdhistorikk.hentArbeidsforholdhistorikkinnslagIder()

        assertNotEquals(arbeidsforhold1, arbeidsforhold2)
        assertEquals(2, arbeidsforhold2.size)
    }

    private fun Arbeidsforholdhistorikk.hentArbeidsforholdhistorikkinnslagIder(): MutableList<UUID> {
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
        val arbeidsforhold1 = arbeidsforholdhistorikk.hentArbeidsforholdhistorikkinnslagIder()

        arbeidsforholdhistorikk.lagre(arbeidsforhold.reversed(), 11.januar)
        val arbeidsforhold2 = arbeidsforholdhistorikk.hentArbeidsforholdhistorikkinnslagIder()

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

    @Test
    fun `harRelevantArbeidsforhold fungerer for eldre innslag i arbeidsforholdhistorikken`() {
        val arbeidsforhold1 = listOf(Arbeidsforhold(orgnummer = "a1", fom = 31.januar(2010), tom = null))
        val arbeidsforhold2 = listOf(Arbeidsforhold(orgnummer = "a1", fom = 31.januar(2010), tom = 30.april(2022)))

        val historikk = Arbeidsforholdhistorikk()
        historikk.lagre(arbeidsforhold1, 1.januar(2018))
        historikk.lagre(arbeidsforhold2, 1.januar(2022))

        assertTrue(historikk.harRelevantArbeidsforhold(1.januar(2018)))
    }

    @Test
    fun `tom historikk har ikke relevant arbeidsforhold for et skjæringstidspunkt`() {
        val historikk = Arbeidsforholdhistorikk()
        assertFalse(historikk.harRelevantArbeidsforhold(1.januar(2018)))
    }

    @Test
    fun `har arbeidsforholdNyereEnnTreMåneder fungerer for eldre innslag i arbeidsforholdhistorikken`() {
        val arbeidsforhold1 = listOf(Arbeidsforhold(orgnummer = "a1", fom = 1.januar(2017), tom = null))
        val arbeidsforhold2 = listOf(Arbeidsforhold(orgnummer = "a1", fom = 1.desember(2020), tom = null))

        val historikk = Arbeidsforholdhistorikk()
        historikk.lagre(arbeidsforhold1, 1.januar(2018))
        historikk.lagre(arbeidsforhold2, 1.januar(2022))

        assertFalse(historikk.harArbeidsforholdNyereEnnTreMåneder(1.januar(2018)))
    }

    @Test
    fun `duplikatsjekk er ikke avhengig av rekkefølgen på innslagene som legges inn`() {
        val arbeidsforhold1 = listOf(Arbeidsforhold(orgnummer = "a1", fom = 1.januar(2017), tom = 31.desember(2017)))
        val arbeidsforhold2 = listOf(Arbeidsforhold(orgnummer = "a1", fom = 1.januar(2022), tom = 31.desember(2022)))

        val historikk = Arbeidsforholdhistorikk()
        historikk.lagre(arbeidsforhold1, 1.juni(2017))
        historikk.lagre(arbeidsforhold2, 1.juni(2022))
        historikk.lagre(arbeidsforhold1, 1.juni(2017))

        assertEquals(2, historikk.hentArbeidsforholdhistorikkinnslagIder().size)
    }

}
