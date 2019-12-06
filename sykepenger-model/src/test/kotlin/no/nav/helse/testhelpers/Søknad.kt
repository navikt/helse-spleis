package no.nav.helse.testhelpers

import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.hendelser.søknad.Sykepengesøknad
import no.nav.helse.toJsonNode
import no.nav.syfo.kafka.sykepengesoknad.dto.PeriodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SykepengesoknadDTO
import java.time.LocalDate

/* Dette er en hjelpeklasse for å generere testdata. Det er viktig at defaults her holdes til absolutt minimum, slik at
 * en ikke ender opp med tester som er avhengig av sære defaults i søknaden
 */
internal class SøknadBuilder {
    internal lateinit var fom: LocalDate
    internal lateinit var tom: LocalDate
    private val perioder = mutableListOf<SoknadsperiodeDTO>()
    private val egenmeldinger = mutableListOf<PeriodeDTO>()

    internal fun søknadsperiode(block: SøknadsperiodeBuilder.() -> Unit) {
        perioder.add(SøknadsperiodeBuilder().apply(block).build())
    }

    internal fun egenmelding(block: PeriodeBuilder.() -> Unit) {
        egenmeldinger.add(PeriodeBuilder().apply(block).build())
    }

    internal fun build(): SykepengesoknadDTO {
        perioder.takeIf { it.isEmpty() }
            ?.add(
                SoknadsperiodeDTO(
                    fom = this@SøknadBuilder.fom,
                    tom = this@SøknadBuilder.tom
                )
            )
        return SykepengesoknadDTO(
            fom = fom,
            tom = tom,
            soknadsperioder = perioder,
            egenmeldinger = egenmeldinger
        )
    }

    internal companion object {
        internal fun søknad(block: SøknadBuilder.() -> Unit) = SøknadBuilder().apply(block).build()
    }
}

internal class SøknadsperiodeBuilder {
    internal var fom: LocalDate? = null
    internal var tom: LocalDate? = null
    internal fun build() = SoknadsperiodeDTO(fom, tom)
}

internal class PeriodeBuilder {
    internal lateinit var fom: LocalDate
    internal lateinit var tom: LocalDate
    internal fun build() = PeriodeDTO(fom, tom)
}

internal fun SykepengesoknadDTO.asHendelse() = SendtSøknadHendelse(Sykepengesøknad(this.toJsonNode()))
