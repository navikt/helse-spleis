package no.nav.helse.utbetalingstidslinje

import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import java.util.*

private const val pad = '='.code.toByte()

internal fun genererUtbetalingsreferanse(uuid: UUID): String {
    return uuid.base32Encode()
}

internal fun decodeUtbetalingsreferanse(utbetalingsreferanse: String): UUID {
    return Base32(pad).decode(utbetalingsreferanse).let {
        ByteBuffer.wrap(it).asLongBuffer()
    }.let {
        UUID(it.get(), it.get())
    }
}

private fun UUID.base32Encode(): String {
    return Base32(pad)
        .encodeAsString(this.byteArray())
        .replace(pad.toString(), "")
}

private fun UUID.byteArray() = ByteBuffer.allocate(Long.SIZE_BYTES * 2).apply {
    putLong(this@byteArray.mostSignificantBits)
    putLong(this@byteArray.leastSignificantBits)
}.array()

