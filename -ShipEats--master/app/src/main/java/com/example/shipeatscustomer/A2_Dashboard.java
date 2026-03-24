package com.example.shipeatscustomer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.*;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class A2_Dashboard extends AppCompatActivity {

    private TextView tvTotalOrders, tvDailySales, tvMenuItems, tvLowStock;
    private TextView tvOrdersSummary, tvSalesSummary, tvMenuSummary, tvStockSummary;
    private CardView cardOrders, cardSales, cardMenu, cardStock;
    private BarChart barChartWeekly;
    private LineChart lineChartMonthly;
    private PieChart stockPieChart;
    private TabLayout tabLayout;

    // ── Firebase ──
    private DatabaseReference ordersRef, foodRef, menuRef;
    private boolean isInitialLoadFinished = false;

    // ── Data for PDF ──
    private final List<AdminOrderModel> allOrders = new ArrayList<>();

    // ── Date range from picker ──
    private long selectedStartMillis = 0;
    private long selectedEndMillis   = 0;

    // ══════════════════════════════════════════════════════════════
    //  DATE HELPERS
    //  Firebase stores: "19 Mar 2026, 11:20"  (dd MMM yyyy, HH:mm)
    // ══════════════════════════════════════════════════════════════
    private static final SimpleDateFormat DB_FMT   =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final SimpleDateFormat DAY_FMT  =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat MON_FMT  =
            new SimpleDateFormat("yyyy-MM",    Locale.getDefault());

    /** "19 Mar 2026, 11:20"  →  "2026-03-19"  (null on failure) */
    private static String toDayKey(String raw) {
        if (raw == null) return null;
        try {
            Date d = DB_FMT.parse(raw);
            return d != null ? DAY_FMT.format(d) : null;
        } catch (ParseException e) {
            // already yyyy-MM-dd?
            return (raw.length() >= 10 && raw.charAt(4) == '-') ? raw.substring(0, 10) : null;
        }
    }

    /** "19 Mar 2026, 11:20"  →  "2026-03"  (null on failure) */
    private static String toMonthKey(String raw) {
        String d = toDayKey(raw);
        return (d != null && d.length() >= 7) ? d.substring(0, 7) : null;
    }

    /** "19 Mar 2026, 11:20"  →  Date object  (null on failure) */
    private static Date toDate(String raw) {
        if (raw == null) return null;
        try { return DB_FMT.parse(raw); } catch (ParseException e) { return null; }
    }

    // Launchers
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    ok -> { if (ok) Toast.makeText(this, "Notifications Enabled", Toast.LENGTH_SHORT).show(); });

    private final ActivityResultLauncher<String> createPdfLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"),
                    uri -> { if (uri != null) writePdfToUri(uri); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_a2_dashboard);

        NotificationHelper.createNotificationChannel(this);
        checkNotificationPermission();
        initUI();

        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        foodRef   = FirebaseDatabase.getInstance().getReference("food_items");
        menuRef = FirebaseDatabase.getInstance().getReference("menu_items");

        loadStatsAndCharts();
        setupOrderRealtimeListener();
        setupCardClicks();
        setupTabs();
        setupBottomNav();
        setupHeader();

        highlightCurrentTab(findViewById(R.id.dashboard_icon), findViewById(R.id.dashboard_text));
        findViewById(R.id.btn_print_report).setOnClickListener(v -> showDateRangePicker());
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED)
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void setupOrderRealtimeListener() {
        ordersRef.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                if (!isInitialLoadFinished) return;

                AdminOrderModel order = snapshot.getValue(AdminOrderModel.class);
                if (order != null && "Pending".equalsIgnoreCase(order.status)) {

                    String myId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();

                    // 🛡️ THE FIX: Only run this if the logged-in user is an Admin
                    FirebaseDatabase.getInstance().getReference("Admins").child(myId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot adminSnap) {
                                    if (adminSnap.exists()) {
                                        // ✅ ONLY THE ADMIN SEES THIS NOW
                                        NotificationHelper.triggerSystemNotification(
                                                A2_Dashboard.this,
                                                "🔔 New Order #" + snapshot.getKey().substring(Math.max(0, snapshot.getKey().length() - 5)),
                                                "Customer " + order.customerName + " just placed an order.",
                                                "order"
                                        );

                                        NotificationHelper.sendNotification(
                                                myId,
                                                "New Order Received",
                                                "Order from " + order.customerName + " needs your attention.",
                                                "order",
                                                snapshot.getKey()
                                        );
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void initUI() {
        tvTotalOrders    = findViewById(R.id.tv_total_orders);
        tvDailySales     = findViewById(R.id.tv_daily_sales);
        tvMenuItems      = findViewById(R.id.tv_menu_items);
        tvLowStock       = findViewById(R.id.tv_low_stock);
        tvOrdersSummary  = findViewById(R.id.tv_orders_summary);
        tvSalesSummary   = findViewById(R.id.tv_sales_summary);
        tvMenuSummary    = findViewById(R.id.tv_menu_summary);
        tvStockSummary   = findViewById(R.id.tv_stock_summary);
        cardOrders       = findViewById(R.id.card_total_orders);
        cardSales        = findViewById(R.id.card_daily_sales);
        cardMenu         = findViewById(R.id.card_menu_items);
        cardStock        = findViewById(R.id.card_low_stock);
        barChartWeekly   = findViewById(R.id.barChartWeekly);
        lineChartMonthly = findViewById(R.id.lineChartMonthly);
        stockPieChart    = findViewById(R.id.stockPieChart);
        tabLayout        = findViewById(R.id.tabLayoutSales);
    }

    private void setupHeader() {
        View notifBtn = findViewById(R.id.admin_notification_btn);
        if (notifBtn != null)
            notifBtn.setOnClickListener(v -> AdminDialogHelper.showNotificationsDialog(this));
    }

    // ══════════════════════════════════════════════════════════════
    //  STEP 1 + 2 — Real Firebase data, correct date parsing, charts
    // ══════════════════════════════════════════════════════════════
    private void loadStatsAndCharts() {
        String today     = DAY_FMT.format(new Date());
        Calendar cal     = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String yesterday = DAY_FMT.format(cal.getTime());

        // ── ORDERS ──────────────────────────────────────────────
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allOrders.clear();

                int    ordersToday = 0,    ordersYest = 0;
                double revToday    = 0.0,  revYest    = 0.0;

                LinkedHashMap<String, Float> weeklyMap  = buildLast7DaysMap();
                LinkedHashMap<String, Float> monthlyMap = buildLast12MonthsMap();

                for (DataSnapshot ds : snapshot.getChildren()) {

                    // Parse totalPrice — handles "RM 19.66" / double / long
                    double price = 0.0;
                    Object po = ds.child("totalPrice").getValue();
                    if (po instanceof Double)    price = (Double) po;
                    else if (po instanceof Long) price = ((Long) po).doubleValue();
                    else if (po instanceof String) {
                        try {
                            String c = ((String) po).replaceAll("[^0-9.]", "");
                            if (!c.isEmpty()) price = Double.parseDouble(c);
                        } catch (Exception ignored) {}
                    }

                    // Parse "19 Mar 2026, 11:20" correctly
                    String rawDate  = ds.child("date").getValue(String.class);
                    String dayKey   = toDayKey(rawDate);
                    String monKey   = toMonthKey(rawDate);

                    if (today.equals(dayKey))     { ordersToday++; revToday += price; }
                    else if (yesterday.equals(dayKey)) { ordersYest++;  revYest  += price; }

                    if (dayKey != null && weeklyMap.containsKey(dayKey))
                        weeklyMap.put(dayKey, weeklyMap.get(dayKey) + (float) price);

                    if (monKey != null && monthlyMap.containsKey(monKey))
                        monthlyMap.put(monKey, monthlyMap.get(monKey) + (float) price);

                    AdminOrderModel order = ds.getValue(AdminOrderModel.class);
                    if (order != null) { order.orderId = ds.getKey(); allOrders.add(order); }
                }

                tvTotalOrders.setText(String.valueOf(ordersToday));
                tvDailySales .setText("RM " + String.format(Locale.getDefault(), "%.2f", revToday));
                updateTrend(tvOrdersSummary, ordersToday, ordersYest);
                updateTrend(tvSalesSummary,  revToday,    revYest);
                updateBarChart(weeklyMap);
                updateLineChart(monthlyMap);
                isInitialLoadFinished = true;
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        // ── FOOD ITEMS (stock cards + pie chart) ────────────────
        // Reads from food_items node
        // Fields used: quantity, status (Available / Low Stock / Sold Out)
        foodRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int ok = 0, low = 0, out = 0;
                int total = (int) snapshot.getChildrenCount();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    // Read quantity as Long, Integer, or String
                    Object qo  = ds.child("quantity").getValue();
                    int    qty = 0;
                    if (qo instanceof Long)    qty = ((Long) qo).intValue();
                    else if (qo instanceof Integer) qty = (Integer) qo;
                    else if (qo instanceof String) {
                        try { qty = Integer.parseInt((String) qo); } catch (Exception ignored) {}
                    }

                    if      (qty <= 0) out++;
                    else if (qty <= 5) low++;
                    else               ok++;
                }

                tvLowStock   .setText(String.valueOf(low));
                tvStockSummary.setText(out + " items currently sold out");
                updatePieChart(ok, low, out);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        // ── MENU ITEMS ─────────────────────────────────────────────
        menuRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int published = 0;
                int total = (int) snapshot.getChildrenCount();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Object pub = ds.child("published").getValue();
                    // handles Boolean true or String "true"
                    if (Boolean.TRUE.equals(pub) || "true".equals(String.valueOf(pub))) {
                        published++;
                    }
                }

                tvMenuItems.setText(String.valueOf(published));
                tvMenuSummary.setText(published + " of " + total + " items published");
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private LinkedHashMap<String, Float> buildLast7DaysMap() {
        LinkedHashMap<String, Float> map = new LinkedHashMap<>();
        Calendar c = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            Calendar d = (Calendar) c.clone(); d.add(Calendar.DATE, -i);
            map.put(DAY_FMT.format(d.getTime()), 0f);
        }
        return map;
    }

    private LinkedHashMap<String, Float> buildLast12MonthsMap() {
        LinkedHashMap<String, Float> map = new LinkedHashMap<>();
        Calendar c = Calendar.getInstance();
        for (int i = 11; i >= 0; i--) {
            Calendar m = (Calendar) c.clone(); m.add(Calendar.MONTH, -i);
            map.put(MON_FMT.format(m.getTime()), 0f);
        }
        return map;
    }

    // ══════════════════════════════════════════════════════════════
    //  Charts
    // ══════════════════════════════════════════════════════════════
    private void updateBarChart(LinkedHashMap<String, Float> map) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        String[] labels = new String[map.size()];
        SimpleDateFormat lf = new SimpleDateFormat("EEE", Locale.getDefault());
        int i = 0;
        for (Map.Entry<String, Float> e : map.entrySet()) {
            entries.add(new BarEntry(i, e.getValue()));
            try { Date d = DAY_FMT.parse(e.getKey()); labels[i] = d != null ? lf.format(d) : e.getKey(); }
            catch (Exception ex) { labels[i] = e.getKey(); }
            i++;
        }
        BarDataSet ds = new BarDataSet(entries, "Sales (RM)");
        ds.setColor(Color.parseColor("#032565"));
        ds.setValueTextColor(Color.parseColor("#032565"));
        ds.setValueTextSize(9f);
        BarData data = new BarData(ds); data.setBarWidth(0.55f);
        barChartWeekly.setData(data);
        barChartWeekly.setFitBars(true);
        barChartWeekly.getDescription().setEnabled(false);
        barChartWeekly.getLegend().setEnabled(false);
        barChartWeekly.setDrawGridBackground(false);
        barChartWeekly.setExtraBottomOffset(5f);
        XAxis x = barChartWeekly.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f); x.setDrawGridLines(false);
        x.setTextColor(Color.parseColor("#555555")); x.setTextSize(11f);
        YAxis left = barChartWeekly.getAxisLeft();
        left.setGridColor(Color.parseColor("#EEEEEE"));
        left.setTextColor(Color.parseColor("#555555")); left.setAxisMinimum(0f);
        barChartWeekly.getAxisRight().setEnabled(false);
        barChartWeekly.animateY(1000);
        barChartWeekly.invalidate();
    }

    private void updateLineChart(LinkedHashMap<String, Float> map) {
        ArrayList<Entry> entries = new ArrayList<>();
        String[] labels = new String[map.size()];
        SimpleDateFormat lf = new SimpleDateFormat("MMM", Locale.getDefault());
        int i = 0;
        for (Map.Entry<String, Float> e : map.entrySet()) {
            entries.add(new Entry(i, e.getValue()));
            try { Date d = MON_FMT.parse(e.getKey()); labels[i] = d != null ? lf.format(d) : e.getKey(); }
            catch (Exception ex) { labels[i] = e.getKey(); }
            i++;
        }
        LineDataSet ds = new LineDataSet(entries, "Revenue (RM)");
        ds.setColor(Color.parseColor("#FDB02C"));
        ds.setCircleColor(Color.parseColor("#032565"));
        ds.setCircleRadius(4f); ds.setLineWidth(2.5f);
        ds.setValueTextSize(9f); ds.setValueTextColor(Color.parseColor("#032565"));
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setDrawFilled(true); ds.setFillColor(Color.parseColor("#FDB02C")); ds.setFillAlpha(25);
        lineChartMonthly.setData(new LineData(ds));
        lineChartMonthly.getDescription().setEnabled(false);
        lineChartMonthly.getLegend().setEnabled(false);
        lineChartMonthly.setDrawGridBackground(false);
        lineChartMonthly.setExtraBottomOffset(5f);
        XAxis x = lineChartMonthly.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setGranularity(1f); x.setDrawGridLines(false);
        x.setTextColor(Color.parseColor("#555555")); x.setTextSize(10f);
        YAxis left = lineChartMonthly.getAxisLeft();
        left.setGridColor(Color.parseColor("#EEEEEE"));
        left.setTextColor(Color.parseColor("#555555")); left.setAxisMinimum(0f);
        lineChartMonthly.getAxisRight().setEnabled(false);
        lineChartMonthly.animateX(1000);
        lineChartMonthly.invalidate();
    }

    private void updateTrend(TextView tv, double today, double yest) {
        if (yest == 0) { tv.setText("New Record Today"); tv.setTextColor(Color.GRAY); return; }
        double diff = ((today - yest) / yest) * 100;
        tv.setText(String.format(Locale.getDefault(), "%s%.1f%% from yesterday", diff >= 0 ? "+" : "", diff));
        tv.setTextColor(diff >= 0 ? Color.parseColor("#4CAF50") : Color.RED);
    }

    private void updatePieChart(int ok, int low, int out) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (ok  > 0) entries.add(new PieEntry(ok,  "Available"));
        if (low > 0) entries.add(new PieEntry(low, "Low Stock"));
        if (out > 0) entries.add(new PieEntry(out, "Sold Out"));
        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#FDB02C"), Color.parseColor("#E53935"));
        set.setValueTextColor(Color.WHITE); set.setValueTextSize(12f); set.setSliceSpace(3f);
        stockPieChart.setData(new PieData(set));
        stockPieChart.setCenterText("Stock\nStatus");
        stockPieChart.setCenterTextSize(13f);
        stockPieChart.setCenterTextColor(Color.parseColor("#032565"));
        stockPieChart.setHoleRadius(42f); stockPieChart.setTransparentCircleRadius(48f);
        stockPieChart.getDescription().setEnabled(false);
        stockPieChart.animateXY(800, 800); stockPieChart.invalidate();
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                barChartWeekly  .setVisibility(tab.getPosition() == 0 ? View.VISIBLE : View.GONE);
                lineChartMonthly.setVisibility(tab.getPosition() == 1 ? View.VISIBLE : View.GONE);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupCardClicks() {
        cardOrders.setOnClickListener(v -> startActivity(new Intent(this, A4_CustomerOrderActivity.class)));
        cardSales .setOnClickListener(v -> startActivity(new Intent(this, A4_CustomerOrderActivity.class)));
        cardMenu  .setOnClickListener(v -> startActivity(new Intent(this, A5_MenuManagementActivity.class)));
        cardStock .setOnClickListener(v -> startActivity(new Intent(this, A3_Inventory_Management.class)));
    }

    // ══════════════════════════════════════════════════════════════
    //  STEP 4 — Date picker themed to #032565
    // ══════════════════════════════════════════════════════════════
    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select Report Period")
                        .setTheme(R.style.CustomDatePickerTheme)
                        .build();
        picker.show(getSupportFragmentManager(), "RANGE_PICKER");
        picker.addOnPositiveButtonClickListener(selection -> {
            selectedStartMillis = selection.first;
            // Include the full end day up to 23:59:59
            selectedEndMillis   = selection.second + (24 * 60 * 60 * 1000L) - 1;
            String s = DAY_FMT.format(new Date(selectedStartMillis));
            String e = DAY_FMT.format(new Date(selectedEndMillis));
            createPdfLauncher.launch("ShipEats_Report_" + s + "_to_" + e + ".pdf");
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  STEP 3 — PDF
    // ══════════════════════════════════════════════════════════════
    private void writePdfToUri(Uri uri) {
        String startLabel = DAY_FMT.format(new Date(selectedStartMillis));
        String endLabel   = DAY_FMT.format(new Date(selectedEndMillis));

        // Aggregate sold per product name across filtered orders
        // cartItem.getFoodItem().getName()  — food name
        // cartItem.getFoodItem().getPrice() — unit price
        // cartItem.getQuantity()            — qty at CartItem level
        LinkedHashMap<String, Integer> soldQtyMap   = new LinkedHashMap<>();
        LinkedHashMap<String, Double>  soldRevMap   = new LinkedHashMap<>();
        LinkedHashMap<String, Double>  unitPriceMap = new LinkedHashMap<>();

        // Fallback: when orderItems is null, use order.items string
        List<String[]> fallbackRows = new ArrayList<>(); // {orderId, itemsStr, totalPrice}

        int    totalOrders = 0;
        double grandTotal  = 0.0;
        boolean hasDetail  = false;

        for (AdminOrderModel order : allOrders) {
            // Filter by date range using millis comparison
            Date od = toDate(order.date);
            if (od == null) continue;
            if (od.getTime() < selectedStartMillis || od.getTime() > selectedEndMillis) continue;

            totalOrders++;
            double orderTotal = parsePriceStr(order.totalPrice);
            grandTotal += orderTotal;

            if (order.orderItems != null && !order.orderItems.isEmpty()) {
                hasDetail = true;
                for (CartItem ci : order.orderItems) {
                    if (ci == null || ci.getFoodItem() == null) continue;
                    String name  = ci.getFoodItem().getName();
                    double price = ci.getFoodItem().getPrice();
                    // ★ quantity is at CartItem level (sibling of foodItem in Firebase)
                    int    qty   = Math.max(ci.getQuantity(), 1);
                    if (name == null || name.isEmpty()) name = "Unknown Item";

                    soldQtyMap .put(name, soldQtyMap .getOrDefault(name, 0)   + qty);
                    soldRevMap .put(name, soldRevMap .getOrDefault(name, 0.0) + price * qty);
                    if (!unitPriceMap.containsKey(name)) unitPriceMap.put(name, price);
                }
            } else {
                // Fallback: use items string e.g. "2x Veggie Sandwich"
                fallbackRows.add(new String[]{
                        order.orderId != null ? order.orderId : "—",
                        order.items   != null ? order.items   : "—",
                        String.format(Locale.getDefault(), "%.2f", orderTotal)
                });
            }
        }

        // ── Build PDF ───────────────────────────────────────────
        PdfDocument doc      = new PdfDocument();
        PdfDocument.Page page = doc.startPage(
                new PdfDocument.PageInfo.Builder(595, 842, 1).create());
        Canvas canvas = page.getCanvas();
        Paint  p      = new Paint(Paint.ANTI_ALIAS_FLAG);

        final int NAVY    = Color.parseColor("#032565");
        final int GOLD    = Color.parseColor("#FDB02C");
        final int LIGHT   = Color.parseColor("#EFF3FB");
        final int MIDGRAY = Color.parseColor("#888888");
        final int WHITE   = Color.WHITE;
        final int BLACK   = Color.BLACK;

        // ── Header bar ─────────────────────────────────────────
        p.setColor(NAVY); p.setStyle(Paint.Style.FILL);
        canvas.drawRect(0f, 0f, 595f, 115f, p);

        p.setColor(GOLD);                               // gold top stripe
        canvas.drawRect(0f, 0f, 595f, 5f, p);

        // Logo circle background
        p.setColor(GOLD);
        canvas.drawCircle(52f, 62f, 32f, p);

        // Draw logo image from drawable
        try {
            Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.shipeats_logo2);
            if (logoBitmap != null) {
                // Scale logo to fit inside the circle (56x56 dp centered in circle)
                Bitmap scaled = Bitmap.createScaledBitmap(logoBitmap, 56, 56, true);
                // Center it: circle center is (52, 62), so top-left = (52-28, 62-28) = (24, 34)
                canvas.drawBitmap(scaled, 24f, 34f, p);
                scaled.recycle();
                logoBitmap.recycle();
            }
        } catch (Exception e) {
            // Fallback to text if image fails
            p.setColor(NAVY);
            p.setTextSize(11f);
            p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("SHIP", 33f, 58f, p);
            canvas.drawText("EATS", 33f, 73f, p);
        }

        p.setColor(WHITE);                              // company name
        p.setTextSize(22f); p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("ShipEats", 96f, 52f, p);
        p.setTextSize(10f); p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        p.setColor(Color.parseColor("#99AABB"));
        canvas.drawText("Official Sales Report  •  Confidential", 96f, 70f, p);
        canvas.drawText("Admin Dashboard Export", 96f, 86f, p);

        p.setColor(GOLD); p.setTextSize(11f);           // right block
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("SALES REPORT", 388f, 50f, p);
        p.setColor(WHITE); p.setTextSize(9f);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Period:    " + startLabel + "  –  " + endLabel, 388f, 67f, p);
        canvas.drawText("Generated: " + DAY_FMT.format(new Date()), 388f, 82f, p);

        int y = 132;

        // ── Summary boxes ──────────────────────────────────────
        pdfSectionTitle(canvas, p, "EXECUTIVE SUMMARY", NAVY, GOLD, y);
        y += 18;

        String[][] summary = {
                {"Total Orders",  String.valueOf(totalOrders)},
                {"Total Revenue", String.format(Locale.getDefault(), "RM %.2f", grandTotal)},
                {"Menu Items",    tvMenuItems.getText().toString()},
                {"Low Stock",     tvLowStock.getText().toString() + " items"},
        };
        float bw = 120f;
        for (int i = 0; i < 4; i++) {
            float bx = 40f + i * (bw + 6f);
            p.setColor(Color.parseColor("#DDDDDD")); p.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(new RectF(bx+2,y+2,bx+bw+2,y+62), 8f, 8f, p);
            p.setColor(WHITE);
            canvas.drawRoundRect(new RectF(bx,y,bx+bw,y+60), 8f, 8f, p);
            p.setColor(NAVY);
            canvas.drawRoundRect(new RectF(bx,y,bx+bw,y+6), 4f, 4f, p);
            p.setColor(MIDGRAY); p.setTextSize(8f);
            p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            canvas.drawText(summary[i][0], bx+8f, y+23f, p);
            p.setColor(NAVY); p.setTextSize(i == 1 ? 10.5f : 13f);
            p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(summary[i][1], bx+8f, y+48f, p);
        }
        y += 80;

        // ── Product table ──────────────────────────────────────
        pdfSectionTitle(canvas, p, "PRODUCT BREAKDOWN", NAVY, GOLD, y);
        y += 18;

        final float cNo = 42f, cName = 68f, cPrice = 330f, cSold = 415f, cTotal = 468f;

        // Header row
        p.setColor(NAVY); p.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(40f, y, 555f, y+26f), 5f, 5f, p);
        p.setColor(WHITE); p.setTextSize(9.5f);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("No.",          cNo,    y+17f, p);
        canvas.drawText("Product Name", cName,  y+17f, p);
        canvas.drawText("Unit Price",   cPrice, y+17f, p);
        canvas.drawText("Sold",         cSold,  y+17f, p);
        canvas.drawText("Total (RM)",   cTotal, y+17f, p);
        y += 26;

        if (hasDetail) {
            // Per-product rows using CartItem → foodItem data
            int rowNum = 1;
            for (String name : soldQtyMap.keySet()) {
                if (y > 755) break;
                int    qty   = soldQtyMap.get(name);
                double rev   = soldRevMap.getOrDefault(name, 0.0);
                double unit  = unitPriceMap.getOrDefault(name, 0.0);
                pdfTableRow(canvas, p, rowNum, name, unit, rev, qty,
                        rowNum % 2 == 0, LIGHT, BLACK, MIDGRAY,
                        cNo, cName, cPrice, cSold, cTotal, y);
                y += 24; rowNum++;
            }
        } else {
            // Fallback: uses order.items string "2x Veggie Sandwich"
            int rowNum = 1;
            for (String[] row : fallbackRows) {
                if (y > 755) break;
                double total = parsePriceStr(row[2]);
                pdfTableRow(canvas, p, rowNum,
                        row[0] + "  –  " + row[1],   // "A0062 – 2x Veggie Sandwich"
                        total, total, 1,
                        rowNum % 2 == 0, LIGHT, BLACK, MIDGRAY,
                        cNo, cName, cPrice, cSold, cTotal, y);
                y += 24; rowNum++;
            }
        }

        // Grand total row
        y += 4;
        p.setColor(NAVY); p.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(40f, y, 555f, y+28f), 5f, 5f, p);
        p.setColor(GOLD); p.setTextSize(10f);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("GRAND TOTAL", cName, y+18f, p);
        p.setColor(WHITE);
        canvas.drawText(String.format(Locale.getDefault(), "RM %.2f", grandTotal), cTotal, y+18f, p);

        // ── Footer ─────────────────────────────────────────────
        p.setColor(GOLD); p.setStrokeWidth(2f);
        canvas.drawLine(40f, 808f, 555f, 808f, p);
        p.setColor(NAVY); p.setStyle(Paint.Style.FILL);
        canvas.drawRect(0f, 818f, 595f, 842f, p);
        p.setColor(WHITE); p.setTextSize(8f);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Authorized by: ShipEats Admin  •  System-generated  •  Confidential", 40f, 832f, p);
        p.setColor(GOLD);
        canvas.drawText("Page 1", 528f, 832f, p);

        doc.finishPage(page);

        try {
            OutputStream out = getContentResolver().openOutputStream(uri);
            if (out != null) { doc.writeTo(out); out.close(); }
            Toast.makeText(this, "PDF Saved!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally { doc.close(); }
    }

    // ── PDF helpers ──────────────────────────────────────────────
    private void pdfSectionTitle(Canvas cv, Paint p, String title,
                                 int navy, int gold, int y) {
        p.setColor(navy); p.setTextSize(11f);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setStyle(Paint.Style.FILL);
        cv.drawText(title, 40f, y, p);
        p.setColor(gold); p.setStrokeWidth(1.5f); p.setStyle(Paint.Style.STROKE);
        cv.drawLine(40f, y+4f, 555f, y+4f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void pdfTableRow(Canvas cv, Paint p,
                             int num, String name, double unit, double total, int qty,
                             boolean alt, int lightBg, int text, int gray,
                             float cNo, float cName, float cPrice,
                             float cSold, float cTotal, int y) {
        if (alt) {
            p.setColor(lightBg); p.setStyle(Paint.Style.FILL);
            cv.drawRect(40f, y, 555f, y+24f, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(gray); p.setTextSize(8.5f);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        cv.drawText(String.valueOf(num), cNo, y+16f, p);
        p.setColor(text); p.setTextSize(9f);
        String dn = name.length() > 38 ? name.substring(0, 38) + "…" : name;
        cv.drawText(dn, cName, y+16f, p);
        cv.drawText(String.format(Locale.getDefault(), "%.2f", unit),  cPrice, y+16f, p);
        cv.drawText(String.valueOf(qty),                                cSold,  y+16f, p);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        cv.drawText(String.format(Locale.getDefault(), "%.2f", total), cTotal, y+16f, p);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        p.setColor(Color.parseColor("#DDDDDD")); p.setStrokeWidth(0.5f);
        cv.drawLine(40f, y+24f, 555f, y+24f, p);
    }

    private double parsePriceStr(String s) {
        if (s == null) return 0.0;
        try {
            String c = s.replaceAll("[^0-9.]", "");
            return c.isEmpty() ? 0.0 : Double.parseDouble(c);
        } catch (Exception e) { return 0.0; }
    }

    // ══════════════════════════════════════════════════════════════
    //  Navigation
    // ══════════════════════════════════════════════════════════════
    private void highlightCurrentTab(ImageView icon, TextView text) {
        if (icon != null && text != null) {
            int gold = Color.parseColor("#FFD700");
            icon.setColorFilter(gold);
            text.setTextColor(gold);
            text.setTypeface(null, Typeface.BOLD);
        }
    }

    private void setupBottomNav() {
        View footer = findViewById(R.id.footer_section);
        footer.findViewById(R.id.dashboard_nav).setOnClickListener(v ->
                Toast.makeText(this, "Already on Dashboard", Toast.LENGTH_SHORT).show());
        footer.findViewById(R.id.inventory_nav).setOnClickListener(v ->
                startActivity(new Intent(this, A3_Inventory_Management.class)));
        footer.findViewById(R.id.orders_nav).setOnClickListener(v ->
                startActivity(new Intent(this, A4_CustomerOrderActivity.class)));
        footer.findViewById(R.id.menu_nav).setOnClickListener(v ->
                startActivity(new Intent(this, A5_MenuManagementActivity.class)));
        footer.findViewById(R.id.profile_nav).setOnClickListener(v ->
                startActivity(new Intent(this, A6_Profile.class)));
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Hides the bottom buttons
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);    // Hides the top status bar
    }
}