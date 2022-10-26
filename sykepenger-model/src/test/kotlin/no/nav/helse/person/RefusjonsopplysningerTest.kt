package no.nav.helse.person

import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Refusjonsopplysninger.Refusjonsopplysning
import no.nav.helse.person.Refusjonsopplysninger.Refusjonsopplysning.Companion.merge
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RefusjonsopplysningerTest {

    @Test
    fun `ny refusjonsopplysning i midten av eksisterende`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(
            Refusjonsopplysning(
                meldingsreferanseId1,
                1.januar,
                31.januar,
                2000.daglig
            )
        )
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(
            Refusjonsopplysning(
                meldingsreferanseId2,
                15.januar,
                20.januar,
                1000.daglig
            )
        )
        assertEquals(
            listOf(
                Refusjonsopplysning(meldingsreferanseId1, 1.januar, 14.januar, 2000.daglig),
                Refusjonsopplysning(meldingsreferanseId2, 15.januar, 20.januar, 1000.daglig),
                Refusjonsopplysning(meldingsreferanseId1, 21.januar, 31.januar, 2000.daglig)
            ),
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger)
        )
    }

    @Test
    fun `ny refusjonsopplysning uten tom`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId1, 1.januar, 31.januar, 2000.daglig))
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId2, 15.januar, null, 1000.daglig))

        assertEquals(
            listOf(
                Refusjonsopplysning(meldingsreferanseId1, 1.januar, 14.januar, 2000.daglig),
                Refusjonsopplysning(meldingsreferanseId2, 15.januar, null, 1000.daglig)
            ),
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger)
        )
    }

    @Test
    fun `ny refusjonsopplysning erstatter gamle`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId1, 2.januar, 30.januar, 2000.daglig))
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId2, 1.januar, 31.januar, 1000.daglig))

        assertEquals(
            nyeRefusjonsopplysninger,
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger)
        )
    }

    @Test
    fun `ny refusjonsopplysning uten tom erstatter gammel uten tom`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId1, 1.januar, null, 2000.daglig))
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId2, 1.januar, null, 1000.daglig))

        assertEquals(
            nyeRefusjonsopplysninger,
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger)
        )
    }

    @Test
    fun `ny refusjonsopplysning uten tom legges på eksisterende uten tom`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId1, 1.januar, null, 2000.daglig))
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId2, 1.mars, null, 1000.daglig))

        assertEquals(
            listOf(Refusjonsopplysning(meldingsreferanseId1, 1.januar, 28.februar, 2000.daglig), Refusjonsopplysning(meldingsreferanseId2, 1.mars, null, 1000.daglig)),
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger)
        )
    }

    @Test
    fun `ny refusjonsopplysning uten tom som starter tidligere enn forrige`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId1, 1.mars, null, 2000.daglig))
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId2, 1.januar, null, 1000.daglig))

        assertEquals(
            nyeRefusjonsopplysninger,
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger)
        )
    }

    @Test
    fun `perfekt overlapp - bruker nye opplysninger`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId1, 1.januar, 1.mars, 2000.daglig))
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId2, 1.januar, 1.mars, 1000.daglig))

        assertEquals(
            nyeRefusjonsopplysninger,
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger)
        )
    }

    @Test
    fun `nye opplysninger erstatter deler av eksisterende opplysninger`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val meldingsreferanseId2 = UUID.randomUUID()
        val meldingsreferanseId3 = UUID.randomUUID()
        val meldingsreferanseId4 = UUID.randomUUID()
        val meldingsreferanseId5 = UUID.randomUUID()
        val meldingsreferanseId6 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(
            Refusjonsopplysning(meldingsreferanseId1, 1.januar, 5.januar, 2000.daglig),
            Refusjonsopplysning(meldingsreferanseId2, 6.januar, 11.januar, 3000.daglig),
            Refusjonsopplysning(meldingsreferanseId3, 12.januar, 17.januar, 4000.daglig),
        )
        val nyeRefusjonsopplysninger = listOf(
            Refusjonsopplysning(meldingsreferanseId4, 2.januar, 4.januar, 5000.daglig),
            Refusjonsopplysning(meldingsreferanseId5, 7.januar, 10.januar, 6000.daglig),
            Refusjonsopplysning(meldingsreferanseId6, 13.januar, 16.januar, 7000.daglig),
        )

        assertEquals(
            listOf(
                Refusjonsopplysning(meldingsreferanseId1, 1.januar, 1.januar, 2000.daglig),
                Refusjonsopplysning(meldingsreferanseId4, 2.januar, 4.januar, 5000.daglig),
                Refusjonsopplysning(meldingsreferanseId1, 5.januar, 5.januar, 2000.daglig),
                Refusjonsopplysning(meldingsreferanseId2, 6.januar, 6.januar, 3000.daglig),
                Refusjonsopplysning(meldingsreferanseId5, 7.januar, 10.januar, 6000.daglig),
                Refusjonsopplysning(meldingsreferanseId2, 11.januar, 11.januar, 3000.daglig),
                Refusjonsopplysning(meldingsreferanseId3, 12.januar, 12.januar, 4000.daglig),
                Refusjonsopplysning(meldingsreferanseId6, 13.januar, 16.januar, 7000.daglig),
                Refusjonsopplysning(meldingsreferanseId3, 17.januar, 17.januar, 4000.daglig)

            ),
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger)
        )
    }

    @Test
    fun `to tomme lister`() {
        assertEquals(emptyList<Refusjonsopplysning>(), emptyList<Refusjonsopplysning>().merge(emptyList()))
    }
}