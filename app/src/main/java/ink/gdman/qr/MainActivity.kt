package ink.gdman.qr

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class MainActivity : AppCompatActivity() {
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var resultText: TextView
    private lateinit var copyBtn: Button
    private lateinit var goBtn: Button
    private var hasCamera = false
    private var link: String? = null

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamera = granted
        if (granted) barcodeView.resume()
        else Toast.makeText(this, "Нужен доступ к камере", Toast.LENGTH_LONG).show()
    }

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            val t = result.text ?: return
            resultText.text = t
            copyBtn.isEnabled = true
            link = asLink(t)
            goBtn.visibility = if (link != null) View.VISIBLE else View.GONE
            barcodeView.pause()
        }
    }

    private fun asLink(t: String): String? {
        val s = t.trim()
        return when {
            s.contains("://") -> s                                   // есть схема (http, https, vless, tg и т.п.)
            Patterns.WEB_URL.matcher(s).matches() -> "https://$s"    // голый домен -> добавим https
            else -> null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        barcodeView = findViewById(R.id.barcode)
        resultText = findViewById(R.id.resultText)
        copyBtn = findViewById(R.id.copyBtn)
        goBtn = findViewById(R.id.goBtn)
        barcodeView.decodeContinuous(callback)
        barcodeView.setStatusText("")

        copyBtn.setOnClickListener {
            val t = resultText.text.toString()
            if (t.isNotEmpty()) {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("QR", t))
                Toast.makeText(this, "Скопировано в буфер", Toast.LENGTH_SHORT).show()
            }
        }
        goBtn.setOnClickListener {
            val l = link ?: return@setOnClickListener
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(l)))
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.againBtn).setOnClickListener {
            resultText.text = ""
            copyBtn.isEnabled = false
            goBtn.visibility = View.GONE
            link = null
            if (hasCamera) barcodeView.resume()
        }

        hasCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!hasCamera) permLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun onResume() {
        super.onResume()
        if (hasCamera && resultText.text.isNullOrEmpty()) barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}
