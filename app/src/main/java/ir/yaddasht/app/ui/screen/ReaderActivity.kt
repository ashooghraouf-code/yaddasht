package ir.yaddasht.app.ui.screen

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import ir.yaddasht.app.R
import ir.yaddasht.app.util.TextExtractor
import java.io.File

class ReaderActivity : Activity() {

    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: Int = 0
    private var totalPages: Int = 0
    private var currentTheme: Int = 0
    private var fontSize: Float = 16f
    private var isPdf: Boolean = false

    private lateinit var pageContainer: View
    private lateinit var pageImage: ImageView
    private lateinit var pageText: TextView
    private lateinit var pageNumText: TextView
    private lateinit var prevBtn: ImageButton
    private lateinit var nextBtn: ImageButton
    private lateinit var themeBtn: ImageButton
    private lateinit var fontSizeBtn: ImageButton
    private lateinit var closeBtn: ImageButton
    private lateinit var pageSeek: SeekBar
    private lateinit var gestureDetector: GestureDetector

    private val themes = intArrayOf(0xFFFFF8E1.toInt(), 0xFFF5E6D3.toInt(), 0xFF1A1A1A.toInt())
    private val textColors = intArrayOf(0xFF2C2C2C.toInt(), 0xFF3E2723.toInt(), 0xFFE0E0E0.toInt())

    private var pdfPages: List<Bitmap> = emptyList()
    private var textPages: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        pageContainer = findViewById(R.id.page_container)
        pageImage = findViewById(R.id.page_image)
        pageText = findViewById(R.id.page_text)
        pageNumText = findViewById(R.id.page_num_text)
        prevBtn = findViewById(R.id.prev_btn)
        nextBtn = findViewById(R.id.next_btn)
        themeBtn = findViewById(R.id.theme_btn)
        fontSizeBtn = findViewById(R.id.font_size_btn)
        closeBtn = findViewById(R.id.close_btn)
        pageSeek = findViewById(R.id.page_seek)

        val filePath = intent.getStringExtra("file_path")
        isPdf = intent.getBooleanExtra("is_pdf", false)

        if (filePath == null) {
            Toast.makeText(this, "فایل پیدا نشد", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (isPdf) openPdf(filePath) else openText(filePath)

        setupControls()
        setupGestures()
        updatePage()
    }

    private fun openPdf(path: String) {
        try {
            val file = File(path)
            if (!file.exists()) {
                Toast.makeText(this, "فایل وجود ندارد", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            val parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor)
            totalPages = pdfRenderer?.pageCount ?: 0
            currentPage = 0

            pdfPages = (0 until totalPages).map { pageIndex ->
                pdfRenderer?.openPage(pageIndex)?.use { page ->
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                } ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }
            pageSeek.max = totalPages - 1
            pageSeek.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در باز کردن PDF: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun openText(path: String) {
        try {
            val text = TextExtractor.extract(path)
            if (text.isBlank()) {
                Toast.makeText(this, "متنی یافت نشد", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            textPages = text.chunked(600) // تقسیم متن به صفحات ۶۰۰ کاراکتری
            totalPages = textPages.size
            currentPage = 0
            pageSeek.max = totalPages - 1
            pageSeek.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(this, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val swipeThreshold = 100
                val swipeVelocityThreshold = 100
                val diffX = e2.x - (e1?.x ?: 0f)
                val diffY = e2.y - (e1?.y ?: 0f)

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > swipeThreshold && Math.abs(velocityX) > swipeVelocityThreshold) {
                        if (diffX > 0) {
                            // Swipe Right -> Previous Page
                            if (currentPage > 0) goToPage(currentPage - 1, true)
                        } else {
                            // Swipe Left -> Next Page
                            if (currentPage < totalPages - 1) goToPage(currentPage + 1, false)
                        }
                        return true
                    }
                }
                return false
            }
        })

        pageContainer.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun goToPage(targetPage: Int, isRightSwipe: Boolean) {
        val animation = TranslateAnimation(
            if (isRightSwipe) -pageContainer.width.toFloat() else pageContainer.width.toFloat(),
            0f, 0f, 0f
        )
        animation.duration = 300
        animation.fillAfter = true
        
        pageContainer.startAnimation(animation)
        
        currentPage = targetPage
        updatePage()
    }

    private fun updatePage() {
        pageNumText.text = "صفحه ${currentPage + 1} از $totalPages"
        pageSeek.progress = currentPage

        if (isPdf) {
            pageImage.visibility = View.VISIBLE
            pageText.visibility = View.GONE
            if (currentPage < pdfPages.size) {
                pageImage.setImageBitmap(pdfPages[currentPage])
            }
        } else {
            pageImage.visibility = View.GONE
            pageText.visibility = View.VISIBLE
            if (currentPage < textPages.size) {
                pageText.text = textPages[currentPage]
            }
        }
    }

    private fun setupControls() {
        prevBtn.setOnClickListener { if (currentPage > 0) goToPage(currentPage - 1, true) }
        nextBtn.setOnClickListener { if (currentPage < totalPages - 1) goToPage(currentPage + 1, false) }
        
        pageSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentPage = progress
                    updatePage()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        themeBtn.setOnClickListener {
            currentTheme = (currentTheme + 1) % themes.size
            applyTheme()
        }

        fontSizeBtn.setOnClickListener {
            fontSize = when {
                fontSize < 18f -> 18f
                fontSize < 22f -> 22f
                fontSize < 26f -> 26f
                else -> 14f
            }
            pageText.textSize = fontSize
            Toast.makeText(this, "اندازه قلم: ${fontSize.toInt()}", Toast.LENGTH_SHORT).show()
        }

        closeBtn.setOnClickListener { finish() }
        applyTheme()
    }

    private fun applyTheme() {
        val bgColor = themes[currentTheme]
        val textColor = textColors[currentTheme]
        findViewById<View>(R.id.reader_root).setBackgroundColor(bgColor)
        pageContainer.setBackgroundColor(bgColor)
        pageNumText.setTextColor(textColor)
        pageText.setTextColor(textColor)
        
        val tintColor = ColorStateList.valueOf(textColor)
        prevBtn.imageTintList = tintColor
        nextBtn.imageTintList = tintColor
        themeBtn.imageTintList = tintColor
        fontSizeBtn.imageTintList = tintColor
        closeBtn.imageTintList = tintColor
        pageSeek.progressTintList = ColorStateList.valueOf(0xFFF5A524.toInt())
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close()
        pdfPages.forEach { it.recycle() }
    }
}
