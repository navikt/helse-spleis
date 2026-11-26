package no.nav.helse.spleis.e2e.avslagsdager

import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.selvstendig
import no.nav.helse.dto.SykdomstidslinjeDagDto
import no.nav.helse.februar
import no.nav.helse.hendelser.Avslagsdager
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utlandsopphold
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_8
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class GrunnlagForLøpendeVilkårTest : AbstractDslTest() {
    @Test
    fun `behovet har sykdomstidslinja og søknadene for hver yrkesaktivitet i beregningsperioden`() {
        val søknadA1 = UUID.randomUUID()
        val søknadA2 = UUID.randomUUID()
        val søknadA3 = UUID.randomUUID()
        a1 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(22.januar, 26.januar), søknadId = søknadA1) }
        a2 { håndterSøknad(januar, søknadId = søknadA2) }
        a3 { håndterSøknad(januar, søknadId = søknadA3) }
        listOf(a1, a2, a3).forEach { orgnummer ->
            orgnummer { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        }
        a1 { håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3) }

        val yrkesaktiviteter = sisteBehov().yrkesaktiviteterIBeregningsperioden.associateBy { it.yrkesaktivitetssporing }
        assertEquals(
            mapOf(arbeidstaker(a1) to setOf(søknadA1), arbeidstaker(a2) to setOf(søknadA2), arbeidstaker(a3) to setOf(søknadA3)),
            yrkesaktiviteter.mapValues { (_, it) -> it.søknadIder },
        )
        yrkesaktiviteter.values.forEach { assertEquals(januar.toList(), it.datoer()) }
        val feriedager =
            yrkesaktiviteter
                .getValue(arbeidstaker(a1))
                .sykdomstidslinje.dager
                .filterIsInstance<SykdomstidslinjeDagDto.FeriedagDto>()
        assertEquals((22.januar til 26.januar).toList(), feriedager.map { it.dato })
    }

    @Test
    fun `behovet har bare dagene og søknadene i beregningsperioden`() {
        val søknadFebruar = UUID.randomUUID()
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar, søknadId = søknadFebruar)

            val yrkesaktivitet = sisteBehov().yrkesaktiviteterIBeregningsperioden.single()
            assertEquals(arbeidstaker(a1), yrkesaktivitet.yrkesaktivitetssporing)
            assertEquals(februar.toList(), yrkesaktivitet.datoer())
            assertEquals(setOf(søknadFebruar), yrkesaktivitet.søknadIder)
        }
    }

    @Test
    fun `behovet har søknaden fra selvstendig næringsdrivende`() {
        val søknad = UUID.randomUUID()
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, søknadId = søknad)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)

            val yrkesaktivitet = sisteBehov().yrkesaktiviteterIBeregningsperioden.single()
            assertEquals(Behandlingsporing.Yrkesaktivitet.Selvstendig, yrkesaktivitet.yrkesaktivitetssporing)
            assertEquals(setOf(søknad), yrkesaktivitet.søknadIder)
        }
    }

    @Test
    fun `varslene i løsningen legges på vedtaksperioden til yrkesaktiviteten`() {
        val grunnlagId = UUID.randomUUID()
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                Ferie(22.januar, 26.januar),
                Utlandsopphold(22.januar, 26.januar),
            )
        }
        a2 { håndterSøknad(januar) }
        a3 { håndterSøknad(januar) }
        listOf(a1, a2, a3).forEach { orgnummer ->
            orgnummer { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3)
            håndterYtelser(
                1.vedtaksperiode,
                avslagsdager = grunnlag(grunnlagId, varsler = listOf(arbeidstaker(a2), arbeidstaker(a3))),
            )
        }
        a2 {
            assertVarsler(listOf(RV_SØ_8), 1.vedtaksperiode.filter())
            assertEquals(listOf(grunnlagId), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.grunnlagForLøpendeVilkårIder)
        }
        a3 {
            assertVarsler(listOf(RV_SØ_8), 1.vedtaksperiode.filter())
            assertEquals(listOf(grunnlagId), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.grunnlagForLøpendeVilkårIder)
        }
        a1 {
            // Alle utenlandsdagene er ferie hos a1, så søknaden gir ikke RV_SØ_8, og løsningen har ikke varsel for a1
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertEquals(listOf(grunnlagId), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.grunnlagForLøpendeVilkårIder)
        }
    }

    @Test
    fun `søknaden gir ikke RV_SØ_8 når behovet for avslagsdager er på`() =
        Toggle.Avslagsdagerbehov.enable {
            a1 {
                håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Utlandsopphold(22.januar, 26.januar))
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                håndterVilkårsgrunnlag(1.vedtaksperiode)
                håndterYtelser(1.vedtaksperiode, avslagsdager = grunnlag(UUID.randomUUID()))

                assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            }
        }

    @Test
    fun `RV_SØ_8 kommer fra løsningen når behovet for avslagsdager er på`() =
        Toggle.Avslagsdagerbehov.enable {
            a1 {
                håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Utlandsopphold(22.januar, 26.januar))
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                håndterVilkårsgrunnlag(1.vedtaksperiode)
                håndterYtelser(1.vedtaksperiode, avslagsdager = grunnlag(UUID.randomUUID(), varsler = listOf(arbeidstaker(a1))))

                assertVarsler(listOf(RV_SØ_8), 1.vedtaksperiode.filter())
            }
        }

    @Test
    fun `ny beregning lagrer ID-en fra den nye løsningen`() {
        val førsteGrunnlag = UUID.randomUUID()
        val andreGrunnlag = UUID.randomUUID()
        a1 {
            tilYtelser(januar)
            håndterYtelser(1.vedtaksperiode, avslagsdager = grunnlag(førsteGrunnlag))
            håndterSimulering(1.vedtaksperiode)
            assertEquals(listOf(førsteGrunnlag), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.grunnlagForLøpendeVilkårIder)

            håndterPåminnelse(
                1.vedtaksperiode,
                AVVENTER_GODKJENNING,
                flagg = setOf("ønskerReberegning"),
            )
            håndterYtelser(1.vedtaksperiode, avslagsdager = grunnlag(andreGrunnlag))
            assertEquals(listOf(andreGrunnlag), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.grunnlagForLøpendeVilkårIder)
        }
    }

    @Test
    fun `uten ny løsning blir ID-en fra forrige beregning stående sammen med avslagstidslinjen`() {
        val grunnlagId = UUID.randomUUID()
        a1 {
            tilYtelser(januar)
            håndterYtelser(1.vedtaksperiode, avslagsdager = grunnlag(grunnlagId))
            håndterSimulering(1.vedtaksperiode)

            håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING, flagg = setOf("ønskerReberegning"))
            håndterYtelser(1.vedtaksperiode)
            assertEquals(listOf(grunnlagId), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.grunnlagForLøpendeVilkårIder)
        }
    }

    @Test
    fun `ID-en lagres når personen serialiseres`() {
        val grunnlagId = UUID.randomUUID()
        a1 {
            tilYtelser(januar)
            håndterYtelser(1.vedtaksperiode, avslagsdager = grunnlag(grunnlagId))
        }
        val gjenopprettet = dto().tilPersonData().tilSerialisertPerson().tilPersonDto()
        val endring =
            gjenopprettet.arbeidsgivere
                .single()
                .vedtaksperioder
                .single()
                .behandlinger.behandlinger
                .last()
                .endringer
                .last()
        assertEquals(grunnlagId, endring.grunnlagForLøpendeVilkårId)
    }

    @Test
    fun `uten løsning lagres ingen ID`() {
        a1 {
            tilYtelser(januar)
            håndterYtelser(1.vedtaksperiode)
            assertEquals(listOf(null), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.grunnlagForLøpendeVilkårIder)
        }
    }

    private fun TestPerson.TestArbeidsgiver.tilYtelser(periode: Periode) {
        håndterSøknad(periode)
        håndterArbeidsgiveropplysninger(listOf(periode.start til periode.start.plusDays(15)))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
    }

    private fun sisteBehov(): EventSubscription.TrengerInformasjonTilBeregningEvent =
        observatør.trengerInformasjonTilBeregningEventer.last()

    private fun EventSubscription.TrengerInformasjonTilBeregningEvent.YrkesaktivitetIBeregningsperioden.datoer() =
        sykdomstidslinje.dager.map { it.dato }

    private fun arbeidstaker(orgnummer: String) = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(orgnummer)

    private fun grunnlag(
        grunnlagForLøpendeVilkårId: UUID,
        varsler: List<Behandlingsporing.Yrkesaktivitet> = emptyList(),
    ) = Avslagsdager(
        vurderteDager = emptyList(),
        avslagsdager = emptyList(),
        grunnlagForLøpendeVilkårId = grunnlagForLøpendeVilkårId,
        varsler = varsler.map { Avslagsdager.Varsel(it, RV_SØ_8) },
    )
}
