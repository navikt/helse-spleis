package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.*
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.Inntektsinspektør
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class InfotrygdInntektsopplysningTest {

    private companion object {
        private const val ORGNR = "123456789"
        private val INNTEKT = 31000.00.månedlig
        private val DATO = 1.januar
        private val PERIODE = Periode(1.februar, 28.februar)
    }

    private lateinit var historikk: Inntektshistorikk
    private lateinit var aktivitetslogg: Aktivitetslogg
    private val inspektør get() = Inntektsinspektør(historikk)

    @BeforeEach
    fun setup() {
        historikk = Inntektshistorikk()
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun likhet() {
        assertEquals(inntektsopplysning(null).hashCode(), inntektsopplysning(null).hashCode())
        assertEquals(inntektsopplysning(1.januar).hashCode(), inntektsopplysning(1.januar).hashCode())
        assertNotEquals(inntektsopplysning().hashCode(), inntektsopplysning(1.januar).hashCode())
        assertNotEquals(inntektsopplysning(1.januar).hashCode(), inntektsopplysning(2.januar).hashCode())
        assertNotEquals(Inntektsopplysning(ORGNR, DATO, 1000.månedlig, true, null).hashCode(), Inntektsopplysning(ORGNR, DATO, 1000.månedlig, false, null).hashCode())
        assertNotEquals(Inntektsopplysning(ORGNR, DATO, 1000.månedlig, true, null).hashCode(), Inntektsopplysning(ORGNR, DATO, 2000.månedlig, true, null).hashCode())
        assertNotEquals(Inntektsopplysning(ORGNR, 1.januar, 1000.månedlig, true, null).hashCode(), Inntektsopplysning(ORGNR, 2.januar, 2100.månedlig, true, null).hashCode())
        assertNotEquals(Inntektsopplysning("ag1", DATO, 1000.månedlig, true, null).hashCode(), Inntektsopplysning("ag2", DATO, 2100.månedlig, true, null).hashCode())
        assertEquals(Inntektsopplysning("ag1", DATO, 123.6667.månedlig, true, null).hashCode(), Inntektsopplysning("ag1", DATO, 123.6667.månedlig, true, null).hashCode())
    }

    @Test
    fun `refusjon opphører før perioden`() {
        inntektsopplysning(1.januar).valider(aktivitetslogg, DATO, Nødnummer.Sykepenger)
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `refusjon opphører i perioden`() {
        inntektsopplysning(15.februar).valider(aktivitetslogg, DATO, Nødnummer.Sykepenger)
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `refusjon opphører etter perioden`() {
        inntektsopplysning(1.mars).valider(aktivitetslogg, DATO, Nødnummer.Sykepenger)
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `Inntekt fra infotrygd brukes til å beregne sykepengegrunnlaget`() {
        Inntektsopplysning.lagreInntekter(
            listOf(Inntektsopplysning(ORGNR, 1.januar, INNTEKT, true)),
            historikk,
            UUID.randomUUID()
        )
        assertEquals(1, inspektør.inntektTeller.size)
        assertEquals(1, inspektør.inntektTeller.first())
        assertEquals(INNTEKT, historikk.grunnlagForSykepengegrunnlag(1.januar, 1.januar)?.grunnlagForSykepengegrunnlag())
    }

    @Test
    fun `Bruker inntekt fra infotrygd fremfor inntekt fra inntektsmelding for å beregne sykepengegrunnlaget`() {
        inntektsmelding(beregnetInntekt = 20000.månedlig).addInntekt(historikk, 1.januar, MaskinellJurist())
        Inntektsopplysning.lagreInntekter(
            listOf(Inntektsopplysning(ORGNR, 1.januar, 25000.månedlig, true)),
            historikk,
            UUID.randomUUID()
        )
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(2, inspektør.inntektTeller.first())
        assertEquals(1, inspektør.inntektTeller.last())
        assertEquals(25000.månedlig, historikk.grunnlagForSykepengegrunnlag(1.januar, 1.januar)?.grunnlagForSykepengegrunnlag())
    }

    @Test
    fun `Bruker inntekt fra infotrygd fremfor inntekt fra skatt for å beregne sykepengegrunnlaget - skatt kommer først`() {
        inntektperioderForSykepengegrunnlag {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNR inntekt INNTEKT
            }
            1.desember(2016) til 1.september(2017) inntekter {
                ORGNR inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, 1.januar, UUID.randomUUID()) }
        Inntektsopplysning.lagreInntekter(
            listOf(Inntektsopplysning(ORGNR, 1.januar, 25000.månedlig, true)),
            historikk,
            UUID.randomUUID()
        )
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(24, inspektør.inntektTeller.first())
        assertEquals(23, inspektør.inntektTeller.last())
        assertEquals(25000.månedlig, historikk.grunnlagForSykepengegrunnlag(1.januar, 1.januar)?.grunnlagForSykepengegrunnlag())
    }

    @Test
    fun `Duplikate opplysninger`() {
        repeat(2) {
            Inntektsopplysning.lagreInntekter(
                listOf(
                    Inntektsopplysning(ORGNR, 1.januar, 25000.månedlig, true),
                    Inntektsopplysning(ORGNR, 1.mars, 25000.månedlig, true)
                ),
                historikk,
                UUID.randomUUID()
            )
        }

        Inntektsopplysning.lagreInntekter(
            listOf(
                Inntektsopplysning(ORGNR, 1.januar, 26900.månedlig, true),
                Inntektsopplysning(ORGNR, 1.mars, 25000.månedlig, true)
            ),
            historikk,
            UUID.randomUUID()
        )

        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(2, inspektør.inntektTeller[0])
        assertEquals(2, inspektør.inntektTeller[1])
    }

    @Test
    fun `Bruker inntekt fra infotrygd fremfor inntekt fra skatt for å beregne sykepengegrunnlaget - skatt kommer sist`() {
        Inntektsopplysning.lagreInntekter(
            listOf(Inntektsopplysning(ORGNR, 1.januar, 25000.månedlig, true)),
            historikk,
            UUID.randomUUID()
        )
        inntektperioderForSykepengegrunnlag {
            1.desember(2016) til 1.desember(2017) inntekter {
                ORGNR inntekt INNTEKT
            }
            1.desember(2016) til 1.september(2017) inntekter {
                ORGNR inntekt INNTEKT
            }
        }.forEach { it.lagreInntekter(historikk, 1.januar, UUID.randomUUID()) }
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(24, inspektør.inntektTeller.first())
        assertEquals(1, inspektør.inntektTeller.last())
        assertEquals(25000.månedlig, historikk.grunnlagForSykepengegrunnlag(1.januar, 1.januar)?.grunnlagForSykepengegrunnlag())
    }

    @Test
    fun `Inntekt for samme dato og annen kilde erstatter ikke eksisterende`() {
        inntektsmelding().addInntekt(historikk, 1.januar, MaskinellJurist())
        Inntektsopplysning.lagreInntekter(
            listOf(Inntektsopplysning(ORGNR, 1.januar, INNTEKT, true)),
            historikk,
            UUID.randomUUID()
        )
        assertEquals(2, inspektør.inntektTeller.size)
        assertEquals(2, inspektør.inntektTeller.first())
        assertEquals(1, inspektør.inntektTeller.last())
    }

    @Test
    fun `Finner nærmeste inntekt fra Infotrygd, hvis det ikke finnes inntekt for skjæringstidspunkt`() {
        Inntektsopplysning.lagreInntekter(
            listOf(
                Inntektsopplysning(ORGNR, 10.januar, 30000.månedlig, true),
                Inntektsopplysning(ORGNR, 5.januar, 25000.månedlig, true)
            ),
            historikk,
            UUID.randomUUID()
        )
        assertEquals(30000.månedlig, historikk.grunnlagForSykepengegrunnlag(1.januar, 11.januar, 1.januar)?.grunnlagForSykepengegrunnlag())
        assertEquals(25000.månedlig, historikk.grunnlagForSykepengegrunnlag(1.januar, 9.januar, 1.januar)?.grunnlagForSykepengegrunnlag())
        assertNull(historikk.grunnlagForSykepengegrunnlag(1.januar, 4.januar, 1.januar))
    }

    @Test
    fun equals() {
        val inntektID = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now()
        val inntektsopplysning1 = Inntektshistorikk.Infotrygd(
            id = inntektID,
            dato = 1.januar,
            hendelseId = hendelseId,
            beløp = 25000.månedlig,
            tidsstempel = tidsstempel
        )
        assertEquals(
            inntektsopplysning1,
            Inntektshistorikk.Infotrygd(
                id = inntektID,
                dato = 1.januar,
                hendelseId = hendelseId,
                beløp = 25000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertNotEquals(
            inntektsopplysning1,
            Inntektshistorikk.Infotrygd(
                id = inntektID,
                dato = 5.januar,
                hendelseId = hendelseId,
                beløp = 25000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertNotEquals(
            inntektsopplysning1,
            Inntektshistorikk.Infotrygd(
                id = inntektID,
                dato = 1.januar,
                hendelseId = hendelseId,
                beløp = 32000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertNotEquals(
            inntektsopplysning1,
            Inntektshistorikk.Infotrygd(
                id = UUID.randomUUID(),
                dato = 1.januar,
                hendelseId = hendelseId,
                beløp = 25000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertNotEquals(
            inntektsopplysning1,
            Inntektshistorikk.Infotrygd(
                id = inntektID,
                dato = 1.januar,
                hendelseId = UUID.randomUUID(),
                beløp = 25000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertEquals(
            inntektsopplysning1,
            Inntektshistorikk.Infotrygd(
                id = inntektID,
                dato = 1.januar,
                hendelseId = hendelseId,
                beløp = 25000.månedlig,
                tidsstempel = LocalDate.EPOCH.atStartOfDay()
            )
        )
    }

    private fun inntektsopplysning(refusjonTom: LocalDate? = null) =
        Inntektsopplysning(ORGNR, DATO, 1000.månedlig, true, refusjonTom)

    private fun inntektsmelding(
        beregnetInntekt: Inntekt = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        arbeidsgiverperioder: List<Periode> = listOf(1.januar til 16.januar)
    ) = Inntektsmelding(
        meldingsreferanseId = UUID.randomUUID(),
        refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
        orgnummer = ORGNR,
        fødselsnummer = "fnr",
        aktørId = "aktør",
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt,
        arbeidsgiverperioder = arbeidsgiverperioder,
        arbeidsforholdId = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt = null,
        mottatt = LocalDateTime.now()
    )
}
