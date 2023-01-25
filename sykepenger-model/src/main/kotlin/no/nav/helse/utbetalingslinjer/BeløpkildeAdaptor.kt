package no.nav.helse.utbetalingslinjer

import no.nav.helse.økonomi.Økonomi

/**
 * Tilpasser Økonomi så det passer til Beløpkilde-porten til utbetalingslinjer
 */
internal class BeløpkildeAdaptor(private val økonomi: Økonomi): Beløpkilde {
    override fun arbeidsgiverbeløp(): Int = økonomi.medAvrundetData { _, _, _, _, _, _, arbeidsgiverbeløp, _, _ -> arbeidsgiverbeløp!! }
    override fun personbeløp(): Int = økonomi.medAvrundetData { _, _, _, _, _, _, _, personbeløp, _ -> personbeløp!! }
}