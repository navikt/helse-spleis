package no.nav.helse.feriepenger

import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.feriepenger.Feriepengegrunnlagsdag.Kilde
import no.nav.helse.feriepenger.Feriepengegrunnlagsdag.Mottaker
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FeriepengegrunnlagstidslinjeTest {

    @Test
    fun `filtering av kilde`() {
        val tidslinje = Feriepengegrunnlagstidslinje.Builder().apply {
            leggTilUtbetaling(1.januar, a1, Mottaker.ARBEIDSGIVER, Kilde.INFOTRYGD, 1234)
            leggTilUtbetaling(1.januar, a1, Mottaker.PERSON, Kilde.INFOTRYGD, 766)

            leggTilUtbetaling(1.januar, a1, Mottaker.ARBEIDSGIVER, Kilde.SPLEIS, 983)
            leggTilUtbetaling(1.januar, a1, Mottaker.PERSON, Kilde.SPLEIS, 123)

            leggTilUtbetaling(2.januar, a1, Mottaker.ARBEIDSGIVER, Kilde.INFOTRYGD, 1234)
        }.build()

        tidslinje.kun(Kilde.INFOTRYGD).also { filtrert ->
            val grunnlag = filtrert.grunnlagFor(a1, 1.0)
            assertEquals(2, grunnlag.dager.size)
            assertEquals(2468, grunnlag.refusjonsresultat.feriepengegrunnlag)
            assertEquals(766, grunnlag.personresultat.feriepengegrunnlag)
        }

        tidslinje.kun(Kilde.SPLEIS).also { filtrert ->
            val grunnlag = filtrert.grunnlagFor(a1, 1.0)
            assertEquals(1, grunnlag.dager.size)
            assertEquals(983, grunnlag.refusjonsresultat.feriepengegrunnlag)
            assertEquals(123, grunnlag.personresultat.feriepengegrunnlag)
        }
    }

    @Test
    fun `filtering av orgnr`() {
        val tidslinje = Feriepengegrunnlagstidslinje.Builder().apply {
            leggTilUtbetaling(1.januar, a1, Mottaker.ARBEIDSGIVER, Kilde.INFOTRYGD, 1234)
            leggTilUtbetaling(1.januar, a2, Mottaker.PERSON, Kilde.INFOTRYGD, 766)

            leggTilUtbetaling(1.januar, a1, Mottaker.ARBEIDSGIVER, Kilde.SPLEIS, 983)
            leggTilUtbetaling(1.januar, a1, Mottaker.PERSON, Kilde.SPLEIS, 123)

            leggTilUtbetaling(2.januar, a2, Mottaker.ARBEIDSGIVER, Kilde.INFOTRYGD, 1234)
        }.build()

        tidslinje.grunnlagFor(a1, 1.0).also { grunnlag ->
            assertEquals(1, grunnlag.dager.size)
            assertEquals(2217, grunnlag.refusjonsresultat.feriepengegrunnlag)
            assertEquals(123, grunnlag.personresultat.feriepengegrunnlag)
        }

        tidslinje.grunnlagFor(a2, 1.0).also { grunnlag ->
            assertEquals(2, grunnlag.dager.size)
            assertEquals(1234, grunnlag.refusjonsresultat.feriepengegrunnlag)
            assertEquals(766, grunnlag.personresultat.feriepengegrunnlag)
        }
    }

    @Test
    fun `tidslinjen kan kun bestå av en unik dato`() {
        assertThrows<IllegalArgumentException> {
            Feriepengegrunnlagstidslinje(
                dager = listOf(
                    Feriepengegrunnlagsdag(1.januar, emptyList()),
                    Feriepengegrunnlagsdag(1.januar, emptyList())
                )
            )
        }
    }

    @Test
    fun `legger sammen tidslinjer basert på dato`() {
        val tidslinje1 = Feriepengegrunnlagstidslinje.Builder().apply {
            leggTilUtbetaling(1.januar, a1, Mottaker.ARBEIDSGIVER, Kilde.INFOTRYGD, 1234)
            leggTilUtbetaling(1.januar, a1, Mottaker.PERSON, Kilde.INFOTRYGD, 766)
            leggTilUtbetaling(2.januar, a1, Mottaker.PERSON, Kilde.INFOTRYGD, 766)
        }.build()

        val tidslinje2 = Feriepengegrunnlagstidslinje.Builder().apply {
            leggTilUtbetaling(1.januar, a1, Mottaker.ARBEIDSGIVER, Kilde.SPLEIS, 2000)
            leggTilUtbetaling(2.januar, a1, Mottaker.ARBEIDSGIVER, Kilde.SPLEIS, 2000)
            leggTilUtbetaling(3.januar, a1, Mottaker.ARBEIDSGIVER, Kilde.SPLEIS, 2000)
        }.build()

        val resultat = tidslinje1 + tidslinje2

        resultat.grunnlagFor(a1, 1.0).also { grunnlag ->
            assertEquals(3, grunnlag.dager.size)
            assertEquals(7234, grunnlag.refusjonsresultat.feriepengegrunnlag)
            assertEquals(1532, grunnlag.personresultat.feriepengegrunnlag)
        }
    }
}
