package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.*
import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.*
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.somFødselsnummer
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.testhelpers.TestEvent.Companion.inntektsmelding
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class ArbeidsgiverUtbetalingerTest {

    private var maksdato: LocalDate? = null
    private var gjenståendeSykedager: Int? = null
    private var forbrukteSykedager: Int? = 0
    private lateinit var inspektør: UtbetalingstidslinjeInspektør
    private lateinit var aktivitetslogg: Aktivitetslogg

    private companion object {
        val UNG_PERSON_FNR_2018 = "12029240045".somFødselsnummer()
        val PERSON_67_ÅR_5_JANUAR_FNR_2018 = "05015112345".somFødselsnummer()
        val PERSON_68_ÅR_1_DESEMBER_2018 = "01125112345".somFødselsnummer()
        val PERSON_70_ÅR_1_FEBRUAR_2018 = "01024812345".somFødselsnummer()
        val ORGNUMMER = "888888888".somOrganisasjonsnummer()
    }

    @Test
    fun `uavgrenset utbetaling`() {
        undersøke(UNG_PERSON_FNR_2018, 12.NAV)
        assertEquals(12, inspektør.size)
        assertEquals(10, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(12000.0, inspektør.totalUtbetaling())
        assertEquals(12.desember, maksdato)
        assertEquals(238, gjenståendeSykedager)
        assertTrue(aktivitetslogg.hasActivities())
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `avgrenset betaling pga minimum inntekt`() {
        val vilkårsgrunnlagElement = VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = sykepengegrunnlag(1000.månedlig),
            sammenligningsgrunnlag = sammenligningsgrunnlag(1000.månedlig),
            avviksprosent = Prosent.prosent(0.0),
            antallOpptjeningsdagerErMinst = 28,
            harOpptjening = true,
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            harMinimumInntekt = false,
            vurdertOk = false,
            meldingsreferanseId = UUID.randomUUID(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )
        undersøke(UNG_PERSON_FNR_2018, 5.NAV(12), 7.NAV, vilkårsgrunnlagElement = vilkårsgrunnlagElement)

        assertEquals(12, inspektør.size)
        assertEquals(0, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(10, inspektør.avvistDagTeller)
        assertEquals(0.0, inspektør.totalUtbetaling())
        assertEquals(26.desember, maksdato)
        assertEquals(248, gjenståendeSykedager)
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `avgrenset betaling pga maksimum inntekt`() {
        undersøke(UNG_PERSON_FNR_2018, 5.NAV(3500), 7.NAV)

        assertEquals(12, inspektør.size)
        assertEquals(10, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
        assertEquals(10805.0 + 6000.0, inspektør.totalUtbetaling())
        assertEquals(12.desember, maksdato)
        assertEquals(238, gjenståendeSykedager)
        assertFalse(aktivitetslogg.hasErrorsOrWorse()) { aktivitetslogg.toString() }
    }

    @Test
    fun `avgrenset betaling pga minimun sykdomsgrad`() {
        undersøke(UNG_PERSON_FNR_2018, 5.NAV(1200, 19.0), 2.ARB, 5.NAV)

        assertEquals(12, inspektør.size)
        assertEquals(5, inspektør.avvistDagTeller)
        assertEquals(2, inspektør.arbeidsdagTeller)
        assertEquals(5, inspektør.navDagTeller)
        assertEquals(6000.0, inspektør.totalUtbetaling())
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `avgrenset betaling pga oppbrukte sykepengedager`() {
        undersøke(PERSON_67_ÅR_5_JANUAR_FNR_2018, 7.UTELATE, 91.NAV)

        assertEquals(91, inspektør.size)
        assertEquals(60, inspektør.navDagTeller)
        assertEquals(26, inspektør.navHelgDagTeller)
        assertEquals(5, inspektør.avvistDagTeller)
        assertEquals(60 * 1200.0, inspektør.totalUtbetaling())
        assertEquals(30.mars, maksdato)
        assertEquals(0, gjenståendeSykedager)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `avgrenset betaling pga oppbrukte sykepengedager i tillegg til beløpsgrenser`() {
        undersøke(PERSON_67_ÅR_5_JANUAR_FNR_2018, 7.UTELATE, 98.NAV)

        assertEquals(98, inspektør.size)
        assertEquals(60, inspektør.navDagTeller)
        assertEquals(28, inspektør.navHelgDagTeller)
        assertEquals(10, inspektør.avvistDagTeller)
        assertEquals((60 * 1200.0), inspektør.totalUtbetaling())
        assertEquals(30.mars, maksdato)
        assertEquals(0, gjenståendeSykedager)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `historiske utbetalingstidslinjer vurdert i 248 grense`() {
        undersøke(
            PERSON_67_ÅR_5_JANUAR_FNR_2018,
            tidslinjeOf(35.UTELATE, 68.NAV),
            tidslinjeOf(7.UTELATE, 26.NAV)
        )

        assertEquals(68, inspektør.size)
        assertEquals(40, inspektør.navDagTeller)
        assertEquals(10, inspektør.avvistDagTeller)
        assertEquals(40 * 1200.0, inspektør.totalUtbetaling())
        assertEquals(30.mars, maksdato)
        assertEquals(0, gjenståendeSykedager)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `beregn maksdato i et sykdomsforløp som slutter på en fredag`() {
        undersøke(UNG_PERSON_FNR_2018, 16.AP, 4.NAV)
        assertEquals(28.desember, maksdato) // 3 dager already paid, 245 left. So should be fredag!
        assertEquals(245, gjenståendeSykedager)
        assertTrue(aktivitetslogg.hasActivities())
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `beregn maksdato i et sykdomsforløp med opphold i sykdom`() {
        undersøke(UNG_PERSON_FNR_2018, 2.ARB, 16.AP, 7.ARB, 8.NAV)
        assertEquals(8.januar(2019), maksdato)
        assertEquals(242, gjenståendeSykedager)
    }

    @Test
    fun `beregn maksdato (med rest) der den ville falt på en lørdag`() { //(351.S + 1.F + 1.S)
        undersøke(UNG_PERSON_FNR_2018, 16.AP, 335.NAV, 1.FRI, 1.NAV)
        assertEquals(31.desember, maksdato)
        assertEquals(8, gjenståendeSykedager)
    }

    @Test
    fun `beregn maksdato (med rest) der den ville falt på en søndag`() { //(23.S + 2.F + 1.S)
        undersøke(UNG_PERSON_FNR_2018, 16.AP, 7.NAV, 2.FRI, 1.NAV)
        assertEquals(1.januar(2019), maksdato)
        assertEquals(242, gjenståendeSykedager)
    }

    @Test
    fun `maksdato forskyves av ferie etterfulgt av sykedager`() { //21.S + 3.F + 1.S)
        undersøke(UNG_PERSON_FNR_2018, 16.AP, 5.NAV, 3.FRI, 1.NAV)
        assertEquals(2.januar(2019), maksdato)
        assertEquals(244, gjenståendeSykedager)
    }

    @Test
    fun `maksdato forskyves ikke av ferie på tampen av sykdomstidslinjen`() { //(21.S + 3.F)
        undersøke(UNG_PERSON_FNR_2018, 16.AP, 5.NAV, 3.FRI)
        assertEquals(2.januar(2019), maksdato)
        assertEquals(245, gjenståendeSykedager)
    }

    @Test
    fun `maksdato forskyves ikke av ferie etterfulgt av arbeidsdag på tampen av sykdomstidslinjen`() { //(21.S + 3.F + 1.A)
        undersøke(UNG_PERSON_FNR_2018, 16.AP, 5.NAV, 3.FRI, 1.ARB)
        assertEquals(3.januar(2019), maksdato)
        assertEquals(245, gjenståendeSykedager)
    }

    @Test
    fun `setter maksdato når ingen dager er brukt`() { //(16.S)
        undersøke(UNG_PERSON_FNR_2018, 16.AP)
        assertEquals(28.desember, maksdato)
        assertEquals(248, gjenståendeSykedager)
        assertEquals(0, forbrukteSykedager)
    }

    @Test
    fun `når sykepengeperioden går over maksdato, så skal utbetaling stoppe ved maksdato`() { //(249.NAV)
        undersøke(UNG_PERSON_FNR_2018, 16.AP, 349.NAV)
        assertEquals(28.desember, maksdato)
        assertEquals(0, gjenståendeSykedager)
        assertEquals(248, inspektør.navDagTeller)
        assertTrue(aktivitetslogg.hasWarningsOrWorse()) { aktivitetslogg.toString() }
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `når personen fyller 67 blir antall gjenværende dager 60`() {
        undersøke(PERSON_67_ÅR_5_JANUAR_FNR_2018, 16.AP, 94.NAV)
        assertEquals(10.april, maksdato)
        assertEquals(0, gjenståendeSykedager)
        assertEquals(60, inspektør.navDagTeller)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `når personen fyller 67 og 248 dager er brukt opp`() {
        undersøke(PERSON_68_ÅR_1_DESEMBER_2018, 16.AP, 366.NAV)
        assertEquals(28.desember, maksdato)
        assertEquals(0, gjenståendeSykedager)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `når personen fyller 70 skal det ikke utbetales sykepenger`() {
        undersøke(PERSON_70_ÅR_1_FEBRUAR_2018, 16.AP, 405.NAV)
        assertEquals(31.januar, maksdato)
        assertEquals(0, gjenståendeSykedager)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    private fun undersøke(
        fnr: Fødselsnummer,
        vararg utbetalingsdager: Utbetalingsdager,
        vilkårsgrunnlagElement: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement? = null
    ) {
        val tidslinje = tidslinjeOf(*utbetalingsdager)
        undersøke(fnr, tidslinje, tidslinjeOf(), vilkårsgrunnlagElement)
    }

    private fun undersøke(
        fnr: Fødselsnummer,
        arbeidsgiverTidslinje: Utbetalingstidslinje,
        historiskTidslinje: Utbetalingstidslinje,
        vilkårsgrunnlagElement: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement? = null
    ) {
        val person = Person("aktørid", fnr)
        // seed arbeidsgiver med sykdomshistorikk
        person.håndter(
            Sykmelding(
                meldingsreferanseId = UUID.randomUUID(),
                fnr = UNG_PERSON_FNR_2018.toString(),
                aktørId = "",
                orgnummer = ORGNUMMER.toString(),
                sykeperioder = listOf(Sykmeldingsperiode(1.januar, 2.januar, 100.prosent)),
                sykmeldingSkrevet = 1.januar.atStartOfDay(),
                mottatt = 1.januar.atStartOfDay()
            )
        )

        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()

        vilkårsgrunnlagHistorikk.lagre(
            1.januar, vilkårsgrunnlagElement ?: VilkårsgrunnlagHistorikk.Grunnlagsdata(
                skjæringstidspunkt = 1.januar,
                sykepengegrunnlag = sykepengegrunnlag(30000.månedlig),
                sammenligningsgrunnlag = sammenligningsgrunnlag(30000.månedlig),
                avviksprosent = Prosent.prosent(0.0),
                antallOpptjeningsdagerErMinst = 28,
                harOpptjening = true,
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
                harMinimumInntekt = true,
                vurdertOk = true,
                meldingsreferanseId = UUID.randomUUID(),
                vilkårsgrunnlagId = UUID.randomUUID()
            )
        )

        person.håndter(
            inntektsmelding = Inntektsmelding(
                meldingsreferanseId = UUID.randomUUID(),
                refusjon = Inntektsmelding.Refusjon(30000.månedlig, null),
                orgnummer = ORGNUMMER.toString(),
                fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
                aktørId = AbstractPersonTest.AKTØRID,
                førsteFraværsdag = 1.januar,
                beregnetInntekt = 30000.månedlig,
                arbeidsgiverperioder = listOf(),
                arbeidsforholdId = null,
                begrunnelseForReduksjonEllerIkkeUtbetalt = null,
                harOpphørAvNaturalytelser = false,
                mottatt = LocalDateTime.now()
            )
        )
        aktivitetslogg = Aktivitetslogg()

        val infotrygdhistorikk = Infotrygdhistorikk().apply {
            this.oppdaterHistorikk(
                InfotrygdhistorikkElement.opprett(
                    oppdatert = LocalDateTime.now(),
                    hendelseId = UUID.randomUUID(),
                    perioder = historiskTidslinje.takeIf(Utbetalingstidslinje::isNotEmpty)?.let { listOf(infotrygdperiodeMockMed(it)) } ?: emptyList(),
                    inntekter = emptyList(),
                    arbeidskategorikoder = emptyMap(),
                    ugyldigePerioder = emptyList(),
                    harStatslønn = false
                )
            )
        }

        ArbeidsgiverUtbetalinger(
            NormalArbeidstaker,
            mapOf(person.arbeidsgiver(ORGNUMMER) to object : IUtbetalingstidslinjeBuilder {
                override fun build(sykdomstidslinje: Sykdomstidslinje, periode: Periode) {}
                override fun result() = arbeidsgiverTidslinje
            }),
            infotrygdhistorikk,
            fnr.alder(),
            null,
            vilkårsgrunnlagHistorikk
        ).also {
            it.beregn(aktivitetslogg, "88888888", Periode(1.januar, 31.desember(2019)))
            maksdato = it.sykepengerettighet.maksdato
            gjenståendeSykedager = it.sykepengerettighet.gjenståendeSykedager
            forbrukteSykedager = it.sykepengerettighet.forbrukteSykedager
        }
        inspektør = person.arbeidsgiver(ORGNUMMER).nåværendeTidslinje().inspektør
    }

    private fun infotrygdperiodeMockMed(historiskTidslinje: Utbetalingstidslinje) =
        object : Infotrygdperiode(historiskTidslinje.periode().start, historiskTidslinje.periode().endInclusive) {
            override fun accept(visitor: InfotrygdhistorikkVisitor) {}
            override fun utbetalingstidslinje() = historiskTidslinje
        }

    private fun sykepengegrunnlag(inntekt: Inntekt) = Sykepengegrunnlag(
        arbeidsgiverInntektsopplysninger = listOf(),
        sykepengegrunnlag = inntekt,
        grunnlagForSykepengegrunnlag = inntekt,
        begrensning = ER_IKKE_6G_BEGRENSET
    )

    private fun sammenligningsgrunnlag(inntekt: Inntekt) = Sammenligningsgrunnlag(
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning("orgnummer",
                Inntektshistorikk.SkattComposite(UUID.randomUUID(), (0 until 12).map {
                    Inntektshistorikk.Skatt.Sammenligningsgrunnlag(
                        dato = LocalDate.now(),
                        hendelseId = UUID.randomUUID(),
                        beløp = inntekt,
                        måned = YearMonth.of(2017, it + 1),
                        type = Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT,
                        fordel = "fordel",
                        beskrivelse = "beskrivelse"
                    )
                })
            )),
    )

}
