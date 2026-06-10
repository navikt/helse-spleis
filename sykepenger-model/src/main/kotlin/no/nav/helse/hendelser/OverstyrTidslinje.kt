package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.OverstyrTidslinje.Avslagsvurdering.SkalAvslås
import no.nav.helse.hendelser.OverstyrTidslinje.Avslagsvurdering.SkalIkkeAvslås
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.nesteDag
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.AAP
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Dagpenger
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Foreldrepenger
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Omsorgspenger
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Opplæringspenger
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Pleiepenger
import no.nav.helse.sykdomstidslinje.Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

data class ManuellOverskrivingDag(
    val dato: LocalDate,
    val type: Dagtype,
    val grad: Int? = null
) {
    init {
        check(type !in setOf(Dagtype.Sykedag, Dagtype.SykedagNav) || grad != null) {
            "👉 Sykedager må ha grad altså 👈"
        }
    }
}

enum class Dagtype {
    Sykedag, Feriedag, ArbeidIkkeGjenopptattDag, Egenmeldingsdag, Permisjonsdag, Arbeidsdag, SykedagNav,
    Foreldrepengerdag, AAPdag, Omsorgspengerdag, Pleiepengerdag, Svangerskapspengerdag, Opplaringspengerdag, Dagpengerdag,
    MeldingTilNavdag, AvslattMeldingTilNavdag;

    companion object {
        val gyldigeTyper = entries.map { it.name }
        val String.dagtype get() = Dagtype.valueOf(this)
    }
}

class OverstyrTidslinje(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    private val dager: List<ManuellOverskrivingDag>,
    opprettet: LocalDateTime,
) : Hendelse {
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = Avsender.SAKSBEHANDLER,
        innsendt = opprettet,
        registrert = LocalDateTime.now(),
        automatiskBehandling = false
    )
    private val kilde = Hendelseskilde(this::class, metadata.meldingsreferanseId, metadata.innsendt)
    private var nesteFraOgMed: LocalDate = LocalDate.MIN

    private val periode: Periode
    var sykdomstidslinje: Sykdomstidslinje
        private set

    private val dagerNavOvertarAnsvar = dager
        .filter { it.type == Dagtype.SykedagNav }
        .map { it.dato }

    init {
        fun Sykdomstidslinje.arbeidstakerspesifikk(type: String) = this.also {
            check(behandlingsporing is Behandlingsporing.Yrkesaktivitet.Arbeidstaker) {
                "$type er spesifikk for Arbeidstakere. Kan ikke brukes for ${behandlingsporing::class.simpleName}"
            }
        }

        fun Sykdomstidslinje.selvstendigspesifikk(type: String) = this.also {
            check(behandlingsporing is Behandlingsporing.Yrkesaktivitet.Selvstendig) {
                "$type er spesifikk for Selvstendig. Kan ikke brukes for ${behandlingsporing::class.simpleName}"
            }
        }

        sykdomstidslinje = dager.map {
            when (it.type) {
                Dagtype.Sykedag,
                Dagtype.SykedagNav -> Sykdomstidslinje.sykedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    grad = checkNotNull(it.grad) { "Sykedag og SykedagNav må ha grad" }.prosent,
                    kilde = kilde
                )

                Dagtype.Feriedag -> Sykdomstidslinje.feriedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde
                ).arbeidstakerspesifikk("Ferie")

                Dagtype.ArbeidIkkeGjenopptattDag -> Sykdomstidslinje.arbeidIkkeGjenopptatt(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde
                ).arbeidstakerspesifikk("Arbeid ikke gjenopptatt")

                Dagtype.Permisjonsdag -> Sykdomstidslinje.permisjonsdager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde
                ).arbeidstakerspesifikk("Permisjon")

                Dagtype.Arbeidsdag -> Sykdomstidslinje.arbeidsdager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde
                )

                Dagtype.Egenmeldingsdag -> Sykdomstidslinje.arbeidsgiverdager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    grad = 100.prosent,
                    kilde = kilde
                ).arbeidstakerspesifikk("Egenmelding")

                Dagtype.Foreldrepengerdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = Foreldrepenger
                )

                Dagtype.AAPdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = AAP
                )

                Dagtype.Omsorgspengerdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = Omsorgspenger
                )

                Dagtype.Pleiepengerdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = Pleiepenger
                )

                Dagtype.Svangerskapspengerdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = Svangerskapspenger
                )

                Dagtype.Opplaringspengerdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = Opplæringspenger
                )

                Dagtype.Dagpengerdag -> Sykdomstidslinje.andreYtelsedager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde,
                    ytelse = Dagpenger
                )

                Dagtype.MeldingTilNavdag -> Sykdomstidslinje.meldingTilNavdager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    grad = 100.prosent,
                    kilde = kilde
                ).selvstendigspesifikk("Melding til Nav")

                Dagtype.AvslattMeldingTilNavdag -> Sykdomstidslinje.arbeidsdager(
                    førsteDato = it.dato,
                    sisteDato = it.dato,
                    kilde = kilde
                ).selvstendigspesifikk("Avslått melding til Nav")
            }
        }.reduce(Sykdomstidslinje::plus)
        periode = checkNotNull(sykdomstidslinje.periode()) {
            "Overstyr tidslinje må ha minst én overstyrt dag"
        }
    }


    internal fun dagerNavOvertarAnsvar(eksisterendeDagerNavOvertarAnsvar: List<Periode>): List<Periode> {
        val utenDagerSaksbehandlerHarEndretPå = eksisterendeDagerNavOvertarAnsvar
            .flatten()
            .filterNot { dag -> dager.any { overstyrtDag -> dag == overstyrtDag.dato } }

        return (utenDagerSaksbehandlerHarEndretPå + dagerNavOvertarAnsvar).grupperSammenhengendePerioder()
    }

    private sealed interface Avslagsvurdering {
        val dato: LocalDate
        data class SkalAvslås(override val dato: LocalDate, val begrunnelser: List<Begrunnelse>): Avslagsvurdering
        data class SkalIkkeAvslås(override val dato: LocalDate): Avslagsvurdering
    }

    internal fun avslagstidslinje(eksisterendeAvslagstidslinje: Avslagstidslinje): Avslagstidslinje {
        val vurderinger = dager
            .map { manuellOverskrivingDag ->
                // Her kan man slå seg løs med nye avslagsdagtyper og tilhørende begrunnelser
                when (manuellOverskrivingDag.type) {
                    Dagtype.AvslattMeldingTilNavdag -> SkalAvslås(manuellOverskrivingDag.dato, listOf(Begrunnelse.AvslåttMeldingTilNavDag))

                    // For dager som er overstyrt til noe annet enn avslagsdager må vi fjerne eventuelle avslagsdager fra eksisterendeAvslagstidslinje
                    else -> SkalIkkeAvslås(manuellOverskrivingDag.dato)
                }
            }

        val avslagstidslinjeHensyntattNyeAvslagsdager = vurderinger
            .filterIsInstance<SkalAvslås>()
            .fold(eksisterendeAvslagstidslinje) { avslagstidslinje, skalAvslås ->
                avslagstidslinje + Avslagstidslinje(skalAvslås.dato, Avslagstidslinje.Avslagsdag(
                    begrunnelser = skalAvslås.begrunnelser,
                    kilde = "Saksbehandler"
                ))
            }

        // Fjerner dager som potensielt var avslag før (på eksisterendeAvslagstidslinje)
        return vurderinger
            .filterIsInstance<SkalIkkeAvslås>()
            .map { it.dato }
            .grupperSammenhengendePerioder()
            .fold(avslagstidslinjeHensyntattNyeAvslagsdager) { avslagstidslinje, skalIkkeAvslås ->
                avslagstidslinje - skalIkkeAvslås
            }
    }

    internal fun vurdertTilOgMed(dato: LocalDate) {
        nesteFraOgMed = dato.nesteDag
        trimSykdomstidslinje(nesteFraOgMed)
    }

    fun erRelevant(other: Periode) = sykdomstidslinje.periode()?.let { other.oppdaterFom(other.start.forrigeDag).overlapperMed(it) } == true

    fun trimSykdomstidslinje(fom: LocalDate) {
        sykdomstidslinje = sykdomstidslinje.fraOgMed(fom)
    }
}
