package no.nav.helse.person

import java.time.LocalDate
import java.util.*
import no.nav.helse.desember
import no.nav.helse.dsl.SubsumsjonsListLog
import no.nav.helse.dsl.lagStandardInntekterForOpptjeningsvurdering
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.etterlevelse.BehandlingSubsumsjonslogg
import no.nav.helse.etterlevelse.Ledd.Companion.ledd
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_2
import no.nav.helse.februar
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.inntektsgrunnlag
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.inspectors.Vilkårgrunnlagsinspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement.Companion.skjæringstidspunktperioder
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.sykepengegrunnlag
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagHistorikkTest {
    private lateinit var historikk: VilkårsgrunnlagHistorikk
    private val inspektør get() = Vilkårgrunnlagsinspektør(historikk.view())
    private val regelverkslogg = SubsumsjonsListLog()
    private val subsumsjonslogg = BehandlingSubsumsjonslogg(regelverkslogg, "fnr", "orgnr", UUID.randomUUID(), UUID.randomUUID())

    companion object {
        private const val ORGNR = "123456789"
        private val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNR, 1.desember(2017), type = Arbeidsforholdtype.ORDINÆRT))
        private val arbeidsforholdFraHistorikk = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(ORGNR, listOf(Arbeidsforhold(1.desember(2017), null, false)))
        )
    }

    @BeforeEach
    fun setup() {
        historikk = VilkårsgrunnlagHistorikk()
    }

    @Test
    fun `lager perioder for vilkårsgrunnlagene`() {
        val grunnlagMedSkjæringstidspunkt = { skjæringstidspunkt: LocalDate ->
            val inntekt = 31000.månedlig
            VilkårsgrunnlagHistorikk.Grunnlagsdata(
                skjæringstidspunkt = skjæringstidspunkt,
                inntektsgrunnlag = inntekt.inntektsgrunnlag(ORGNR),
                opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforholdFraHistorikk, skjæringstidspunkt),
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
                vurdertOk = true,
                meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                vilkårsgrunnlagId = UUID.randomUUID()
            )
        }
        val grunnlag1Januar = grunnlagMedSkjæringstidspunkt(1.januar)
        val grunnlag1Februar = grunnlagMedSkjæringstidspunkt(1.februar)

        assertEquals(1.januar til LocalDate.MAX, skjæringstidspunktperioder(listOf(grunnlag1Januar)).single())
        skjæringstidspunktperioder(listOf(grunnlag1Januar, grunnlag1Februar)).also { resultat ->
            assertEquals(listOf(januar, 1.februar til LocalDate.MAX), resultat)
        }
        skjæringstidspunktperioder(listOf(grunnlag1Februar, grunnlag1Januar)).also { resultat ->
            assertEquals(listOf(januar, 1.februar til LocalDate.MAX), resultat)
        }
    }

    @Test
    fun `fjerner vilkårsgrunnlag som ikke gjelder lengre`() {
        val inntekt = 21000.månedlig
        val gammeltSkjæringstidspunkt = 10.januar
        val nyttSkjæringstidspunkt = 1.januar

        historikk.lagre(
            VilkårsgrunnlagHistorikk.Grunnlagsdata(
                skjæringstidspunkt = gammeltSkjæringstidspunkt,
                inntektsgrunnlag = inntekt.inntektsgrunnlag(ORGNR),
                opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforholdFraHistorikk, gammeltSkjæringstidspunkt),
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
                vurdertOk = true,
                meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                vilkårsgrunnlagId = UUID.randomUUID()
            )
        )

        assertEquals(1, historikk.inspektør.vilkårsgrunnlagTeller.size)
        historikk.oppdaterHistorikk(Aktivitetslogg(), setOf(nyttSkjæringstidspunkt))

        assertEquals(2, historikk.inspektør.vilkårsgrunnlagTeller.size)
        assertEquals(0, historikk.inspektør.vilkårsgrunnlagTeller[0]) { "det siste innslaget skal være tomt" }
        assertEquals(1, historikk.inspektør.vilkårsgrunnlagTeller[1])
        assertNull(historikk.vilkårsgrunnlagFor(gammeltSkjæringstidspunkt)) { "skal ikke beholde vilkårsgrunnlag for skjæringstidspunkter som ikke finnes" }
        assertNull(historikk.vilkårsgrunnlagFor(nyttSkjæringstidspunkt)) { "skal ikke ha vilkårsgrunnlag for skjæringstidspunkt som ikke er vilkårsprøvd" }
    }

    @Test
    fun `korrekt antall innslag i vilkårsgrunnlagshistorikken ved én vilkårsprøving`() {
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            skjæringstidspunkt = 1.januar,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker("ORGNUMMER"),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(emptyList(), 1.januar),
            inntekterForOpptjeningsvurdering = lagStandardInntekterForOpptjeningsvurdering("ORGNUMMER", INGEN, 1.januar),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(
            Aktivitetslogg(),
            10000.månedlig.sykepengegrunnlag,
            subsumsjonslogg
        )
        historikk.lagre(vilkårsgrunnlag.grunnlagsdata())
        assertNotNull(historikk.vilkårsgrunnlagFor(1.januar))
        val grunnlagsdataInspektør = GrunnlagsdataInspektør(historikk.vilkårsgrunnlagFor(1.januar)!!.view())
        assertTrue(grunnlagsdataInspektør.vurdertOk)
        assertEquals(1, inspektør.vilkårsgrunnlagTeller[0])
    }

    @Test
    fun `Registrerer subsumsjoner ved validering av vilkårsgrunnlag`() {
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            skjæringstidspunkt = 1.januar,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker("ORGNUMMER"),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(emptyList(), 1.januar),
            inntekterForOpptjeningsvurdering = lagStandardInntekterForOpptjeningsvurdering("ORGNUMMER", INGEN, 1.januar),
            arbeidsforhold = arbeidsforhold
        )

        vilkårsgrunnlag.valider(Aktivitetslogg(), 10000.månedlig.sykepengegrunnlag, subsumsjonslogg)
        SubsumsjonInspektør(regelverkslogg).assertVurdert(paragraf = PARAGRAF_8_2, ledd = 1.ledd, versjon = 12.juni(2020))
    }

    @Test
    fun `ny vilkårsprøving på samme skjæringstidspunkt overskriver gammel vilkårsprøving - medfører nytt innslag`() {
        val arbeidsforhold = arbeidsforhold
        val vilkårsgrunnlag1 = Vilkårsgrunnlag(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            skjæringstidspunkt = 1.januar,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker("ORGNUMMER"),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(emptyList(), 1.januar),
            inntekterForOpptjeningsvurdering = lagStandardInntekterForOpptjeningsvurdering("ORGNUMMER", INGEN, 1.januar),
            arbeidsforhold = arbeidsforhold
        )
        val vilkårsgrunnlag2 = Vilkårsgrunnlag(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            skjæringstidspunkt = 1.januar,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker("ORGNUMMER"),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei),
            inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(emptyList(), 1.januar),
            inntekterForOpptjeningsvurdering = lagStandardInntekterForOpptjeningsvurdering("ORGNUMMER", INGEN, 1.januar),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag1.valider(
            Aktivitetslogg(),
            10000.månedlig.sykepengegrunnlag,
            subsumsjonslogg
        )
        vilkårsgrunnlag2.valider(
            Aktivitetslogg(),
            10000.månedlig.sykepengegrunnlag,
            subsumsjonslogg
        )

        historikk.lagre(vilkårsgrunnlag1.grunnlagsdata())
        assertNotNull(historikk.vilkårsgrunnlagFor(1.januar))
        val grunnlagsdataInspektør1 = GrunnlagsdataInspektør(historikk.vilkårsgrunnlagFor(1.januar)!!.view())
        assertTrue(grunnlagsdataInspektør1.vurdertOk)

        historikk.lagre(vilkårsgrunnlag2.grunnlagsdata())
        assertNotNull(historikk.vilkårsgrunnlagFor(1.januar))
        val grunnlagsdataInspektør2 = GrunnlagsdataInspektør(historikk.vilkårsgrunnlagFor(1.januar)!!.view())
        assertFalse(grunnlagsdataInspektør2.vurdertOk)

        assertEquals(1, inspektør.vilkårsgrunnlagTeller[0])
        assertEquals(1, inspektør.vilkårsgrunnlagTeller[1])
    }

    @Test
    fun `vilkårsprøving på to ulike skjæringstidspunkt medfører to innslag der siste innslag har vilkårsprøving for begge skjæringstidspunktene`() {
        val vilkårsgrunnlag1 = Vilkårsgrunnlag(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            skjæringstidspunkt = 1.januar,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker("ORGNUMMER"),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(emptyList(), 1.januar),
            inntekterForOpptjeningsvurdering = lagStandardInntekterForOpptjeningsvurdering("ORGNUMMER", INGEN, 1.januar),
            arbeidsforhold = arbeidsforhold
        )
        val vilkårsgrunnlag2 = Vilkårsgrunnlag(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            skjæringstidspunkt = 2.januar,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker("ORGNUMMER"),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(emptyList(), 1.januar),
            inntekterForOpptjeningsvurdering = lagStandardInntekterForOpptjeningsvurdering("ORGNUMMER", INGEN, 1.januar),
            arbeidsforhold = arbeidsforhold
        )

        vilkårsgrunnlag1.valider(Aktivitetslogg(), 10000.månedlig.sykepengegrunnlag, subsumsjonslogg)
        vilkårsgrunnlag2.valider(Aktivitetslogg(), 10000.månedlig.sykepengegrunnlag, subsumsjonslogg)
        historikk.lagre(vilkårsgrunnlag1.grunnlagsdata())
        historikk.lagre(vilkårsgrunnlag2.grunnlagsdata())
        assertEquals(1, inspektør.vilkårsgrunnlagTeller[1])
        assertEquals(2, inspektør.vilkårsgrunnlagTeller[0])
    }

    @Test
    fun `Finner vilkårsgrunnlag for skjæringstidspunkt - ok`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            skjæringstidspunkt = 1.januar,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker("ORGNUMMER"),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(emptyList(), 1.januar),
            inntekterForOpptjeningsvurdering = lagStandardInntekterForOpptjeningsvurdering("ORGNUMMER", INGEN, 1.januar),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(
            Aktivitetslogg(),
            10000.månedlig.sykepengegrunnlag,
            subsumsjonslogg
        )
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag.grunnlagsdata())
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
        val grunnlagsdataInspektør = GrunnlagsdataInspektør(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)!!.view())
        assertTrue(grunnlagsdataInspektør.vurdertOk)
    }

    @Test
    fun `Finner vilkårsgrunnlag for skjæringstidspunkt - ikke ok`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            skjæringstidspunkt = 1.januar,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker("ORGNUMMER"),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei),
            inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(emptyList(), 1.januar),
            inntekterForOpptjeningsvurdering = lagStandardInntekterForOpptjeningsvurdering("ORGNUMMER", INGEN, 1.januar),
            arbeidsforhold = arbeidsforhold
        )
        vilkårsgrunnlag.valider(
            Aktivitetslogg(),
            10000.månedlig.sykepengegrunnlag,
            subsumsjonslogg
        )
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag.grunnlagsdata())
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
        val grunnlagsdataInspektør = GrunnlagsdataInspektør(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)!!.view())
        assertFalse(grunnlagsdataInspektør.vurdertOk)
    }
}
