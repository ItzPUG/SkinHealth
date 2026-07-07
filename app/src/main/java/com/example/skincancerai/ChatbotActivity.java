package com.example.skincancerai;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private TextInputEditText edtQuestion;
    private MaterialButton btnSend;
    private LinearLayout layoutSuggestions;

    private ChatMessageAdapter adapter;
    private AppChatbotEngine engine;
    private String latestScanResult = "";
    private TextView txtTyping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvChat = findViewById(R.id.rvChat);
        edtQuestion = findViewById(R.id.edtQuestion);
        btnSend = findViewById(R.id.btnSend);
        layoutSuggestions = findViewById(R.id.layoutSuggestions);

        adapter = new ChatMessageAdapter();
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);
        txtTyping = findViewById(R.id.txtTyping);
        engine = new AppChatbotEngine();
        latestScanResult = getIntent().getStringExtra("scan_result");

        if (latestScanResult == null) {
            latestScanResult = "";
        }

        addBot("Xin chào, mình là trợ lý ảo hỗ trợ kỹ thuật của app. Bạn có thể hỏi về kết quả nguy cơ, tái kiểm tra, tái nhắc quét, cách chụp ảnh hoặc khi nào nên đi khám. Mình không thể thay thế bác sĩ đưa ra chẩn đoán");
        renderSuggestions(engine.ask("").suggestions);

        btnSend.setOnClickListener(v -> submitQuestion());
    }

    private void submitQuestion() {
        String q = edtQuestion.getText() == null ? "" : edtQuestion.getText().toString().trim();
        if (q.isEmpty()) return;

        addUser(q);
        edtQuestion.setText("");
        txtTyping.setVisibility(View.VISIBLE);
        ChatbotResponse response = engine.ask(q, latestScanResult);
        addBot(response.answer);
        renderSuggestions(response.suggestions);
        txtTyping.setVisibility(View.GONE);
    }

    private void renderSuggestions(java.util.List<String> suggestions) {
        layoutSuggestions.removeAllViews();

        if (suggestions == null) return;

        for (String s : suggestions) {
            TextView chip = new TextView(this);
            chip.setText(s);
            chip.setTextColor(0xFF2563EB);
            chip.setTextSize(13f);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setBackgroundResource(R.drawable.bg_outline_blue_pill);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, dp(8), dp(8));
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                edtQuestion.setText(s);
                edtQuestion.setSelection(s.length());
                submitQuestion();
            });

            layoutSuggestions.addView(chip);
        }
    }

    private void addUser(String text) {
        adapter.add(new ChatMessage(ChatMessage.ROLE_USER, text));
        scrollToBottom();
    }

    private void addBot(String text) {
        adapter.add(new ChatMessage(ChatMessage.ROLE_BOT, text));
        scrollToBottom();
    }

    private void scrollToBottom() {
        rvChat.post(() -> rvChat.smoothScrollToPosition(Math.max(0, adapter.getItemCount() - 1)));
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
