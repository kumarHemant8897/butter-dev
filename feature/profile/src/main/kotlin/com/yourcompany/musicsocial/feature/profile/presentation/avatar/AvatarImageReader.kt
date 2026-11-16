package com.yourcompany.musicsocial.feature.profile.presentation.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.yourcompany.musicsocial.core.common.coroutines.IoDispatcher
import com.yourcompany.musicsocial.core.common.logging.AppLogger
import com.yourcompany.musicsocial.core.common.logging.LogCategory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

/** Turns a picked photo into a small square-ish JPEG for a profile picture. */
interface AvatarImageReader {
    /** JPEG bytes no larger than [MAX_SIDE] px on the long side, or null if the image cannot be read. */
    suspend fun read(uri: String): ByteArray?

    companion object {
        const val MAX_SIDE = 512
        const val QUALITY = 85
    }
}

internal class AndroidAvatarImageReader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val logger: AppLogger,
) : AvatarImageReader {

    override suspend fun read(uri: String): ByteArray? = withContext(io) {
        try {
            val bitmap = decode(Uri.parse(uri)) ?: return@withContext null
            val scaled = scaleDown(bitmap)
            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, AvatarImageReader.QUALITY, out)
                out.toByteArray()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Unreadable or unsupported file: the screen says so; never crash for a bad pick.
            logger.warn(LogCategory.Ui, "Could not read the picked photo", e)
            null
        }
    }

    /**
     * The whole file is read into memory first: decoding straight from the photo picker's
     * stream sometimes failed with "Input was incomplete". ImageDecoder (Android 9+) also
     * applies the photo's rotation; BitmapFactory is the fallback and the path for older phones.
     */
    private fun decode(uri: Uri): Bitmap? {
        val bytes = readAll(uri) ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
                    val longSide = max(info.size.width, info.size.height)
                    if (longSide > AvatarImageReader.MAX_SIDE) {
                        val ratio = AvatarImageReader.MAX_SIDE.toFloat() / longSide
                        decoder.setTargetSize((info.size.width * ratio).roundToInt(), (info.size.height * ratio).roundToInt())
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (e: IOException) {
                logger.warn(LogCategory.Ui, "ImageDecoder could not read the photo; trying BitmapFactory", e)
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= AvatarImageReader.MAX_SIDE) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** Null when the file can't be opened or is larger than [MAX_INPUT_BYTES]. */
    private fun readAll(uri: Uri): ByteArray? =
        context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                total += n
                if (total > MAX_INPUT_BYTES) return null
                out.write(buffer, 0, n)
            }
            out.toByteArray()
        }

    private companion object {
        /** Big enough for any phone camera photo; stops a huge file from exhausting memory. */
        const val MAX_INPUT_BYTES = 40L * 1024 * 1024
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longSide = max(bitmap.width, bitmap.height)
        if (longSide <= AvatarImageReader.MAX_SIDE) return bitmap
        val ratio = AvatarImageReader.MAX_SIDE.toFloat() / longSide
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).roundToInt(), (bitmap.height * ratio).roundToInt(), true)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AvatarModule {
    @Binds
    abstract fun bindAvatarImageReader(impl: AndroidAvatarImageReader): AvatarImageReader
}
