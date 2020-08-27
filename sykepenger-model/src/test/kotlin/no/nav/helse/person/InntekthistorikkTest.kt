package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Inntekthistorikk.Inntektsendring
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class InntekthistorikkTest {

    private lateinit var historikk: Inntekthistorikk
    private val inspektør get() = Inntektsinspektør(historikk)

    private companion object {
        const val UNG_PERSON_FNR_2018 = "12020052345"
        const val AKTØRID = "42"
        const val ORGNUMMER = "987654321"
        val INNTEKT = 31000.00.månedlig
    }

    @BeforeEach
    fun setup() {
        historikk = Inntekthistorikk()
    }

    @Test
    fun `Inntekt fra inntektsmelding blir lagt til i inntektshistorikk`() {
        inntektsmelding().addInntekt(historikk)
        assertEquals(1, inspektør.inntektTeller)
    }


    private class Inntektsinspektør(historikk: Inntekthistorikk) : InntekthistorikkVisitor {
        var inntektTeller = 0

        init {
            historikk.accept(this)
        }

        override fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {
            inntektTeller = 0
        }

        override fun visitInntekt(inntektsendring: Inntektsendring, id: UUID) {
            inntektTeller += 1
        }

    }

    private fun inntektsmelding(
        id: UUID = UUID.randomUUID(),
        arbeidsgiverperioder: List<Periode> = listOf(1.januar til 16.januar),
        ferieperioder: List<Periode> = emptyList(),
        beregnetInntekt: Inntekt = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjon: Triple<LocalDate?, Inntekt, List<LocalDate>> = Triple(
            null,
            INNTEKT, emptyList()
        ),
        orgnummer: String = ORGNUMMER
    ): Inntektsmelding {
        return Inntektsmelding(
            meldingsreferanseId = id,
            refusjon = Inntektsmelding.Refusjon(refusjon.first, refusjon.second, refusjon.third),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = ferieperioder,
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )
    }
}

