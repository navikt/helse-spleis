package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.inspectors.Vilkårgrunnlagsinspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf.PARAGRAF_8_2
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.somFødselsnummer
import no.nav.helse.sykepengegrunnlag
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagHistorikkTest {
    private val historikk = VilkårsgrunnlagHistorikk()
    private val inspektør get() = Vilkårgrunnlagsinspektør(historikk)

    companion object {
        private val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold("123456789", 1.desember(2017)))
        private val arbeidsforholdFraHistorikk = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag("123456789", listOf(Arbeidsforholdhistorikk.Arbeidsforhold(1.desember(2017), null, false)))
        )
    }

    @Test
    fun `korrekt antall innslag i vilkårsgrunnlagshistorikken ved én vilkårsprøving`() {
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969".somFødselsnummer(),
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )
        historikk.lagre(1.januar, vilkårsgrunnlag)
        assertNotNull(historikk.vilkårsgrunnlagFor(1.januar))
        val grunnlagsdataInspektør = GrunnlagsdataInspektør(historikk.vilkårsgrunnlagFor(1.januar)!!)
        assertTrue(grunnlagsdataInspektør.vurdertOk)
        assertEquals(1, inspektør.vilkårsgrunnlagTeller[0])
    }

    @Test
    fun `Registrerer subsumsjoner ved validering av vilkårsgrunnlag`() {
        val vilkårsgrunnlag = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )

        val jurist = MaskinellJurist()
        vilkårsgrunnlag.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, jurist),
            1,
            jurist
        )
        SubsumsjonInspektør(jurist).assertVurdert(paragraf = PARAGRAF_8_2, ledd = 1.ledd, versjon = 12.juni(2020))
    }

    @Test
    fun `ny vilkårsprøving på samme skjæringstidspunkt overskriver gammel vilkårsprøving - medfører nytt innslag`() {
        val arbeidsforhold = arbeidsforhold
        val vilkårsgrunnlag1 = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )
        val vilkårsgrunnlag2 = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag1.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )
        vilkårsgrunnlag2.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )

        historikk.lagre(1.januar, vilkårsgrunnlag1)
        assertNotNull(historikk.vilkårsgrunnlagFor(1.januar))
        val grunnlagsdataInspektør1 = GrunnlagsdataInspektør(historikk.vilkårsgrunnlagFor(1.januar)!!)
        assertTrue(grunnlagsdataInspektør1.vurdertOk)

        historikk.lagre(1.januar, vilkårsgrunnlag2)
        assertNotNull(historikk.vilkårsgrunnlagFor(1.januar))
        val grunnlagsdataInspektør2 = GrunnlagsdataInspektør(historikk.vilkårsgrunnlagFor(1.januar)!!)
        assertFalse(grunnlagsdataInspektør2.vurdertOk)

        assertEquals(1, inspektør.vilkårsgrunnlagTeller[0])
        assertEquals(1, inspektør.vilkårsgrunnlagTeller[1])
    }

    @Test
    fun `vilkårsprøving på to ulike skjæringstidspunkt medfører to innslag der siste innslag har vilkårsprøving for begge skjæringstidspunktene`() {
        val vilkårsgrunnlag = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )
        historikk.lagre(1.januar, vilkårsgrunnlag)
        historikk.lagre(4.januar, vilkårsgrunnlag)
        assertEquals(1, inspektør.vilkårsgrunnlagTeller[1])
        assertEquals(2, inspektør.vilkårsgrunnlagTeller[0])
    }

    @Test
    fun `to ulike skjæringstidspunker, der det ene er i infotrygd, medfører to innslag der siste innslag har vilkårsprøving for begge skjæringstidspunktene`() {
        val vilkårsgrunnlag = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
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
        vilkårsgrunnlag.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )

        historikk.lagre(1.januar, vilkårsgrunnlag)
        infotrygdhistorikk.lagreVilkårsgrunnlag(
            skjæringstidspunkt = 4.januar,
            vilkårsgrunnlagHistorikk = historikk,
            kanOverskriveVilkårsgrunnlag = { false },
            sykepengegrunnlagFor = INGEN::sykepengegrunnlag
        )
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
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )
        vilkårsgrunnlagHistorikk.lagre(1.januar, vilkårsgrunnlag)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
        val grunnlagsdataInspektør = GrunnlagsdataInspektør(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)!!)
        assertTrue(grunnlagsdataInspektør.vurdertOk)
    }

    @Test
    fun `Finner vilkårsgrunnlag for skjæringstidspunkt - ikke ok`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )
        vilkårsgrunnlagHistorikk.lagre(1.januar, vilkårsgrunnlag)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
        val grunnlagsdataInspektør = GrunnlagsdataInspektør(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)!!)
        assertFalse(grunnlagsdataInspektør.vurdertOk)
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
        historikk.lagreVilkårsgrunnlag(
            skjæringstidspunkt = 1.januar,
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk,
            kanOverskriveVilkårsgrunnlag = { false },
            sykepengegrunnlagFor = 31000.månedlig::sykepengegrunnlag
        )
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
        historikk.lagreVilkårsgrunnlag(
            skjæringstidspunkt = 1.januar,
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk,
            kanOverskriveVilkårsgrunnlag = { false },
            sykepengegrunnlagFor = 31000.månedlig::sykepengegrunnlag
        )
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
        historikk.lagreVilkårsgrunnlag(
            skjæringstidspunkt = 1.januar,
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk,
            kanOverskriveVilkårsgrunnlag = { false },
            sykepengegrunnlagFor = INGEN::sykepengegrunnlag
        )
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
        historikk.lagreVilkårsgrunnlag(
            skjæringstidspunkt = 1.januar,
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk,
            kanOverskriveVilkårsgrunnlag = { false },
            sykepengegrunnlagFor = INGEN::sykepengegrunnlag
        )
        assertNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `Avviser kun utbetalingsdager som har likt skjæringstidspunkt som et vilkårsgrunnlag som ikke er ok`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag1 = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag1.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )
        val vilkårsgrunnlag2 = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag2.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )
        vilkårsgrunnlagHistorikk.lagre(10.januar, vilkårsgrunnlag1)
        vilkårsgrunnlagHistorikk.lagre(1.januar, vilkårsgrunnlag2)
        val utbetalingstidslinjeMedNavDager = tidslinjeOf(16.AP, 10.NAV)
        vilkårsgrunnlagHistorikk.avvisInngangsvilkår(listOf(utbetalingstidslinjeMedNavDager), "20043769969".somFødselsnummer().alder())
        assertEquals(8, utbetalingstidslinjeMedNavDager.filterIsInstance<Utbetalingstidslinje.Utbetalingsdag.NavDag>().size)
    }

    @Test
    fun `Avviser kun utbetalingsdager som har likt skjæringstidspunkt som et vilkårsgrunnlag som ikke er ok - vilkårsgrunnlag fra IT`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag1 = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag1.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )
        val vilkårsgrunnlag2 = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag2.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )
        vilkårsgrunnlagHistorikk.lagre(10.januar, vilkårsgrunnlag1)
        vilkårsgrunnlagHistorikk.lagre(1.januar, VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag(1.januar, 10000.månedlig.sykepengegrunnlag))
        val utbetalingstidslinjeMedNavDager = tidslinjeOf(16.AP, 10.NAV)
        vilkårsgrunnlagHistorikk.avvisInngangsvilkår(listOf(utbetalingstidslinjeMedNavDager), "20043769969".somFødselsnummer().alder())
        assertEquals(8, utbetalingstidslinjeMedNavDager.filterIsInstance<Utbetalingstidslinje.Utbetalingsdag.NavDag>().size)
    }

    @Test
    fun `Avslår vilkår for minimum inntekt med riktig begrunnelse for personer under 67 år`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(
            10.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )
        vilkårsgrunnlagHistorikk.lagre(1.januar, vilkårsgrunnlag)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
        val grunnlagsdataInspektør = GrunnlagsdataInspektør(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)!!)
        assertFalse(grunnlagsdataInspektør.vurdertOk)
        val utbetalingstidslinjeMedNavDager = tidslinjeOf(16.AP, 10.NAV)
        vilkårsgrunnlagHistorikk.avvisInngangsvilkår(listOf(utbetalingstidslinjeMedNavDager), "20043769969".somFødselsnummer().alder())
        utbetalingstidslinjeMedNavDager.filterIsInstance<Utbetalingstidslinje.Utbetalingsdag.AvvistDag>().let { avvisteDager ->
            assertEquals(8, avvisteDager.size)
            avvisteDager.forEach {
                assertEquals(1, it.begrunnelser.size)
                assertEquals(Begrunnelse.MinimumInntekt, it.begrunnelser.first())
            }
        }
    }

    @Test
    fun `Avslår vilkår for minimum inntekt med riktig begrunnelse for dem mellom 67 og 70`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val fødselsnummer = "01015036963".somFødselsnummer()
        val vilkårsgrunnlag = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = fødselsnummer,
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(
            10.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )
        vilkårsgrunnlagHistorikk.lagre(1.januar, vilkårsgrunnlag)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
        val grunnlagsdataInspektør = GrunnlagsdataInspektør(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)!!)
        assertFalse(grunnlagsdataInspektør.vurdertOk)
        val utbetalingstidslinjeMedNavDager = tidslinjeOf(16.AP, 10.NAV)
        vilkårsgrunnlagHistorikk.avvisInngangsvilkår(listOf(utbetalingstidslinjeMedNavDager), fødselsnummer.alder())

        utbetalingstidslinjeMedNavDager.filterIsInstance<Utbetalingstidslinje.Utbetalingsdag.AvvistDag>().let { avvisteDager ->
            assertEquals(8, avvisteDager.size)
            avvisteDager.forEach {
                assertEquals(1, it.begrunnelser.size)
                assertEquals(Begrunnelse.MinimumInntektOver67, it.begrunnelser.first())
            }
        }
    }

    @Test
    fun `equals av InfotrygdVilkårsgrunnlag`() {
        val sykepengegrunnlag = 25000.månedlig.sykepengegrunnlag
        val element1 = VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag(
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = sykepengegrunnlag
        )
        assertEquals(element1, element1)
        assertEquals(
            element1, VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag(
                skjæringstidspunkt = 1.januar,
                sykepengegrunnlag = sykepengegrunnlag
            )
        )
        assertNotEquals(
            element1, VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag(
                skjæringstidspunkt = 2.januar,
                sykepengegrunnlag = sykepengegrunnlag
            )
        )
        assertNotEquals(
            element1, VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag(
                skjæringstidspunkt = 5.februar,
                sykepengegrunnlag = 25000.månedlig.sykepengegrunnlag
            )
        )
        assertNotEquals(
            element1, VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag(
                skjæringstidspunkt = 1.januar,
                sykepengegrunnlag = 30900.månedlig.sykepengegrunnlag
            )
        )
    }

    @Test
    fun `kan overskrive vilkårsgrunnlag`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag = Vilkårsgrunnlag(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = UUID.randomUUID().toString(),
                aktørId = "AKTØR_ID",
                fødselsnummer = "20043769969".somFødselsnummer(),
                orgnummer = "ORGNUMMER",
                inntektsvurdering = Inntektsvurdering(emptyList()),
                medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = emptyList(), arbeidsforhold = emptyList()),
                arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(
            10000.månedlig.sykepengegrunnlag,
            sammenligningsgrunnlag(10000.månedlig, 1.januar),
            1.januar,
            Opptjening.opptjening(arbeidsforholdFraHistorikk, 1.januar, MaskinellJurist()),
            1,
            MaskinellJurist()
        )
        vilkårsgrunnlagHistorikk.lagre(1.januar, vilkårsgrunnlag)

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
        historikk.lagreVilkårsgrunnlag(
            skjæringstidspunkt = 1.januar,
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk,
            kanOverskriveVilkårsgrunnlag = { true },
            sykepengegrunnlagFor = 31000.månedlig::sykepengegrunnlag
        )
        assertInstanceOf(VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag::class.java, vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }

    private fun sammenligningsgrunnlag(inntekt: Inntekt, skjæringstidspunkt: LocalDate) = Sammenligningsgrunnlag(
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning(
                "ORGNR1",
                Inntektshistorikk.SkattComposite(UUID.randomUUID(), (0 until 12).map {
                    Inntektshistorikk.Skatt.Sammenligningsgrunnlag(
                        dato = skjæringstidspunkt,
                        hendelseId = UUID.randomUUID(),
                        beløp = inntekt,
                        måned = YearMonth.from(skjæringstidspunkt).minusMonths(12L - it),
                        type = Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT,
                        fordel = "fordel",
                        beskrivelse = "beskrivelse"
                    )
                })
            )
        ),
    )
}
