package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.NyeRefusjonsopplysninger
import no.nav.helse.person.Refusjonsopplysning
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OverstyrRefusjonTest {


    @Test
    fun `overstyrer refusjon`() {
        val refusjonsopplysning1 = Refusjonsopplysning(UUID.randomUUID(), 1.januar, null, Inntekt.INGEN)
        val refusjonsopplysning2 = Refusjonsopplysning(UUID.randomUUID(), 2.januar, null, Inntekt.INGEN)

        val ag1Refusjonsopplysninger = Refusjonsopplysning.Refusjonsopplysninger.RefusjonsopplysningerBuilder()
            .leggTil(refusjonsopplysning1, LocalDateTime.now()).build()
        val ag2Refusjonsopplysninger = Refusjonsopplysning.Refusjonsopplysninger.RefusjonsopplysningerBuilder()
            .leggTil(refusjonsopplysning2, LocalDateTime.now()).build()

        val refusjonsopplysninger = mapOf(
            "ag1" to ag1Refusjonsopplysninger,
            "ag2" to ag2Refusjonsopplysninger
        )
        val overstyrRefusjon = OverstyrRefusjon(
            UUID.randomUUID(),
            "",
            "",
            Aktivitetslogg(),
            refusjonsopplysninger,
            1.januar
        )

        val inntektsopplysning1 =
            Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), Inntekt.INGEN)
        val inntektsopplysning2 =
            Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 2.januar, UUID.randomUUID(), Inntekt.INGEN)
        val nyeRefusjonsopplysningerBuilder = NyeRefusjonsopplysninger(
            listOf(
                ArbeidsgiverInntektsopplysning(
                    "ag1",
                    inntektsopplysning1,
                    Refusjonsopplysning.Refusjonsopplysninger()
                ),
                ArbeidsgiverInntektsopplysning(
                    "ag2",
                    inntektsopplysning2,
                    Refusjonsopplysning.Refusjonsopplysninger()
                )
            )
        )

        overstyrRefusjon.overstyr(nyeRefusjonsopplysningerBuilder)

        val expected = listOf(
            ArbeidsgiverInntektsopplysning(
                "ag1",
                inntektsopplysning1,
                ag1Refusjonsopplysninger
            ),
            ArbeidsgiverInntektsopplysning(
                "ag2",
                inntektsopplysning2,
                ag2Refusjonsopplysninger
            )
        )

        assertEquals(expected, nyeRefusjonsopplysningerBuilder.resultat())
    }



}