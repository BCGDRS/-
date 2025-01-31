package com.eynnzerr.yuukatalk.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.collection.LruCache
import androidx.recyclerview.widget.RecyclerView
import com.eynnzerr.yuukatalk.data.preference.PreferenceKeys
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

object ImageUtils {

    private val mmkv = MMKV.defaultMMKV()

    val defaultExportPath: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath

    fun generateBitmap(view: View): Bitmap {
        if (view is RecyclerView) {
            return view.adapter?.let {
                generateBitMapInRange(view, 0..<it.itemCount)
            } ?: Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        }

        // never used.
        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        view.draw(canvas)
        return bitmap
    }

    fun generateBitMapInRange(view: RecyclerView, range: IntRange): Bitmap {
        return view.adapter?.let { adapter ->
            assert(adapter.itemCount > range.last && range.first >= 0) {
                "Range outbounds adapter count."
            }

            var iHeight = 0f
            var totalHeight = 0f

            val paint = Paint()
            val cacheSize = (Runtime.getRuntime().maxMemory() / 1024 / 4).toInt()
            val bitmapCache = LruCache<Int, Bitmap>(cacheSize)

            for (i in range) {
                val holder = adapter.createViewHolder(view, adapter.getItemViewType(i))
                adapter.onBindViewHolder(holder, i)
                holder.itemView.apply {
                    view.addView(this)
                    measure(
                        View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    )
                    layout(0, 0, holder.itemView.measuredWidth, holder.itemView.measuredHeight)
                    view.removeView(this)

                    val itemBitmap = Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    val itemCanvas = Canvas(itemBitmap)
                    draw(itemCanvas)
                    bitmapCache.put(i, itemBitmap)
                    totalHeight += height
                    Log.d(TAG, "generateBitmap: talk piece $i height: $height")
                }
            }

            val padding = 16
            val screenshot = Bitmap.createBitmap(view.width + 2 * padding, totalHeight.toInt() + 2 * padding, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(screenshot).apply {
                val bgColor = mmkv.decodeString(PreferenceKeys.BACKGROUND_COLOR) ?: "#fff7e3"
                drawColor(Color.parseColor(bgColor))
            }

            for (i in range) {
                val bitmap = bitmapCache[i]
                canvas.drawBitmap(bitmap!!, 0F + padding, iHeight + padding, paint)
                iHeight += bitmap.height
                bitmap.recycle()
            }

            // draw watermark
            val mmkv = MMKV.defaultMMKV()
            if (mmkv.decodeBool(PreferenceKeys.USE_WATERMARK, false)) {
                val author = mmkv.decodeString(PreferenceKeys.AUTHOR_NAME, "")
                paint.textSize = 20f
                paint.color = Color.DKGRAY
                canvas.drawText(
                    "Author:$author",
                    0f,
                    (canvas.height - 25).toFloat(),
                    paint
                )
                canvas.drawText(
                    "Made by YuukaTalk",
                    0f,
                    (canvas.height - 5).toFloat(),
                    paint
                )
            }

            screenshot
        } ?: Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )
    }

    suspend fun saveBitMapToDisk(bitmap: Bitmap, context: Context): Uri {
        val formatIndex = mmkv.decodeInt(PreferenceKeys.COMPRESS_FORMAT, 1)
        val format = getCompressFormat(formatIndex)
        val suffix = when (formatIndex) {
            0 -> "jpeg"
            1 -> "png"
            else -> "webp"
        }
        Log.d(TAG, "saveBitMapToDisk: format: ${format.name}")

        val file = File(
            // Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            mmkv.decodeString(PreferenceKeys.IMAGE_EXPORT_PATH) ?: defaultExportPath,
            "screenshot-${System.currentTimeMillis()}.$suffix"
        )
        val quality = mmkv.decodeInt(PreferenceKeys.IMAGE_QUALITY, 100)
        file.writeBitmap(bitmap, format, quality)

        return scanFilePath(context, file.path) ?: throw Exception("File could not be saved")
    }

    private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            out.flush()
        }
    }

    private suspend fun scanFilePath(context: Context, filePath: String): Uri? {
        return suspendCancellableCoroutine { continuation ->
            MediaScannerConnection.scanFile(
                context,
                arrayOf(filePath),
                arrayOf("image/*")
            ) { _, scannedUri ->
                if (scannedUri == null) {
                    continuation.cancel(Exception("File $filePath could not be scanned"))
                } else {
                    continuation.resume(scannedUri)
                }
            }
        }
    }

    private val compressFormatMap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        mapOf(
            0 to Bitmap.CompressFormat.JPEG,
            1 to Bitmap.CompressFormat.PNG,
            2 to Bitmap.CompressFormat.WEBP_LOSSY,
            3 to Bitmap.CompressFormat.WEBP_LOSSLESS
        )
    } else {
        mapOf(
            0 to Bitmap.CompressFormat.JPEG,
            1 to Bitmap.CompressFormat.PNG,
        )
    }

    private fun getCompressFormat(index: Int) = compressFormatMap.getOrDefault(index, Bitmap.CompressFormat.PNG)

    private const val TAG = "ImageUtils"
}