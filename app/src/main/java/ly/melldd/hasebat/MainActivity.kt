package ly.melldd.hasebat

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import java.util.Locale
import kotlin.math.ceil

class MainActivity : AppCompatActivity() {
    private val dp: Float get() = resources.displayMetrics.density
    private val prefs by lazy { getSharedPreferences("hasebat_prefs", MODE_PRIVATE) }
    private val productIds = listOf("pro_monthly", "pro_yearly")
    private val productDetails = mutableMapOf<String, ProductDetails>()
    private lateinit var billingClient: BillingClient
    private var billingReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(d(20), d(24), d(20), d(24))
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
        }
        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.app_cover)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }, LinearLayout.LayoutParams(-1, d(180)))

        root.addView(TextView(this).apply {
            text = "حاسبة البناء الليبي"
            textSize = 28f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, d(8))
        }, match())

        root.addView(TextView(this).apply {
            text = "حسابات سريعة للبلوك واللياسة والخرسانة والمواد"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, d(14))
        }, match())

        root.addView(Button(this).apply {
            text = "⭐ حاسبة البناء Pro"
            textSize = 18f
            setOnClickListener { showProScreen() }
        }, match())

        val calculators = listOf(
            "حاسبة البلوك" to ::blockCalculator,
            "حاسبة مساحة الحوائط" to ::wallAreaCalculator,
            "حاسبة اللياسة" to ::plasterCalculator,
            "حاسبة الأسمنت والرمل" to ::mortarCalculator,
            "حاسبة الخرسانة" to ::concreteCalculator,
            "حاسبة تكلفة البناء" to ::costCalculator,
            "أسعار البناء الليبية" to ::pricesDialog
        )
        calculators.forEach { (name, action) ->
            root.addView(Button(this).apply {
                text = name
                textSize = 18f
                setOnClickListener { action() }
            }, match())
        }

        root.addView(TextView(this).apply {
            text = "ملاحظة: النتائج تقديرية للاستخدام الأولي، ويجب مراجعتها مع فني أو مهندس للمشاريع الإنشائية."
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, d(18), 0, 0)
        }, match())
        setContentView(root)
    }

    private fun setupBilling() {
        billingClient = BillingClient.newBuilder(this)
            .setListener { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) purchases.forEach { handlePurchase(it) }
            }
            .enablePendingPurchases()
            .build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                billingReady = result.responseCode == BillingClient.BillingResponseCode.OK
                if (billingReady) { queryProducts(); restorePurchases() }
            }
            override fun onBillingServiceDisconnected() { billingReady = false }
        })
    }

    private fun queryProducts() {
        if (!billingReady) return
        val list = productIds.map { QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.SUBS).build() }
        billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(list).build()) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) details.productDetailsList.forEach { productDetails[it.productId] = it }
        }
    }

    private fun buy(productId: String) {
        val details = productDetails[productId] ?: run { queryProducts(); Toast.makeText(this, "المنتج غير متاح حاليًا في Google Play.", Toast.LENGTH_LONG).show(); return }
        val offer = details.subscriptionOfferDetails?.firstOrNull() ?: return
        val pd = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(offer.offerToken).build()
        billingClient.launchBillingFlow(this, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(pd)).build())
    }

    private fun restorePurchases() {
        if (!billingReady) return
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases.forEach { handlePurchase(it) }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            prefs.edit().putBoolean("pro_active", true).apply()
            if (!purchase.isAcknowledged) billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBilling()
        showHome()
    }

    private fun showProScreen() {
        val scroll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(22), d(22), d(22), d(22))
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
        }

        scroll.addView(TextView(this).apply {
            text = "⭐ حاسبة البناء Pro"
            textSize = 30f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, d(8))
        }, match())

        scroll.addView(TextView(this).apply {
            text = "كل أدوات البناء في مكان واحد، بدون إعلانات وبمزايا إضافية."
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, d(20))
        }, match())

        val features = listOf(
            "✓ إزالة الإعلانات",
            "✓ حفظ المشاريع والحسابات",
            "✓ إنشاء تقارير PDF",
            "✓ سجل الحسابات السابقة",
            "✓ أدوات وحاسبات متقدمة",
            "✓ تحديثات ومزايا Pro مستقبلية"
        )
        features.forEach { feature ->
            scroll.addView(TextView(this).apply {
                text = feature
                textSize = 17f
                setPadding(d(8), d(9), d(8), d(9))
            }, match())
        }

        scroll.addView(TextView(this).apply {
            text = "اختر خطتك"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, d(20), 0, d(12))
        }, match())

        addPlan(scroll, "شهري", "10 د.ل / شهر")
        addPlan(scroll, "سنوي", "80 د.ل / سنة")

        scroll.addView(Button(this).apply {
            text = "استعادة المشتريات"
            textSize = 16f
            setOnClickListener {
                Toast.makeText(this@MainActivity, "سيتم ربط استعادة المشتريات مع Google Play Billing عند تفعيل الدفع.", Toast.LENGTH_LONG).show()
            }
        }, match())

        scroll.addView(Button(this).apply {
            text = "← العودة للرئيسية"
            textSize = 16f
            setOnClickListener { showHome() }
        }, match())

        setContentView(scroll)
    }

    private fun addPlan(parent: LinearLayout, name: String, price: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(d(14), d(14), d(14), d(14))
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
        }
        box.addView(TextView(this).apply {
            text = name
            textSize = 20f
            gravity = Gravity.CENTER
        }, match())
        box.addView(TextView(this).apply {
            text = price
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, d(5), 0, d(8))
        }, match())
        box.addView(Button(this).apply {
            text = "اشترك الآن"
            textSize = 16f
            setOnClickListener { buy(productId) }
        }, LinearLayout.LayoutParams(-1, -2))
        parent.addView(box, match())
    }

    private fun blockCalculator() {
        val length = input("طول الحائط بالمتر")
        val height = input("ارتفاع الحائط بالمتر")
        val blockW = input("عرض وجه البلوك بالسنتيمتر", "40")
        val blockH = input("ارتفاع وجه البلوك بالسنتيمتر", "20")
        showForm("حاسبة البلوك", listOf(length, height, blockW, blockH), "احسب") {
            val l = value(length); val h = value(height); val bw = value(blockW); val bh = value(blockH)
            val area = l * h
            val face = (bw / 100.0) * (bh / 100.0)
            val basic = area / face
            val total = ceil(basic * 1.10)
            "مساحة الحائط: ${fmt(area)} م²\nالكمية الأساسية: ${fmt(basic)} بلوكة\nمع 10% هالك: ${fmt(total)} بلوكة"
        }
    }

    private fun wallAreaCalculator() {
        val length = input("طول الحائط بالمتر")
        val height = input("ارتفاع الحائط بالمتر")
        val openings = input("مساحة الأبواب والنوافذ بالمتر المربع", "0")
        showForm("حاسبة مساحة الحوائط", listOf(length, height, openings), "احسب") {
            val gross = value(length) * value(height)
            val net = gross - value(openings)
            require(net >= 0) { "مساحة الفتحات أكبر من مساحة الحائط" }
            "المساحة الإجمالية: ${fmt(gross)} م²\nمساحة الفتحات: ${fmt(value(openings))} م²\nالمساحة الصافية: ${fmt(net)} م²"
        }
    }

    private fun plasterCalculator() {
        val area = input("مساحة اللياسة بالمتر المربع")
        val thickness = input("سماكة اللياسة بالسنتيمتر", "1.5")
        val cementRatio = input("جزء الأسمنت في الخلطة", "1")
        val sandRatio = input("أجزاء الرمل في الخلطة", "4")
        showForm("حاسبة اللياسة", listOf(area, thickness, cementRatio, sandRatio), "احسب") {
            materialResult(value(area), value(thickness), value(cementRatio), value(sandRatio))
        }
    }

    private fun mortarCalculator() {
        val area = input("المساحة بالمتر المربع")
        val thickness = input("السماكة بالسنتيمتر")
        val cementRatio = input("جزء الأسمنت", "1")
        val sandRatio = input("أجزاء الرمل", "4")
        showForm("حاسبة الأسمنت والرمل", listOf(area, thickness, cementRatio, sandRatio), "احسب") {
            materialResult(value(area), value(thickness), value(cementRatio), value(sandRatio))
        }
    }

    private fun materialResult(area: Double, thicknessCm: Double, cement: Double, sand: Double): String {
        require(area > 0 && thicknessCm > 0 && cement > 0 && sand > 0) { "أدخل أرقاماً أكبر من صفر" }
        val wet = area * thicknessCm / 100.0
        val dry = wet * 1.33
        val totalParts = cement + sand
        val cementVol = dry * cement / totalParts
        val sandVol = dry * sand / totalParts
        val cementKg = cementVol * 1440.0
        val bags = ceil(cementKg / 50.0)
        return "الحجم الرطب: ${fmt(wet)} م³\nالحجم التقديري للمواد الجافة: ${fmt(dry)} م³\nالأسمنت: ${fmt(cementKg)} كجم ≈ ${fmt(bags)} كيس (50 كجم)\nالرمل: ${fmt(sandVol)} م³"
    }

    private fun concreteCalculator() {
        val length = input("الطول بالمتر")
        val width = input("العرض بالمتر")
        val depth = input("السماكة/العمق بالمتر")
        showForm("حاسبة الخرسانة", listOf(length, width, depth), "احسب") {
            val volume = value(length) * value(width) * value(depth)
            require(volume > 0) { "أدخل أرقاماً أكبر من صفر" }
            val cementBags = ceil(volume * 7.0)
            val sand = volume * 0.5
            val aggregate = volume * 0.8
            "حجم الخرسانة: ${fmt(volume)} م³\nالأسمنت التقريبي: ${fmt(cementBags)} كيس (50 كجم)\nالرمل التقريبي: ${fmt(sand)} م³\nالحصى التقريبي: ${fmt(aggregate)} م³\n\nهذه تقديرات أولية وليست خلطة تصميم هندسية."
        }
    }

    private fun costCalculator() {
        val area = input("مساحة البناء بالمتر المربع")
        val price = input("سعر بناء المتر المربع بالدينار الليبي", prefs.getString("build_price", "0") ?: "0")
        showForm("حاسبة تكلفة البناء", listOf(area, price), "احسب") {
            val total = value(area) * value(price)
            require(total >= 0) { "تحقق من البيانات" }
            prefs.edit().putString("last_cost", fmt(total)).apply()
            "مساحة البناء: " + fmt(value(area)) + " م²\nسعر المتر: " + fmt(value(price)) + " د.ل\nالتكلفة التقديرية: " + fmt(total) + " د.ل"
        }
    }

    private fun pricesDialog() {
        val field = EditText(this).apply {
            hint = "سعر بناء المتر المربع بالدينار الليبي"
            setText(prefs.getString("build_price", ""))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        AlertDialog.Builder(this).setTitle("أسعار البناء - Pro")
            .setMessage("أدخل سعر المتر المربع حسب أسعار السوق المحلية.")
            .setView(field)
            .setPositiveButton("حفظ") { _, _ ->
                prefs.edit().putString("build_price", field.text.toString()).apply()
                Toast.makeText(this, "تم حفظ السعر", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun input(hint: String, default: String = ""): EditText = EditText(this).apply {
        this.hint = hint
        setText(default)
        textSize = 16f
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
    }

    private fun showForm(title: String, fields: List<EditText>, buttonText: String, calculate: () -> String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(d(20), d(4), d(20), 0)
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
        }
        fields.forEach { box.addView(it, match()) }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(box)
            .setPositiveButton(buttonText) { _, _ ->
                try { resultDialog(title, calculate()) }
                catch (e: Exception) { errorDialog(e.message ?: "تأكد من البيانات المدخلة") }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun resultDialog(title: String, result: String) {
        AlertDialog.Builder(this).setTitle("النتيجة - $title").setMessage(result)
            .setPositiveButton("تم", null).setNeutralButton("العودة للرئيسية") { _, _ -> showHome() }.show()
    }

    private fun errorDialog(message: String) {
        AlertDialog.Builder(this).setTitle("تنبيه").setMessage(message).setPositiveButton("حسناً", null).show()
    }

    private fun value(edit: EditText): Double = edit.text.toString().trim().replace(',', '.').toDouble()
    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
    private fun d(v: Int): Int = (v * dp).toInt()
    private fun match() = LinearLayout.LayoutParams(-1, -2)
}
