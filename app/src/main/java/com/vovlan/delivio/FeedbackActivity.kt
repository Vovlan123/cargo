package com.vovlan.delivio

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.graphics.Color
import android.view.Gravity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.DocumentSnapshot

class FeedbackActivity : AppCompatActivity() {

    // нижняя панель
    private lateinit var tabHistory: LinearLayout
    private lateinit var tabHome: LinearLayout
    private lateinit var tabFeedback: LinearLayout
    private lateinit var bottomNav: LinearLayout

    // чат
    private lateinit var sendButton: ImageButton
    private lateinit var messageInput: EditText
    private lateinit var thanksText: TextView
    private lateinit var messagesContainer: LinearLayout

    // Firestore
    private val db = Firebase.firestore
    private val messagesRef = db.collection("feedbackMessages")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        // нижняя панель
        tabHistory = findViewById(R.id.tabHistory)
        tabHome = findViewById(R.id.tabHome)
        tabFeedback = findViewById(R.id.tabFeedback)
        bottomNav = findViewById(R.id.bottomNav)

        // навигация
        tabHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            overridePendingTransition(0, 0) // отключаем анимацию
            finish()
        }
        tabHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0) // отключаем анимацию
            finish()
        }
        tabFeedback.setOnClickListener {
            // уже на Feedback
        }

        // чат
        sendButton = findViewById(R.id.sendButton)
        messageInput = findViewById(R.id.messageInput)
        thanksText = findViewById(R.id.thanksText)
        messagesContainer = findViewById(R.id.messagesContainer)

        // prefill из intent, если пришли из "Задать вопрос"
        val prefill = intent.getStringExtra("prefill_message")
        if (!prefill.isNullOrBlank()) {
            messageInput.setText(prefill)
            messageInput.setSelection(prefill.length)
        }

        // слушаем сообщения из Firestore
        listenMessages()

        // отправка сообщения пользователя
        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessageFromUser(text)
                messageInput.setText("")
                thanksText.visibility = TextView.VISIBLE
                thanksText.text = "Спасибо за обратную связь"
            }
        }

        // клавиатура: просто прячем/показываем нижнюю панель
        setupKeyboardListener()
    }

    private fun sendMessageFromUser(text: String) {
        val data = mapOf(
            "text" to text,
            "from" to "user", // пользователь
            "timestamp" to System.currentTimeMillis()
        )
        messagesRef.add(data)
    }

    // Ты будешь отвечать с ноутбука сообщениями с from = "operator"

    private fun listenMessages() {
        messagesRef
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                refreshMessagesFromSnapshots(snapshots.documents)
            }
    }

    private fun refreshMessagesFromSnapshots(
        docs: List<DocumentSnapshot>
    ) {
        messagesContainer.removeAllViews()
        val inflater = layoutInflater

        // сортировка:
        // 1) по timestamp (старые выше, новые ниже)
        // 2) если timestamp одинаковый — сначала user, потом operator
        val items = docs.mapNotNull { doc ->
            val text = doc.getString("text") ?: return@mapNotNull null
            val from = doc.getString("from") ?: "user"
            val ts = doc.getLong("timestamp") ?: 0L
            Triple(ts, from, text)
        }.sortedWith { a, b ->
            val tsCompare = a.first.compareTo(b.first)
            if (tsCompare != 0) {
                tsCompare
            } else {
                when {
                    a.second == b.second -> 0
                    a.second == "user" -> -1  // user выше
                    else -> 1                 // operator ниже
                }
            }
        }

        for ((_, from, text) in items) {
            // корень item_feedback_message.xml — LinearLayout
            val itemView = inflater.inflate(
                R.layout.item_feedback_message,
                messagesContainer,
                false
            ) as LinearLayout

            val textView: TextView = itemView.findViewById(R.id.messageText)
            textView.text = text

            // меняем выравнивание и фон в зависимости от отправителя
            val lp = itemView.layoutParams as LinearLayout.LayoutParams

            if (from == "user") {
                // сообщение пользователя — справа, голубой пузырь
                lp.gravity = Gravity.END
                itemView.layoutParams = lp
                itemView.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.feedback_bubble_user
                )
                textView.setTextColor(Color.BLACK)
            } else {
                // сообщение оператора — слева, белый пузырь
                lp.gravity = Gravity.START
                itemView.layoutParams = lp
                itemView.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.feedback_bubble_operator
                )
                textView.setTextColor(Color.BLACK)
            }

            messagesContainer.addView(itemView)
        }
    }

    private fun setupKeyboardListener() {
        val root = findViewById<View>(android.R.id.content)

        root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            root.getWindowVisibleDisplayFrame(rect)

            val screenHeight = root.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val keyboardVisible = keypadHeight > screenHeight * 0.15

            // при открытой клавиатуре скрываем нижнюю панель,
            // а остальное поднимает adjustResize
            bottomNav.visibility = if (keyboardVisible) View.GONE else View.VISIBLE
        }
    }
}