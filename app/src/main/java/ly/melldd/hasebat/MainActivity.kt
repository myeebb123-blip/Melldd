package ly.melldd.hasebat

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
        }
        val title = TextView(this).apply {
            text = "حاسبة البناء الليبي"
            textSize = 28f
            setPadding(0, 0, 0, 32)
        }
        root.addView(title)
        listOf(
            "حاسبة البلوك",
            "حاسبة اللياسة",
            "حاسبة الأسمنت والرمل",
            "حاسبة مساحة الحوائط",
            "حاسبة الخرسانة"
        ).forEach { name ->
            root.addView(Button(this).apply {
                text = name
                textSize = 18f
                setOnClickListener { }
            })
        }
        setContentView(root)
    }
}
