package no.nav.helse

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.etterlevelse.BehandlingSubsumsjonslogg
import no.nav.helse.etterlevelse.KontekstType
import no.nav.helse.etterlevelse.Subsumsjonskontekst
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.Subsumsjonslogg.Companion.EmptyLog
import no.nav.helse.etterlevelse.`§ 8-3 ledd 2 punktum 1`
import no.nav.helse.etterlevelse.`§ 8-51 ledd 2`
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class GrunnbeløpTest {

    private val jurist = BehandlingSubsumsjonslogg(
        EmptyLog, listOf(
        Subsumsjonskontekst(KontekstType.Fødselsnummer, "fnr"),
        Subsumsjonskontekst(KontekstType.Organisasjonsnummer, "orgnr"),
        Subsumsjonskontekst(KontekstType.Vedtaksperiode, "${UUID.randomUUID()}"),
    )
    )

    @Test
    fun dagsats() {
        assertEquals(2304.daglig, Grunnbeløp.`6G`.dagsats(1.mai(2019)))
        assertEquals(2236.daglig, Grunnbeløp.`6G`.dagsats(30.april(2019)))
    }

    @Test
    fun grunnbeløp() {
        assertEquals(599148.årlig, Grunnbeløp.`6G`.beløp(1.mai(2019)))
        assertEquals(581298.årlig, Grunnbeløp.`6G`.beløp(30.april(2019)))
    }

    @Test
    fun snitt() {
        assertEquals(116239.årlig, Grunnbeløp.`1G`.snitt(2023))
        assertEquals(109784.årlig, Grunnbeløp.`1G`.snitt(2022))
        assertEquals(104716.årlig, Grunnbeløp.`1G`.snitt(2021))
        assertEquals(33575.årlig, Grunnbeløp.`1G`.snitt(1990))
    }

    @Test
    fun `grunnbeløp før virkingdato`() {
        assertEquals(99858.årlig, Grunnbeløp.`1G`.beløp(1.mai(2020)))
        assertEquals(99858.årlig, Grunnbeløp.`1G`.beløp(30.april(2020), 20.september(2020)))
        assertEquals(99858.årlig, Grunnbeløp.`1G`.beløp(1.mai(2020), 20.september(2020)))
    }

    @Test
    fun `grunnbeløp etter virkingdato`() {
        assertEquals(99858.årlig, Grunnbeløp.`1G`.beløp(30.april(2020), 21.september(2020)))
        assertEquals(101351.årlig, Grunnbeløp.`1G`.beløp(1.mai(2020), 21.september(2020)))
        assertEquals(101351.årlig, Grunnbeløp.`1G`.beløp(21.september(2020)))
    }

    @Test
    fun `skjæringstidspunkt brukes når virkningsdato er eldre`() {
        assertEquals(101351.årlig, Grunnbeløp.`1G`.beløp(10.oktober(2020), 21.september(2020)))
    }


    @Test
    fun `virkningstidspunktet for regulering av kravet til minsteinntekt`() {
        val halvG2018 = Grunnbeløp.halvG.beløp(30.april(2019))
        val halvG2019 = Grunnbeløp.halvG.beløp(1.mai(2019))
        val `2G2018` = Grunnbeløp.`2G`.beløp(30.april(2019))
        val `2G2019` = Grunnbeløp.`2G`.beløp(1.mai(2019))

        // person er under 67 ved skjæringstidspunkt
        assertMinsteinntektOk(
            skjæringstidspunkt = 26.mai(2019),
            inntekt = halvG2018,
            alder = under67(),
        )
        assertMinimumInntektAvslag(
            skjæringstidspunkt = 27.mai(2019),
            inntekt = halvG2018,
            alder = under67(),
        )
        assertMinsteinntektOk(
            skjæringstidspunkt = 27.mai(2019),
            inntekt = halvG2019,
            alder = under67(),
        )

        // 67-åring blir behandlet som under 67 år
        assertMinsteinntektOk(
            skjæringstidspunkt = 26.mai(2019),
            inntekt = halvG2018,
            alder = akkurat67(26.mai(2019)),
        )
        assertMinimumInntektAvslag(
            skjæringstidspunkt = 27.mai(2019),
            inntekt = halvG2018,
            alder = akkurat67(27.mai(2019)),
        )
        assertMinsteinntektOk(
            skjæringstidspunkt = 27.mai(2019),
            inntekt = halvG2019,
            alder = akkurat67(27.mai(2019)),
        )


        // person er over 67 ved skjæringstidspunkt
        assertMinsteinntektOk(
            skjæringstidspunkt = 26.mai(2019),
            inntekt = `2G2018`,
            alder = over67(30.april(2019)),
        )
        assertMinimumInntektOver67Avslag(
            skjæringstidspunkt = 27.mai(2019),
            inntekt = `2G2018`,
            alder = over67(27.mai(2019)),
        )
        assertMinsteinntektOk(
            skjæringstidspunkt = 27.mai(2019),
            inntekt = `2G2019`,
            alder = over67(30.april(2019)),
        )
    }

    @Test
    fun `virkningstidspunkt for Grunnbeløp`() {
        val beløp = Grunnbeløp.`1G`.beløp(1.mai(2019))
        val virkningstidspunkt = Grunnbeløp.virkningstidspunktFor(beløp)
        assertEquals(1.mai(2019), virkningstidspunkt)
    }

    @Test
    fun `virkningstidspunkt før ny G sitt virkningstidspunkt`() {
        val beløp = Grunnbeløp.`1G`.beløp(30.april(2020))
        val virkningstidspunkt = Grunnbeløp.virkningstidspunktFor(beløp)
        assertEquals(1.mai(2019), virkningstidspunkt)
    }


    @Test
    fun `virkningstidspunkt etter ny G sitt virkningstidspunkt`() {
        val beløp = Grunnbeløp.`1G`.beløp(2.mai(2021))
        val virkningstidspunkt = Grunnbeløp.virkningstidspunktFor(beløp)
        assertEquals(1.mai(2021), virkningstidspunkt)
    }

    @Test
    fun `kaster exception for beløp som ikke er gyldig Grunnbeløp`() {
        val ikkeGyldigGrunnbeløp = 123123.årlig
        assertThrows<IllegalArgumentException> { Grunnbeløp.virkningstidspunktFor(ikkeGyldigGrunnbeløp) }
    }

    private fun assertMinsteinntektOk(
        skjæringstidspunkt: LocalDate,
        inntekt: Inntekt,
        alder: Alder
    ) = assertNull(
        Grunnbeløp.validerMinsteInntekt(
            skjæringstidspunkt = skjæringstidspunkt,
            inntekt = inntekt,
            alder = alder,
            subsumsjonslogg = jurist
        )
    )

    private fun assertMinimumInntektOver67Avslag(
        skjæringstidspunkt: LocalDate,
        inntekt: Inntekt,
        alder: Alder
    ) = assertEquals(
        Begrunnelse.MinimumInntektOver67,
        Grunnbeløp.validerMinsteInntekt(
            skjæringstidspunkt = skjæringstidspunkt,
            inntekt = inntekt,
            alder = alder,
            subsumsjonslogg = jurist
        )
    )

    private fun assertMinimumInntektAvslag(
        skjæringstidspunkt: LocalDate,
        inntekt: Inntekt,
        alder: Alder
    ) = assertEquals(
        Begrunnelse.MinimumInntekt, Grunnbeløp.validerMinsteInntekt(
        skjæringstidspunkt = skjæringstidspunkt,
        inntekt = inntekt,
        alder = alder,
        subsumsjonslogg = jurist
    )
    )

    private fun under67() = LocalDate.now().minusYears(66).alder
    private fun over67(skjæringstidspunkt: LocalDate) = skjæringstidspunkt.minusYears(67).minusDays(1).alder
    private fun akkurat67(skjæringstidspunkt: LocalDate) = skjæringstidspunkt.minusYears(67).alder

    private fun Grunnbeløp.oppfyllerMinsteInntekt(dato: LocalDate, inntekt: Inntekt) =
        inntekt >= minsteinntekt(dato)

    private fun Grunnbeløp.Companion.validerMinsteInntekt(skjæringstidspunkt: LocalDate, inntekt: Inntekt, alder: Alder, subsumsjonslogg: Subsumsjonslogg): Begrunnelse? {
        val gjeldendeGrense = if (alder.forhøyetInntektskrav(skjæringstidspunkt)) `2G` else halvG
        val oppfylt = gjeldendeGrense.oppfyllerMinsteInntekt(skjæringstidspunkt, inntekt)

        if (alder.forhøyetInntektskrav(skjæringstidspunkt)) {
            subsumsjonslogg.logg(
                `§ 8-51 ledd 2`(
                    oppfylt = oppfylt,
                    skjæringstidspunkt = skjæringstidspunkt,
                    alderPåSkjæringstidspunkt = alder.alderPåDato(skjæringstidspunkt),
                    beregningsgrunnlagÅrlig = inntekt.årlig,
                    minimumInntektÅrlig = gjeldendeGrense.minsteinntekt(skjæringstidspunkt).årlig
                )
            )
            if (oppfylt) return null
            return Begrunnelse.MinimumInntektOver67
        }
        subsumsjonslogg.logg(
            `§ 8-3 ledd 2 punktum 1`(
                oppfylt = oppfylt,
                skjæringstidspunkt = skjæringstidspunkt,
                beregningsgrunnlagÅrlig = inntekt.årlig,
                minimumInntektÅrlig = gjeldendeGrense.minsteinntekt(skjæringstidspunkt).årlig
            )
        )
        if (oppfylt) return null
        return Begrunnelse.MinimumInntekt
    }
}
