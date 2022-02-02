package no.nav.helse.person

import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class ArbeidsforholdhistorikkTest {

    @Test
    fun `Lagrer ikke duplikat av arbeidsforhold`() {
        val arbeidsforhold = listOf(
            Arbeidsforhold(ansattFom = 31.januar(2010), ansattTom = null, deaktivert = false),
            Arbeidsforhold(ansattFom = 31.januar, ansattTom = null, deaktivert = false)
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

        arbeidsforholdhistorikk.lagre(listOf(Arbeidsforhold(ansattFom = 31.januar, ansattTom = null, deaktivert = false)), 1.januar)
        val arbeidsforhold1 = arbeidsforholdhistorikk.hentArbeidsforholdhistorikkinnslagIder()

        arbeidsforholdhistorikk.lagre(listOf(Arbeidsforhold(ansattFom = 31.januar(2010), ansattTom = null, deaktivert = false)), 1.januar)
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
            Arbeidsforhold(ansattFom = 31.januar(2010), ansattTom = null, deaktivert = false),
            Arbeidsforhold(ansattFom = 31.januar, ansattTom = null, deaktivert = false)
        )

        val arbeidsforholdhistorikk = Arbeidsforholdhistorikk()

        arbeidsforholdhistorikk.lagre(arbeidsforhold, 1.januar)
        val arbeidsforhold1 = arbeidsforholdhistorikk.hentArbeidsforholdhistorikkinnslagIder()

        arbeidsforholdhistorikk.lagre(arbeidsforhold.reversed(), 11.januar)
        val arbeidsforhold2 = arbeidsforholdhistorikk.hentArbeidsforholdhistorikkinnslagIder()

        assertNotEquals(arbeidsforhold1, arbeidsforhold2)
        assertEquals(2, arbeidsforhold2.size)
    }

    @Test
    fun `skal kunne markere et arbeidsforhold som inaktivt for et skjæringstidspunkt`() {
        val arbeidsforhold = listOf(Arbeidsforhold(ansattFom = 31.januar(2010), ansattTom = null, deaktivert = false))
        val historikk = Arbeidsforholdhistorikk()
        historikk.lagre(arbeidsforhold, 1.januar)
        assertFalse(historikk.harDeaktivertArbeidsforhold(1.januar))
        assertTrue(historikk.harRelevantArbeidsforhold(1.januar))
        historikk.deaktiverArbeidsforhold(1.januar)
        assertTrue(historikk.harDeaktivertArbeidsforhold(1.januar))
        assertFalse(historikk.harRelevantArbeidsforhold(1.januar))
    }

    @Test
    fun `skal kunne markere et arbeidsforhold som aktivt for et skjæringstidspunkt`() {
        val arbeidsforhold = listOf(Arbeidsforhold(ansattFom = 31.januar(2010), ansattTom = null, deaktivert = false))
        val historikk = Arbeidsforholdhistorikk()
        historikk.lagre(arbeidsforhold, 1.januar)
        historikk.deaktiverArbeidsforhold(1.januar)
        historikk.aktiverArbeidsforhold(1.januar)
        assertFalse(historikk.harDeaktivertArbeidsforhold(1.januar))
        assertTrue(historikk.harRelevantArbeidsforhold(1.januar))
    }

    @Test
    fun `duplikatsjekk skal fange opp forsøk på deaktivering av inaktivt arbeidsforhold`() {
        val arbeidsforhold = listOf(Arbeidsforhold(ansattFom = 31.januar(2010), ansattTom = null, deaktivert = false))
        val historikk = Arbeidsforholdhistorikk()
        historikk.lagre(arbeidsforhold, 1.januar)
        historikk.deaktiverArbeidsforhold(1.januar)
        val historikkinnslag = historikk.hentArbeidsforholdhistorikkinnslagIder()
        historikk.deaktiverArbeidsforhold(1.januar)
        assertEquals(historikkinnslag, historikk.hentArbeidsforholdhistorikkinnslagIder())
    }

    @Test
    fun `duplikatsjekk skal fange opp forsøk på aktivering av aktivt arbeidsforhold`() {
        val arbeidsforhold = listOf(Arbeidsforhold(ansattFom = 31.januar(2010), ansattTom = null, deaktivert = false))
        val historikk = Arbeidsforholdhistorikk()
        historikk.lagre(arbeidsforhold, 1.januar)
        val historikkinnslag = historikk.hentArbeidsforholdhistorikkinnslagIder()
        historikk.aktiverArbeidsforhold(1.januar)
        assertEquals(historikkinnslag, historikk.hentArbeidsforholdhistorikkinnslagIder())
    }

    @Test
    fun `harRelevantArbeidsforhold fungerer for eldre innslag i arbeidsforholdhistorikken`() {
        val arbeidsforhold1 = listOf(Arbeidsforhold(ansattFom = 31.januar(2010), ansattTom = null, deaktivert = false))
        val arbeidsforhold2 = listOf(Arbeidsforhold(ansattFom = 31.januar(2010), ansattTom = 30.april(2022), deaktivert = false))

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
        val arbeidsforhold1 = listOf(Arbeidsforhold(ansattFom = 1.januar(2017), ansattTom = null, deaktivert = false))
        val arbeidsforhold2 = listOf(Arbeidsforhold(ansattFom = 1.desember(2020), ansattTom = null, deaktivert = false))

        val historikk = Arbeidsforholdhistorikk()
        historikk.lagre(arbeidsforhold1, 1.januar(2018))
        historikk.lagre(arbeidsforhold2, 1.januar(2022))

        assertFalse(historikk.harArbeidsforholdNyereEnnTreMåneder(1.januar(2018)))
    }

    @Test
    fun `duplikatsjekk er ikke avhengig av rekkefølgen på innslagene som legges inn`() {
        val arbeidsforhold1 = listOf(Arbeidsforhold(ansattFom = 1.januar(2017), ansattTom = 31.desember(2017), deaktivert = false))
        val arbeidsforhold2 = listOf(Arbeidsforhold(ansattFom = 1.januar(2022), ansattTom = 31.desember(2022), deaktivert = false))

        val historikk = Arbeidsforholdhistorikk()
        historikk.lagre(arbeidsforhold1, 1.juni(2017))
        historikk.lagre(arbeidsforhold2, 1.juni(2022))
        historikk.lagre(arbeidsforhold1, 1.juni(2017))

        assertEquals(2, historikk.hentArbeidsforholdhistorikkinnslagIder().size)
    }

}
