package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Fødselsnummer
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hentInfo
import no.nav.helse.inspectors.UtbetalingstidslinjeInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Person
import no.nav.helse.person.Sammenligningsgrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.somFødselsnummer
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykepengegrunnlag
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.UTELATE
import no.nav.helse.testhelpers.Utbetalingsdager
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
        val ORGNUMMER = "888888888"
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
            sykepengegrunnlag = 1000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag(1000.månedlig),
            avviksprosent = Prosent.prosent(0.0),
            opptjening = Opptjening.opptjening(emptyList(), 1.januar, MaskinellJurist()),
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
        assertTrue(aktivitetslogg.hentInfo().contains("Maks antall sykepengedager er nådd i perioden"))
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
        assertTrue(aktivitetslogg.hentInfo().contains("Maks antall sykepengedager er nådd i perioden"))
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
        assertTrue(aktivitetslogg.hentInfo().contains("Maks antall sykepengedager er nådd i perioden"))
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
        assertTrue(aktivitetslogg.hentInfo().contains("Maks antall sykepengedager er nådd i perioden"))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `når personen fyller 67 blir antall gjenværende dager 60`() {
        undersøke(PERSON_67_ÅR_5_JANUAR_FNR_2018, 16.AP, 94.NAV)
        assertEquals(10.april, maksdato)
        assertEquals(0, gjenståendeSykedager)
        assertEquals(60, inspektør.navDagTeller)
        assertTrue(aktivitetslogg.hentInfo().contains("Maks antall sykepengedager er nådd i perioden"))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `når personen fyller 67 og 248 dager er brukt opp`() {
        undersøke(PERSON_68_ÅR_1_DESEMBER_2018, 16.AP, 366.NAV)
        assertEquals(28.desember, maksdato)
        assertEquals(0, gjenståendeSykedager)
        assertTrue(aktivitetslogg.hentInfo().contains("Maks antall sykepengedager er nådd i perioden"))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `når personen fyller 70 skal det ikke utbetales sykepenger`() {
        undersøke(PERSON_70_ÅR_1_FEBRUAR_2018, 1.NAV, startdato = 1.februar)
        assertEquals(31.januar, maksdato)
        assertEquals(0, gjenståendeSykedager)
        assertEquals(0, forbrukteSykedager)
    }

    private fun undersøke(
        fnr: Fødselsnummer,
        vararg utbetalingsdager: Utbetalingsdager,
        vilkårsgrunnlagElement: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement? = null,
        startdato: LocalDate = 1.januar
    ) {
        val tidslinje = tidslinjeOf(*utbetalingsdager, startDato = startdato)
        undersøke(fnr, tidslinje, tidslinjeOf(), vilkårsgrunnlagElement)
    }

    private fun undersøke(
        fnr: Fødselsnummer,
        arbeidsgiverTidslinje: Utbetalingstidslinje,
        historiskTidslinje: Utbetalingstidslinje,
        vilkårsgrunnlagElement: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement? = null
    ) {
        val person = Person("aktørid", fnr, MaskinellJurist())
        // seed arbeidsgiver med sykdomshistorikk
        val førsteDag = arbeidsgiverTidslinje.periode().start
        val sisteDag = arbeidsgiverTidslinje.periode().endInclusive
        person.håndter(
            Sykmelding(
                meldingsreferanseId = UUID.randomUUID(),
                fnr = UNG_PERSON_FNR_2018.toString(),
                aktørId = "",
                orgnummer = ORGNUMMER,
                sykeperioder = listOf(Sykmeldingsperiode(førsteDag, sisteDag, 100.prosent)),
                sykmeldingSkrevet = 1.januar.atStartOfDay(),
                mottatt = 1.januar.atStartOfDay()
            )
        )
        person.håndter(
            Søknad(
                meldingsreferanseId = UUID.randomUUID(),
                fnr = UNG_PERSON_FNR_2018.toString(),
                aktørId = "",
                orgnummer = ORGNUMMER,
                perioder = listOf(Sykdom(førsteDag, sisteDag, 100.prosent)),
                sykmeldingSkrevet = 1.januar.atStartOfDay(),
                andreInntektskilder = emptyList(),
                sendtTilNAVEllerArbeidsgiver = 1.januar.atStartOfDay(),
                permittert = false,
                merknaderFraSykmelding = emptyList()
            )
        )

        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()

        vilkårsgrunnlagHistorikk.lagre(
            1.januar, vilkårsgrunnlagElement ?: VilkårsgrunnlagHistorikk.Grunnlagsdata(
                skjæringstidspunkt = 1.januar,
                sykepengegrunnlag = 30000.månedlig.sykepengegrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag(30000.månedlig),
                avviksprosent = Prosent.prosent(0.0),
                opptjening = Opptjening.opptjening(emptyList(), 1.januar, MaskinellJurist()),
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
                orgnummer = ORGNUMMER,
                fødselsnummer = AbstractPersonTest.UNG_PERSON_FNR_2018.toString(),
                aktørId = AbstractPersonTest.AKTØRID,
                førsteFraværsdag = førsteDag,
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
                override fun result() = arbeidsgiverTidslinje
                override fun fridag(dato: LocalDate) {}
                override fun arbeidsdag(dato: LocalDate) {}
                override fun arbeidsgiverperiodedag(
                    dato: LocalDate,
                    økonomi: Økonomi,
                    kilde: SykdomstidslinjeHendelse.Hendelseskilde
                ) {}
                override fun utbetalingsdag(
                    dato: LocalDate,
                    økonomi: Økonomi,
                    kilde: SykdomstidslinjeHendelse.Hendelseskilde
                ) {}
                override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {}
                override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse) {}
            }),
            infotrygdhistorikk,
            fnr.alder(),
            null,
            vilkårsgrunnlagHistorikk,
            MaskinellJurist()
        ).also {
            it.beregn(aktivitetslogg, "88888888", Periode(1.januar, 31.desember(2019)))
            maksdato = it.maksimumSykepenger.sisteDag()
            gjenståendeSykedager = it.maksimumSykepenger.gjenståendeDager()
            forbrukteSykedager = it.maksimumSykepenger.forbrukteDager()
        }
        inspektør = person.arbeidsgiver(ORGNUMMER).nåværendeTidslinje().inspektør
    }

    private fun infotrygdperiodeMockMed(historiskTidslinje: Utbetalingstidslinje) =
        object : Infotrygdperiode(historiskTidslinje.periode().start, historiskTidslinje.periode().endInclusive) {
            override fun accept(visitor: InfotrygdhistorikkVisitor) {}
            override fun utbetalingstidslinje() = historiskTidslinje
        }

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
