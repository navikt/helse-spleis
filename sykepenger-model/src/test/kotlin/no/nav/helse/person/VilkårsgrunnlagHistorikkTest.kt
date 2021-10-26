package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagHistorikkTest {

    private lateinit var historikk: VilkårsgrunnlagHistorikk
    private val inspektør get() = Vilkårgrunnlagsinspektør(historikk)

    companion object {
        private val arbeidsforhold = listOf(Arbeidsforhold("123456789", 1.desember(2017)))
    }

    @BeforeEach
    fun beforEach() {
        historikk = VilkårsgrunnlagHistorikk()
    }

    @Test
    fun `korrekt antall innslag i vilkårsgrunnlagshistorikken ved én vilkårsprøving`() {
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(sykepengegrunnlag(10000.månedlig), 10000.månedlig, 1.januar, 1, Periodetype.FØRSTEGANGSBEHANDLING)
        historikk.lagre(vilkårsgrunnlag, 1.januar)
        assertNotNull(historikk.vilkårsgrunnlagFor(1.januar))
        assertTrue(historikk.vilkårsgrunnlagFor(1.januar)!!.isOk())
        assertEquals(1, inspektør.vilkårsgrunnlagTeller[0])
    }

    @Test
    fun `ny vilkårsprøving på samme skjæringstidspunkt overskriver gammel vilkårsprøving - medfører nytt innslag`() {
        val arbeidsforhold = arbeidsforhold
        val vilkårsgrunnlag1 = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        val vilkårsgrunnlag2 = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag1.valider(sykepengegrunnlag(10000.månedlig), 10000.månedlig, 1.januar, 1, Periodetype.FØRSTEGANGSBEHANDLING)
        vilkårsgrunnlag2.valider(sykepengegrunnlag(10000.månedlig), 10000.månedlig, 1.januar, 1, Periodetype.FØRSTEGANGSBEHANDLING)

        historikk.lagre(vilkårsgrunnlag1, 1.januar)
        assertNotNull(historikk.vilkårsgrunnlagFor(1.januar))
        assertTrue(historikk.vilkårsgrunnlagFor(1.januar)!!.isOk())

        historikk.lagre(vilkårsgrunnlag2, 1.januar)
        assertNotNull(historikk.vilkårsgrunnlagFor(1.januar))
        assertFalse(historikk.vilkårsgrunnlagFor(1.januar)!!.isOk())

        assertEquals(1, inspektør.vilkårsgrunnlagTeller[0])
        assertEquals(1, inspektør.vilkårsgrunnlagTeller[1])
    }

    @Test
    fun `vilkårsprøving på to ulike skjæringstidspunkt medfører to innslag der siste innslag har vilkårsprøving for begge skjæringstidspunktene`() {
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(sykepengegrunnlag(10000.månedlig), 10000.månedlig, 1.januar, 1, Periodetype.FØRSTEGANGSBEHANDLING)
        historikk.lagre(vilkårsgrunnlag, 1.januar)
        historikk.lagre(vilkårsgrunnlag, 4.januar)
        assertEquals(1, inspektør.vilkårsgrunnlagTeller[1])
        assertEquals(2, inspektør.vilkårsgrunnlagTeller[0])
    }

    @Test
    fun `to ulike skjæringstidspunker, der det ene er i infotrygd, medfører to innslag der siste innslag har vilkårsprøving for begge skjæringstidspunktene`() {
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        val infotrygdhistorikk = Infotrygdhistorikk().apply {
            oppdaterHistorikk(
                InfotrygdhistorikkElement.opprett(
                    oppdatert = LocalDateTime.now(),
                    hendelseId = UUID.randomUUID(),
                    perioder = emptyList(),
                    inntekter = listOf(
                        Inntektsopplysning("ORGNUMMER", 4.januar, 31000.månedlig, true)
                    ),
                    arbeidskategorikoder = emptyMap(),
                    ugyldigePerioder = emptyList(),
                    harStatslønn = false
                )
            )
        }
        vilkårsgrunnlag.valider(sykepengegrunnlag(10000.månedlig), 10000.månedlig, 1.januar, 1, Periodetype.FØRSTEGANGSBEHANDLING)

        historikk.lagre(vilkårsgrunnlag, 1.januar)
        infotrygdhistorikk.lagreVilkårsgrunnlag(4.januar, Periodetype.OVERGANG_FRA_IT, historikk, sykepengegrunnlagFor(INGEN))
        assertEquals(1, inspektør.vilkårsgrunnlagTeller[1])
        assertEquals(2, inspektør.vilkårsgrunnlagTeller[0])
    }

    @Test
    fun `Finner vilkårsgrunnlag for skjæringstidspunkt - ok`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(sykepengegrunnlag(10000.månedlig), 10000.månedlig, 1.januar, 1, Periodetype.FØRSTEGANGSBEHANDLING)
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag, 1.januar)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
        assertTrue(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)!!.isOk())
    }

    @Test
    fun `Finner vilkårsgrunnlag for skjæringstidspunkt - ikke ok`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(sykepengegrunnlag(10000.månedlig), 10000.månedlig, 1.januar, 1, Periodetype.FØRSTEGANGSBEHANDLING)
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag, 1.januar)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
        assertFalse(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)!!.isOk())
    }

    @Test
    fun `lagrer grunnlagsdata fra Infotrygd ved overgang fra IT`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val historikk = Infotrygdhistorikk().apply {
            oppdaterHistorikk(
                InfotrygdhistorikkElement.opprett(
                    oppdatert = LocalDateTime.now(),
                    hendelseId = UUID.randomUUID(),
                    perioder = emptyList(),
                    inntekter = listOf(Inntektsopplysning("987654321", 1.januar, 31000.månedlig, true)),
                    arbeidskategorikoder = emptyMap(),
                    ugyldigePerioder = emptyList(),
                    harStatslønn = false
                )
            )
        }
        historikk.lagreVilkårsgrunnlag(1.januar, Periodetype.OVERGANG_FRA_IT, vilkårsgrunnlagHistorikk, sykepengegrunnlagFor(31000.månedlig))
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `lagrer grunnlagsdata fra Infotrygd ved infotrygdforlengelse`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val historikk = Infotrygdhistorikk().apply {
            oppdaterHistorikk(
                InfotrygdhistorikkElement.opprett(
                    oppdatert = LocalDateTime.now(),
                    hendelseId = UUID.randomUUID(),
                    perioder = emptyList(),
                    inntekter = listOf(Inntektsopplysning("987654321", 1.januar, 31000.månedlig, true)),
                    arbeidskategorikoder = emptyMap(),
                    ugyldigePerioder = emptyList(),
                    harStatslønn = false
                )
            )
        }
        historikk.lagreVilkårsgrunnlag(1.januar, Periodetype.INFOTRYGDFORLENGELSE, vilkårsgrunnlagHistorikk, sykepengegrunnlagFor(31000.månedlig))
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `lagrer ikke grunnlagsdata ved førstegangsbehandling`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val historikk = Infotrygdhistorikk().apply {
            oppdaterHistorikk(
                InfotrygdhistorikkElement.opprett(
                    oppdatert = LocalDateTime.now(),
                    hendelseId = UUID.randomUUID(),
                    perioder = emptyList(),
                    inntekter = emptyList(),
                    arbeidskategorikoder = emptyMap(),
                    ugyldigePerioder = emptyList(),
                    harStatslønn = false
                )
            )
        }
        historikk.lagreVilkårsgrunnlag(1.januar, Periodetype.FØRSTEGANGSBEHANDLING, vilkårsgrunnlagHistorikk, sykepengegrunnlagFor(INGEN))
        assertNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `lagrer ikke grunnlagsdata ved forlengelse`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val historikk = Infotrygdhistorikk().apply {
            oppdaterHistorikk(
                InfotrygdhistorikkElement.opprett(
                    oppdatert = LocalDateTime.now(),
                    hendelseId = UUID.randomUUID(),
                    perioder = emptyList(),
                    inntekter = emptyList(),
                    arbeidskategorikoder = emptyMap(),
                    ugyldigePerioder = emptyList(),
                    harStatslønn = false
                )
            )
        }
        historikk.lagreVilkårsgrunnlag(1.januar, Periodetype.FORLENGELSE, vilkårsgrunnlagHistorikk, sykepengegrunnlagFor(INGEN))
        assertNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `Avviser kun utbetalingsdager som har likt skjæringstidspunkt som et vilkårsgrunnlag som ikke er ok`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag1 = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag1.valider(sykepengegrunnlag(10000.månedlig), 10000.månedlig, 1.januar, 1, Periodetype.FØRSTEGANGSBEHANDLING)
        val vilkårsgrunnlag2 = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag2.valider(sykepengegrunnlag(10000.månedlig), 10000.månedlig, 1.januar, 1, Periodetype.FØRSTEGANGSBEHANDLING)
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag1, 10.januar)
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag2, 1.januar)
        val utbetalingstidslinjeMedNavDager = tidslinjeOf(16.AP, 3.NAV, 2.HELG, 5.NAV)
        vilkårsgrunnlagHistorikk.avvisUtbetalingsdagerMedBegrunnelse(listOf(utbetalingstidslinjeMedNavDager))
        assertEquals(8, utbetalingstidslinjeMedNavDager.filterIsInstance<Utbetalingstidslinje.Utbetalingsdag.NavDag>().size)
    }

    @Test
    fun `Avviser kun utbetalingsdager som har likt skjæringstidspunkt som et vilkårsgrunnlag som ikke er ok - vilkårsgrunnlag fra IT`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag1 = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag1.valider(sykepengegrunnlag(10000.månedlig), 10000.månedlig, 1.januar, 1, Periodetype.FØRSTEGANGSBEHANDLING)
        val vilkårsgrunnlag2 = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag2.valider(sykepengegrunnlag(10000.månedlig), 10000.månedlig, 1.januar, 1, Periodetype.FØRSTEGANGSBEHANDLING)
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag1, 10.januar)
        vilkårsgrunnlagHistorikk.lagre(1.januar, VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag(sykepengegrunnlag(10000.månedlig)))
        val utbetalingstidslinjeMedNavDager = tidslinjeOf(16.AP, 3.NAV, 2.HELG, 5.NAV)
        vilkårsgrunnlagHistorikk.avvisUtbetalingsdagerMedBegrunnelse(listOf(utbetalingstidslinjeMedNavDager))
        assertEquals(8, utbetalingstidslinjeMedNavDager.filterIsInstance<Utbetalingstidslinje.Utbetalingsdag.NavDag>().size)
    }

    @Test
    fun `Avslår vilkår for minimum inntekt med riktig begrunnelse`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(sykepengegrunnlag(10.månedlig), 10.månedlig, 1.januar, 1, Periodetype.FØRSTEGANGSBEHANDLING)
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag, 1.januar)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
        assertFalse(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)!!.isOk())
        val utbetalingstidslinjeMedNavDager = tidslinjeOf(16.AP, 3.NAV, 2.HELG, 5.NAV)
        vilkårsgrunnlagHistorikk.avvisUtbetalingsdagerMedBegrunnelse(listOf(utbetalingstidslinjeMedNavDager))
        assertEquals(
            8,
            utbetalingstidslinjeMedNavDager.filterIsInstance<Utbetalingstidslinje.Utbetalingsdag.AvvistDag>()
                .filter { it.begrunnelser.size == 1 && it.begrunnelser.first() == Begrunnelse.MinimumInntekt }.size
        )
    }

    private fun sykepengegrunnlag(inntekt: Inntekt) =
        Sykepengegrunnlag(
            arbeidsgiverInntektsopplysninger = listOf(),
            sykepengegrunnlag = inntekt,
            grunnlagForSykepengegrunnlag = inntekt,
            begrensning = ER_IKKE_6G_BEGRENSET
        )

    private fun sykepengegrunnlagFor(inntekt: Inntekt): (LocalDate) -> Sykepengegrunnlag = {
        Sykepengegrunnlag(
            arbeidsgiverInntektsopplysninger = listOf(),
            sykepengegrunnlag = inntekt,
            grunnlagForSykepengegrunnlag = inntekt,
            begrensning = ER_IKKE_6G_BEGRENSET
        )
    }
}
