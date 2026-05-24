package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.AccountEntity
import com.example.security.CryptoUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.spec.SecretKeySpec

object ExportUtils {

    /**
     * Draws the A4 page layout on any Canvas.
     * Dimensions are scaled according to width and height of canvas.
     */
    private fun drawA4Report(
        canvas: Canvas,
        width: Int,
        height: Int,
        accounts: List<AccountEntity>,
        masterKey: SecretKeySpec?,
        showPasswords: Boolean
    ) {
        val backgroundPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Title Paint
        val titlePaint = Paint().apply {
            color = Color.parseColor("#121212")
            textSize = width * 0.045f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#757575")
            textSize = width * 0.025f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Card Paints
        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#F5F5F7")
            style = Paint.Style.FILL
        }

        val cardOutlinePaint = Paint().apply {
            color = Color.parseColor("#E5E5EA")
            style = Paint.Style.STROKE
            strokeWidth = width * 0.002f
        }

        val itemTitlePaint = Paint().apply {
            color = Color.parseColor("#1C1C1E")
            textSize = width * 0.032f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val itemDetailPaint = Paint().apply {
            color = Color.parseColor("#48484A")
            textSize = width * 0.024f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val passwordPaint = Paint().apply {
            color = Color.parseColor("#007AFF")
            textSize = width * 0.026f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val footerPaint = Paint().apply {
            color = Color.parseColor("#8E8E93")
            textSize = width * 0.020f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        // Draw Header
        val paddingLeft = width * 0.06f
        var currentY = height * 0.07f

        // Brand Banner Line
        val brandPaint = Paint().apply {
            color = Color.parseColor("#34C759") // Green theme accent
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.015f, brandPaint)

        canvas.drawText("CorreoVault - Reporte Seguro de Cuentas", paddingLeft, currentY, titlePaint)
        currentY += titlePaint.textSize * 1.3f

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateString = "Generado el: ${sdf.format(Date())} · Total: ${accounts.size} cuentas"
        canvas.drawText(dateString, paddingLeft, currentY, subtitlePaint)
        currentY += width * 0.05f

        // Draw Divider
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#D1D1D6")
            strokeWidth = width * 0.0015f
        }
        canvas.drawLine(paddingLeft, currentY, width - paddingLeft, currentY, dividerPaint)
        currentY += width * 0.04f

        // Draw Accounts (Max that can fit neatly in 1 A4 page)
        // Since we scale, we print max 8 accounts to fit elegantly on a single page, showing a notice if truncated.
        val maxToShow = minOf(accounts.size, 8)
        val cardHeight = height * 0.082f

        for (i in 0 until maxToShow) {
            val acc = accounts[i]
            val cardLeft = paddingLeft
            val cardTop = currentY
            val cardRight = width - paddingLeft
            val cardBottom = currentY + cardHeight

            // Draw Card Body
            val rectF = RectF(cardLeft, cardTop, cardRight, cardBottom)
            val rx = width * 0.015f
            canvas.drawRoundRect(rectF, rx, rx, cardBgPaint)
            canvas.drawRoundRect(rectF, rx, rx, cardOutlinePaint)

            // Provider Pill Accent
            val accentPaint = Paint().apply {
                color = when (acc.provider.lowercase(Locale.getDefault())) {
                    "gmail" -> Color.parseColor("#DE3B3B") // Red
                    "outlook" -> Color.parseColor("#0078D4") // Blue
                    "yahoo" -> Color.parseColor("#6001D2") // Purple
                    else -> Color.parseColor("#8E8E93") // Gray
                }
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(cardLeft, cardTop, cardLeft + (width * 0.012f), cardBottom),
                0f, 0f, accentPaint
            )

            // Inside Card Padding
            var insideY = cardTop + (cardHeight * 0.28f)
            val insideX = cardLeft + (width * 0.035f)

            // Draw Email Prefix and Provider
            canvas.drawText("${acc.email} (${acc.provider.uppercase(Locale.getDefault())})", insideX, insideY, itemTitlePaint)
            insideY += itemTitlePaint.textSize * 1.15f

            // Draw Category · Section · Role
            val metaText = "Categoría: ${acc.categoryName}  |  Sección: ${acc.sectionName}  |  Rol: ${acc.roleName}"
            canvas.drawText(metaText, insideX, insideY, itemDetailPaint)
            insideY += itemDetailPaint.textSize * 1.15f

            // Decrypt password if requested
            val passwordText = if (showPasswords && masterKey != null) {
                val decrypted = CryptoUtils.decrypt(acc.encryptedPassword, masterKey)
                "Contraseña: $decrypted"
            } else {
                "Contraseña: •••••••• (Protegida)"
            }
            canvas.drawText(passwordText, insideX, insideY, passwordPaint)

            currentY += cardHeight + (height * 0.012f)
        }

        // Check if entries are truncated
        if (accounts.size > 8) {
            val truncatedPaint = Paint().apply {
                color = Color.parseColor("#FF9500")
                textSize = width * 0.024f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("· Mostrando las primeras 8 de ${accounts.size} cuentas por espacio en A4 ·", paddingLeft, currentY + 15f, truncatedPaint)
        }

        // Draw Footer
        val footerY = height - (height * 0.04f)
        canvas.drawText(
            "Guardado seguro con encriptación AES-256 local. CorreoVault.",
            paddingLeft,
            footerY,
            footerPaint
        )
    }

    /**
     * Exporst and shares A4 PDF
     */
    fun sharePdfReport(
        context: Context,
        accounts: List<AccountEntity>,
        masterKey: SecretKeySpec?,
        showPasswords: Boolean
    ) {
        try {
            val pdfDocument = PdfDocument()
            // Standard A4 sizes in PostScript points: 595 x 842
            val width = 595
            val height = 842
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            drawA4Report(page.canvas, width, height, accounts, masterKey, showPasswords)
            pdfDocument.finishPage(page)

            val cacheFile = File(context.cacheDir, "Reporte_CorreoVault_${System.currentTimeMillis()}.pdf")
            FileOutputStream(cacheFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()

            // Share file
            val fileUri = FileProvider.getUriForFile(
                context,
                "com.aistudio.correovault.gxywtq.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte Seguro CorreoVault")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Reporte PDF (A4)"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Exports and shares A4 PNG Image
     */
    fun sharePngReport(
        context: Context,
        accounts: List<AccountEntity>,
        masterKey: SecretKeySpec?,
        showPasswords: Boolean
    ) {
        try {
            // high quality A4 aspect ratio bitmap (1190 x 1684)
            val width = 1190
            val height = 1684
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawA4Report(canvas, width, height, accounts, masterKey, showPasswords)

            val cacheFile = File(context.cacheDir, "Imagen_A4_CorreoVault_${System.currentTimeMillis()}.png")
            FileOutputStream(cacheFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }

            // Share File
            val fileUri = FileProvider.getUriForFile(
                context,
                "com.aistudio.correovault.gxywtq.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte Seguro CorreoVault PNG")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Reporte PNG (A4)"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
