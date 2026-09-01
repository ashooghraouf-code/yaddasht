package ir.yaddasht.app.ui.screen

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
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

    private lateinit var pdfImageView: ImageView
    private lateinit var textScrollView: ScrollView
    private lateinit var textContent: TextView
    private lateinit var pageNumText: TextView
    private lateinit var prevBtn: ImageButton
    private lateinit var nextBtn: ImageButton
    private lateinit var themeBtn: ImageButton
    private lateinit var fontSizeBtn: ImageButton
    private lateinit var closeBtn: ImageButton
    private lateinit var pageSeek: SeekBar

    private val themes = intArrayOf(
        0xFFFFF8E1.toInt(), // کاغذی روشن
        0xFFF5E6D3.toInt(), // سپیا
        0xFF1A1A1A.toInt()  // شب
    )
    private val textColors = intArrayOf(
        0xFF2C2C2C.toInt(),
        0xFF3E2723.toInt(),
        0xFFE0E0E0.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        pdfImageView = findViewById(R.id.pdf_image_view)
        textScrollView = findViewById(R.id.text_scroll_view)
        textContent = findViewById(R.id.text_content)
        pageNumText = findViewById(R.id.page_num_text)
        prevBtn = findViewById(R.id.prev_btn)
        nextBtn = findViewById(R.id.next_btn)
        themeBtn = findViewById(R.id.theme_btn)
        fontSizeBtn = findViewById(R.id.font_size_btn)
        closeBtn = findViewById(R.id.close_btn)
        pageSeek = findViewById(R.id.page_seek)

        val filePath = intent.getStringExtra("file_path")
        val isPdf = intent.getBooleanExtra("is_pdf", false)

        if (filePath == null) {
            Toast.makeText(this, "فایل پیدا نشد", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (isPdf) {
            openPdf(filePath)
        } else {
            openText(filePath)
        }

        setupControls()
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

            pdfImageView.visibility = View.VISIBLE
            textScrollView.visibility = View.GONE
            pageSeek.visibility = View.VISIBLE

            pageSeek.max = totalPages - 1
            updatePage()
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در باز کردن PDF: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun openText(path: String) {
        try {
            val text = TextExtractor.extract(path)
            if (text.isBlank()) {
                Toast.makeText(this, "متنی یافت نشد یا فرمت پشتیبانی نمی‌شود", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            pdfImageView.visibility = View.GONE
            textScrollView.visibility = View.VISIBLE
            pageSeek.visibility = View.GONE
            textContent.text = text
            textContent.textSize = fontSize
            pageNumText.text = "حالت مطالعه متن"
        } catch (e: Exception) {
            Toast.makeText(this, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun updatePage() {
        if (totalPages == 0) return

        pdfRenderer?.openPage(currentPage)?.use { page ->
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdfImageView.setImageBitmap(bitmap)
        }

        pageNumText.text = "صفحه ${currentPage + 1} از $totalPages"
        pageSeek.progress = currentPage
    }

    private fun setupControls() {
        prevBtn.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                updatePage()
            }
        }

        nextBtn.setOnClickListener {
            if (currentPage < totalPages - 1) {
                currentPage++
                updatePage()
            }
        }

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
            textContent.textSize = fontSize
            Toast.makeText(this, "اندازه قلم: ${fontSize.toInt()}", Toast.LENGTH_SHORT).show()
        }

        closeBtn.setOnClickListener { finish() }

        applyTheme()
    }

    private fun applyTheme() {
        val bgColor = themes[currentTheme]
        val textColor = textColors[currentTheme]

        findViewById<View>(R.id.reader_root).setBackgroundColor(bgColor)
        textContent.setTextColor(textColor)
        pageNumText.setTextColor(textColor)
        
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
    }
}
