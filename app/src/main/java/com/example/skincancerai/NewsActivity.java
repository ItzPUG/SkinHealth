package com.example.skincancerai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.viewpager2.widget.ViewPager2;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsActivity extends AppCompatActivity implements NewsFeedAdapter.OnNewsClickListener {

    private static final String TAB_KNOWLEDGE = "KNOWLEDGE";
    private static final String TAB_DISEASE = "DISEASE";

    private RecyclerView recyclerNews;
    private NewsFeedAdapter adapter;

    private ImageView imgFeatured;
    private TextView txtFeaturedCategory;
    private TextView txtFeaturedTitle;
    private TextView txtFeaturedMeta;

    private ImageView imgHighlightArticle;
    private TextView txtHighlightMeta;
    private TextView txtHighlightTitle;

    private TextView tabKnowledge;
    private TextView tabDisease;

    private LinearLayout navHome;
    private LinearLayout navHistory;
    private LinearLayout navHealth;
    private LinearLayout navProfile;
    private FloatingActionButton fabCamera;

    private ViewPager2 viewPagerHighlight;
    private LinearLayout layoutSliderIndicator;
    private NewsHighlightPagerAdapter highlightPagerAdapter;
    private final List<NewsFeedItem> highlightItems = new ArrayList<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final List<NewsFeedItem> knowledgeItems = new ArrayList<>();
    private final List<NewsFeedItem> diseaseItems = new ArrayList<>();

    private String currentTab = TAB_KNOWLEDGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        bindViews();
        setupToolbar();
        setupBottomNav();
        setupRecycler();
        setupTabs();
        seedDiseaseItems();
        loadKnowledgeFromWeb();
        renderCurrentTab();
    }

    private void bindViews() {
        recyclerNews = findViewById(R.id.recyclerNews);

        imgFeatured = findViewById(R.id.imgFeatured);
        txtFeaturedCategory = findViewById(R.id.txtFeaturedCategory);
        txtFeaturedTitle = findViewById(R.id.txtFeaturedTitle);
        txtFeaturedMeta = findViewById(R.id.txtFeaturedMeta);

        imgHighlightArticle = findViewById(R.id.imgHighlightArticle);
        txtHighlightMeta = findViewById(R.id.txtHighlightMeta);
        txtHighlightTitle = findViewById(R.id.txtHighlightTitle);

        tabKnowledge = findViewById(R.id.tabKnowledge);
        tabDisease = findViewById(R.id.tabDisease);

        navHome = findViewById(R.id.navHome);
        navHistory = findViewById(R.id.navHistory);
        navHealth = findViewById(R.id.navHealth);
        navProfile = findViewById(R.id.navProfile);
        fabCamera = findViewById(R.id.fabCamera);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarNews);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> PageTransitionHelper.finishWithAnimation(this));
    }

    private void setupRecycler() {
        recyclerNews.setLayoutManager(new LinearLayoutManager(this));
        recyclerNews.setNestedScrollingEnabled(false);
        adapter = new NewsFeedAdapter(this);
        recyclerNews.setAdapter(adapter);
    }

    private void setupTabs() {
        tabKnowledge.setOnClickListener(v -> {
            currentTab = TAB_KNOWLEDGE;
            renderCurrentTab();
        });

        tabDisease.setOnClickListener(v -> {
            currentTab = TAB_DISEASE;
            renderCurrentTab();
        });
    }

    private void setupBottomNav() {
        if (navHome != null) {
            navHome.setOnClickListener(v ->
                    PageTransitionHelper.navigateWithLoading(
                            this,
                            new Intent(this, MainActivity.class),
                            true
                    )
            );
        }

        if (navHistory != null) {
            navHistory.setOnClickListener(v ->
                    PageTransitionHelper.navigateWithLoading(
                            this,
                            new Intent(this, HistoryActivity.class),
                            true
                    )
            );
        }

        if (navHealth != null) {
            navHealth.setOnClickListener(v ->
                    PageTransitionHelper.navigateWithLoading(
                            this,
                            new Intent(this, MedicalProfileListActivity.class),
                            true
                    )
            );
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v ->
                    PageTransitionHelper.navigateWithLoading(
                            this,
                            new Intent(this, ProfileActivity.class),
                            true
                    )
            );
        }

        if (fabCamera != null) {
            fabCamera.setOnClickListener(v ->
                    PageTransitionHelper.navigateWithLoading(
                            this,
                            new Intent(this, ScanActivity.class)
                    )
            );
        }
    }

    private void loadKnowledgeFromWeb() {
        executor.execute(() -> {
            try {
                List<WebNewsItem> webNews = new VnExpressNewsScraper().fetchSkinNews();
                List<NewsFeedItem> converted = new ArrayList<>();

                for (WebNewsItem item : webNews) {
                    converted.add(new NewsFeedItem(
                            item.title,
                            item.summary,
                            item.content,
                            item.dateText,
                            item.category,
                            item.articleUrl,
                            item.imageUrl,
                            0
                    ));
                }

                runOnUiThread(() -> {
                    knowledgeItems.clear();
                    knowledgeItems.addAll(converted);

                    if (TAB_KNOWLEDGE.equals(currentTab)) {
                        renderCurrentTab();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (knowledgeItems.isEmpty()) {
                        seedFallbackKnowledgeItems();
                        if (TAB_KNOWLEDGE.equals(currentTab)) {
                            renderCurrentTab();
                        }
                    }
                    Utils.toast(this, "Không tải được tin web, đang dùng dữ liệu dự phòng");
                });
            }
        });
    }

    private void seedFallbackKnowledgeItems() {
        knowledgeItems.clear();

        knowledgeItems.add(new NewsFeedItem(
                "Những dấu hiệu da cần được theo dõi sớm",
                "Một số thay đổi ở da nên được quan sát kỹ hơn theo thời gian.",
                "Nếu một vùng da thay đổi rõ về màu sắc, kích thước, bờ viền hoặc kéo dài không cải thiện, bạn nên theo dõi sát hơn và đi khám da liễu khi cần.",
                "24/04/2026",
                "Kiến thức da liễu",
                "",
                "",
                R.drawable.onboard_health_2
        ));

        knowledgeItems.add(new NewsFeedItem(
                "Cách chăm sóc da nhạy cảm đúng cách",
                "Ưu tiên chăm sóc dịu nhẹ và hạn chế kích thích không cần thiết.",
                "Da nhạy cảm cần quy trình chăm sóc đơn giản, tránh chà xát mạnh và nên theo dõi phản ứng khi đổi sản phẩm.",
                "23/04/2026",
                "Chăm sóc da",
                "",
                "",
                R.drawable.onboard_health_1
        ));

        knowledgeItems.add(new NewsFeedItem(
                "Khi nào nên tái kiểm tra vùng da đã từng quét",
                "Tái kiểm tra giúp theo dõi diễn tiến thay vì chỉ nhìn một thời điểm.",
                "Nếu vùng da thay đổi sau vài ngày hoặc vài tuần, bạn nên lưu lại và so sánh theo thời gian để đánh giá tốt hơn.",
                "22/04/2026",
                "Theo dõi da",
                "",
                "",
                R.drawable.onboard_health_2
        ));
    }

    private void seedDiseaseItems() {
        diseaseItems.clear();

        diseaseItems.add(new NewsFeedItem(
                "Nốt ruồi bất thường",
                "Một số thay đổi ở nốt ruồi cần được theo dõi kỹ hơn theo thời gian.",
                "Nốt ruồi có thể lành tính, nhưng nếu thay đổi rõ về màu sắc, kích thước, bờ viền hoặc hình dạng, bạn nên theo dõi sát hơn. Nếu thay đổi liên tục hoặc dễ chảy máu, nên đi khám bác sĩ da liễu.\n\nLưu ý: Nội dung chỉ mang tính tham khảo, không thay thế chẩn đoán của bác sĩ.",
                "Tham khảo",
                "Thông tin bệnh",
                "",
                "",
                R.drawable.not_ruoi
        ));

        diseaseItems.add(new NewsFeedItem(
                "Da kích ứng",
                "Da có thể đỏ, rát hoặc ngứa do mỹ phẩm, thời tiết hoặc ma sát.",
                "Da kích ứng thường biểu hiện bằng đỏ da, khô rát hoặc bong nhẹ. Bạn nên theo dõi vùng da này, hạn chế tác động mạnh và đi khám nếu vùng da ngày càng nặng hơn hoặc kéo dài không cải thiện.",
                "Tham khảo",
                "Thông tin bệnh",
                "",
                "",
                R.drawable.da_kich_ung
        ));

        diseaseItems.add(new NewsFeedItem(
                "Da nhạy cảm",
                "Da nhạy cảm thường dễ phản ứng với mỹ phẩm, nắng nóng hoặc môi trường.",
                "Da nhạy cảm dễ châm chích, đỏ hoặc ngứa khi thay đổi sản phẩm hoặc thời tiết. Bạn nên tối giản quy trình chăm sóc và quan sát phản ứng của da theo thời gian.",
                "Tham khảo",
                "Thông tin bệnh",
                "",
                "",
                R.drawable.da_nhay_cam
        ));

        diseaseItems.add(new NewsFeedItem(
                "Mụn viêm",
                "Mụn viêm thường đỏ, sưng và có thể gây đau khi chạm vào.",
                "Mụn viêm có thể để lại thâm hoặc sẹo nếu tự nặn không đúng cách. Bạn nên theo dõi mức độ viêm và đi khám nếu mụn kéo dài hoặc lan rộng.",
                "Tham khảo",
                "Thông tin bệnh",
                "",
                "",
                R.drawable.mun_viem
        ));

        diseaseItems.add(new NewsFeedItem(
                "Nấm da",
                "Nấm da có thể gây ngứa, bong vảy hoặc xuất hiện vùng da đổi màu.",
                "Nấm da là tình trạng thường gặp ở các vùng ẩm. Nếu vùng da có biểu hiện kéo dài, lan rộng hoặc tái phát nhiều lần, bạn nên đi khám để xác định đúng nguyên nhân.",
                "Tham khảo",
                "Thông tin bệnh",
                "",
                "",
                R.drawable.nam_da
        ));

        diseaseItems.add(new NewsFeedItem(
                "Viêm da cơ địa",
                "Viêm da cơ địa thường gây khô da, ngứa và tái phát theo từng đợt.",
                "Viêm da cơ địa có thể tái phát theo thời tiết, căng thẳng hoặc tiếp xúc kích thích. Nếu vùng da ngứa nhiều, rỉ dịch hoặc lan rộng, nên đi khám sớm.",
                "Tham khảo",
                "Thông tin bệnh",
                "",
                "",
                R.drawable.viem_da_co_dia
        ));
    }

    private void renderCurrentTab() {
        boolean isKnowledge = TAB_KNOWLEDGE.equals(currentTab);
        updateTabStyles(isKnowledge);

        List<NewsFeedItem> source = isKnowledge ? knowledgeItems : diseaseItems;

        if (source.isEmpty()) {
            txtFeaturedCategory.setText(isKnowledge ? "Kiến thức da liễu" : "Thông tin bệnh");
            txtFeaturedTitle.setText("Đang tải nội dung...");
            txtFeaturedMeta.setText("");
            txtHighlightMeta.setText("");
            txtHighlightTitle.setText("Vui lòng chờ trong giây lát");
            adapter.setItems(new ArrayList<>());
            return;
        }

        NewsFeedItem featured = source.get(0);
        bindFeatured(featured);

        if (source.size() > 1) {
            bindHighlight(source.get(1));
        } else {
            bindHighlight(featured);
        }

        List<NewsFeedItem> others = new ArrayList<>();
        if (source.size() > 2) {
            others.addAll(source.subList(2, source.size()));
        }

        adapter.setItems(others);
    }

    private void updateTabStyles(boolean isKnowledge) {
        if (isKnowledge) {
            tabKnowledge.setBackgroundResource(R.drawable.bg_tab_news_active);
            tabKnowledge.setTextColor(getColor(android.R.color.white));

            tabDisease.setBackground(null);
            tabDisease.setTextColor(0xFF334155);
        } else {
            tabDisease.setBackgroundResource(R.drawable.bg_tab_news_active);
            tabDisease.setTextColor(getColor(android.R.color.white));

            tabKnowledge.setBackground(null);
            tabKnowledge.setTextColor(0xFF334155);
        }
    }

    private void bindFeatured(NewsFeedItem item) {
        txtFeaturedCategory.setText(item.category);
        txtFeaturedTitle.setText(item.title);
        txtFeaturedMeta.setText(item.dateText);

        if (item.hasRemoteImage()) {
            Glide.with(this)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.onboard_health_2)
                    .error(R.drawable.onboard_health_2)
                    .centerCrop()
                    .into(imgFeatured);
        } else {
            imgFeatured.setImageResource(item.imageRes != 0 ? item.imageRes : R.drawable.onboard_health_2);
        }

        View cardFeatured = findViewById(R.id.cardFeatured);
        if (cardFeatured != null) {
            cardFeatured.setOnClickListener(v -> onNewsClick(item));
        }
    }

    private void bindHighlight(NewsFeedItem item) {
        txtHighlightMeta.setText(item.dateText);
        txtHighlightTitle.setText(item.title);

        if (item.hasRemoteImage()) {
            Glide.with(this)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.onboard_health_1)
                    .error(R.drawable.onboard_health_1)
                    .centerCrop()
                    .into(imgHighlightArticle);
        } else {
            imgHighlightArticle.setImageResource(item.imageRes != 0 ? item.imageRes : R.drawable.onboard_health_1);
        }

        View cardHighlight = findViewById(R.id.cardHighlightArticle);
        if (cardHighlight != null) {
            cardHighlight.setOnClickListener(v -> onNewsClick(item));
        }
    }

    @Override
    public void onNewsClick(NewsFeedItem item) {
        if (item == null) return;

        Intent intent = new Intent(this, NewsWebDetailActivity.class);
        intent.putExtra("title", item.title);
        intent.putExtra("summary", item.summary);
        intent.putExtra("content", item.content);
        intent.putExtra("date", item.dateText);
        intent.putExtra("category", item.category);
        intent.putExtra("imageUrl", item.imageUrl);
        intent.putExtra("imageRes", item.imageRes);
        intent.putExtra("articleUrl", item.articleUrl);

        PageTransitionHelper.navigateWithLoading(this, intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
