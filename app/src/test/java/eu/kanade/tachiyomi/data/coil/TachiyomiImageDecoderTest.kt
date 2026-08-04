package eu.kanade.tachiyomi.data.coil

import coil3.decode.ImageSource
import coil3.request.Options
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * Verifies that a failed image decode (e.g. an extremely large image on a low-end device) is
 * contained as a controlled, catchable [IllegalStateException] instead of crashing the app.
 *
 * Coil intercepts exceptions thrown by [Decoder.decode] and routes them to the request's error
 * callback, which in the reader surfaces the "decode error" warning layout with retry options.
 */
class TachiyomiImageDecoderTest {

    @Test
    fun `decode throws controlled exception when image source cannot be opened`() = runTest {
        val imageSource = mockk<ImageSource>()
        every { imageSource.sourceOrNull() } returns null
        val options = mockk<Options>()

        val decoder = TachiyomiImageDecoder(imageSource, options)

        val error = shouldThrow<IllegalStateException> { decoder.decode() }
        error.message shouldContain "Failed to initialize decoder"
    }

    @Test
    fun `decode error is a recoverable exception and not a fatal error`() = runTest {
        val imageSource = mockk<ImageSource>()
        every { imageSource.sourceOrNull() } returns null

        val decoder = TachiyomiImageDecoder(imageSource, mockk<Options>())

        // The thrown exception must be a regular Exception subtype (catchable by Coil), never
        // an Error such as OutOfMemoryError escaping the decode pipeline.
        val error = shouldThrow<Exception> { decoder.decode() }
        error.message shouldBe "Failed to initialize decoder"
    }
}
