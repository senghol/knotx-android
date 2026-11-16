package com.senghol.knotx.utils.helperclass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.FileOutputStream
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt

class QrPrintAdapter(
    private val bitmap: Bitmap,
    private val text: String
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }

        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("Knotx-QR.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build(),
            true
        )
    }

    override fun onWrite(
        pages: Array<PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        val document = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(
            595,   // A4 width
            842,   // A4 height
            1
        ).create()

        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // ===== Colors =====

        val blue = "#1976D2".toColorInt()
        val lightGray = "#EEEEEE".toColorInt()
        val darkGray = "#666666".toColorInt()

        // ===== Header =====

        val headerPaint = Paint().apply {
            color = blue
        }

        canvas.drawRect(
            0f,
            0f,
            pageInfo.pageWidth.toFloat(),
            110f,
            headerPaint
        )

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 30f
            isFakeBoldText = true
        }

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 16f
        }

        canvas.drawText("Knotx", 40f, 55f, titlePaint)
        canvas.drawText("Scan to connect instantly", 40f, 85f, subtitlePaint)

        // ===== QR Card =====

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            setShadowLayer(8f, 0f, 2f, lightGray)
        }

        val cardLeft = 70f
        val cardTop = 140f
        val cardRight = pageInfo.pageWidth - 70f
        val cardBottom = 620f

        canvas.drawRoundRect(
            cardLeft,
            cardTop,
            cardRight.toFloat(),
            cardBottom,
            24f,
            24f,
            cardPaint
        )
// ===== QR =====

        val qrSize = 448 // Better balance between 320 and 512

        val qr = bitmap.scale(qrSize, qrSize)

        val qrLeft = (pageInfo.pageWidth - qrSize) / 2f
        val qrTop = 155f

        canvas.drawBitmap(
            qr,
            qrLeft,
            qrTop,
            null
        )

// ===== Username / Phone =====

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(
            text,
            pageInfo.pageWidth / 2f,
            qrTop + qrSize + 45f,   // Automatically below QR
            valuePaint
        )

// ===== Instructions =====

        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkGray
            textSize = 15f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(
            "Scan this QR code using the Knotx app",
            pageInfo.pageWidth / 2f,
            qrTop + qrSize + 80f,   // Automatically below username
            infoPaint
        )

        document.finishPage(page)

        FileOutputStream(destination.fileDescriptor).use {
            document.writeTo(it)
        }

        document.close()

        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }

}