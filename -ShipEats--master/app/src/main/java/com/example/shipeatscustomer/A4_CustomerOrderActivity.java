package com.example.shipeatscustomer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class A4_CustomerOrderActivity extends AppCompatActivity {
    private static final String TAG = "AdminOrders";

    private TabLayout tabLayout;
    private RecyclerView rvOrders;
    private AdminOrdersAdapter adapter;
    private List<AdminOrderModel> allOrders = new ArrayList<>();
    private List<AdminOrderModel> filteredList = new ArrayList<>();
    private String currentStatus = "All";
    private TextInputEditText searchInput;
    private ImageView btnFilterDate;
    private TextView tvActiveFilter;

    // Filter state
    // filterMode: "today" | "yesterday" | "7" | "30" | "90" | "all" | "custom"
    private String filterMode = "all";

    // Custom range millis (only used when filterMode == "custom")
    private long customStartMillis = 0;
    private long customEndMillis   = 0;

    // Date formats
    // Firebase stores: "19 Mar 2026, 11:20"
    private static final SimpleDateFormat DB_FMT  =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final SimpleDateFormat DAY_FMT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat LABEL_FMT =
            new SimpleDateFormat("d MMM", Locale.getDefault()); // e.g. "1 Mar – 19 Mar"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_a4_cust_order);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, A1_Login_Page.class));
            finish();
            return;
        }

        tabLayout      = findViewById(R.id.tabLayout);
        rvOrders       = findViewById(R.id.rvOrders);
        searchInput    = findViewById(R.id.search_input);
        btnFilterDate  = findViewById(R.id.btn_filter_date);
        tvActiveFilter = findViewById(R.id.tv_active_filter); // optional pill badge

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminOrdersAdapter(this, filteredList);
        rvOrders.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab != null && tab.getText() != null) {
                    currentStatus = tab.getText().toString();
                    applyFilters();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Default: Pending tab
        if (tabLayout.getTabCount() > 1) {
            TabLayout.Tab pendingTab = tabLayout.getTabAt(1);
            if (pendingTab != null) { pendingTab.select(); currentStatus = "Pending"; }
        }

        loadAllOrders();

        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
                @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) { applyFilters(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (btnFilterDate != null)
            btnFilterDate.setOnClickListener(v -> showFilterBottomSheet());

        setupBottomNav();
        setupHeader();
        highlightCurrentTab(findViewById(R.id.orders_icon), findViewById(R.id.orders_text));
    }

    // ══════════════════════════════════════════════════════════════
    //  Bottom Sheet Filter
    // ══════════════════════════════════════════════════════════════
    private void showFilterBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.admin_bottom_sheet_filter, null);
        dialog.setContentView(view);
        dialog.getBehavior().setPeekHeight(1000);

        // Restore check state
        updateFilterChecks(view, filterMode);

        // Restore custom label if previously set
        if ("custom".equals(filterMode) && customStartMillis > 0) {
            TextView tvCustomLabel = view.findViewById(R.id.tv_custom_range_label);
            if (tvCustomLabel != null)
                tvCustomLabel.setText(buildCustomLabel(customStartMillis, customEndMillis));
        }

        // Standard filter rows
        view.findViewById(R.id.filter_today)
                .setOnClickListener(v -> selectFilter(dialog, view, "today", "Today"));
        view.findViewById(R.id.filter_yesterday)
                .setOnClickListener(v -> selectFilter(dialog, view, "yesterday", "Yesterday"));
        view.findViewById(R.id.filter_7days)
                .setOnClickListener(v -> selectFilter(dialog, view, "7", "Last 7 Days"));
        view.findViewById(R.id.filter_30days)
                .setOnClickListener(v -> selectFilter(dialog, view, "30", "Last 30 Days"));
        view.findViewById(R.id.filter_3months)
                .setOnClickListener(v -> selectFilter(dialog, view, "90", "Last 3 Months"));
        view.findViewById(R.id.filter_all)
                .setOnClickListener(v -> selectFilter(dialog, view, "all", "All History"));

        // Custom date range row
        view.findViewById(R.id.filter_custom).setOnClickListener(v -> {
            // Dismiss bottom sheet first so date picker shows on top cleanly
            dialog.dismiss();
            showCustomDatePicker(view);
        });

        // Reset
        view.findViewById(R.id.tv_reset_filter).setOnClickListener(v -> {
            filterMode = "all";
            customStartMillis = 0;
            customEndMillis   = 0;
            updateFilterChecks(view, filterMode);
            updateFilterBadge("All History");
            TextView tvCustomLabel = view.findViewById(R.id.tv_custom_range_label);
            if (tvCustomLabel != null) tvCustomLabel.setText("Tap to select dates");
        });

        // Apply
        view.findViewById(R.id.btn_apply_filter).setOnClickListener(v -> {
            applyFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void selectFilter(BottomSheetDialog dialog, View view, String mode, String label) {
        filterMode = mode;
        // Clear custom range when switching to a preset
        if (!"custom".equals(mode)) {
            customStartMillis = 0;
            customEndMillis   = 0;
        }
        updateFilterChecks(view, mode);
        updateFilterBadge(label);
    }

    // Custom date picker (MaterialDatePicker range)
    private void showCustomDatePicker(View bottomSheetView) {
        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select Date Range")
                        .setTheme(R.style.CustomDatePickerTheme)
                        .build();

        picker.show(getSupportFragmentManager(), "FILTER_DATE_PICKER");

        picker.addOnPositiveButtonClickListener(selection -> {
            customStartMillis = selection.first;
            // Include full end day up to 23:59:59
            customEndMillis   = selection.second + (24 * 60 * 60 * 1000L) - 1;
            filterMode        = "custom";

            String label = buildCustomLabel(customStartMillis, customEndMillis);
            updateFilterBadge(label);
            updateFilterChecks(bottomSheetView, "custom");

            // Apply immediately after picking
            applyFilters();
        });
    }

    /** Builds a human-readable label e.g. "1 Mar – 19 Mar" */
    private String buildCustomLabel(long startMillis, long endMillis) {
        return LABEL_FMT.format(new Date(startMillis))
                + " – "
                + LABEL_FMT.format(new Date(endMillis));
    }

    /** Shows/hides the check icon next to the active filter row */
    private void updateFilterChecks(View view, String mode) {
        int[]    checkIds = {
                R.id.check_today, R.id.check_yesterday,
                R.id.check_7days, R.id.check_30days,
                R.id.check_3months, R.id.check_all, R.id.check_custom
        };
        String[] modes = {"today", "yesterday", "7", "30", "90", "all", "custom"};

        for (int i = 0; i < checkIds.length; i++) {
            View check = view.findViewById(checkIds[i]);
            if (check != null)
                check.setVisibility(modes[i].equals(mode) ? View.VISIBLE : View.GONE);
        }
    }

    /** Updates the pill badge next to the filter icon in the toolbar */
    private void updateFilterBadge(String label) {
        if (tvActiveFilter != null) {
            if ("All History".equals(label)) {
                tvActiveFilter.setVisibility(View.GONE);
            } else {
                tvActiveFilter.setText(label);
                tvActiveFilter.setVisibility(View.VISIBLE);
            }
        }
        // Turn filter icon gold when active
        if (btnFilterDate != null) {
            boolean active = !"all".equals(filterMode);
            btnFilterDate.setColorFilter(active
                    ? Color.parseColor("#FDB02C")
                    : Color.parseColor("#032565"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Firebase
    // ══════════════════════════════════════════════════════════════
    private void loadAllOrders() {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("Orders");
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allOrders.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AdminOrderModel order = ds.getValue(AdminOrderModel.class);
                    if (order != null) {
                        order.orderId = ds.getKey();
                        allOrders.add(order);
                    }
                }
                applyFilters();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Filter logic
    // ══════════════════════════════════════════════════════════════
    private void applyFilters() {
        String query = (searchInput != null && searchInput.getText() != null)
                ? searchInput.getText().toString().toLowerCase().trim() : "";
        filteredList.clear();

        // Pre-compute day keys for today / yesterday
        Calendar cal = Calendar.getInstance();
        String todayKey     = DAY_FMT.format(cal.getTime());
        cal.add(Calendar.DATE, -1);
        String yesterdayKey = DAY_FMT.format(cal.getTime());

        // Pre-compute rangeStart for day-based filters
        cal = Calendar.getInstance();
        Date rangeStart = null;
        switch (filterMode) {
            case "7":  cal.add(Calendar.DATE,  -7);  rangeStart = cal.getTime(); break;
            case "30": cal.add(Calendar.DATE,  -30); rangeStart = cal.getTime(); break;
            case "90": cal.add(Calendar.DATE,  -90); rangeStart = cal.getTime(); break;
        }

        for (AdminOrderModel order : allOrders) {
            if (order == null || order.status == null) continue;

            // Status filter
            String status    = order.status.trim();
            String activeTab = currentStatus != null ? currentStatus.trim() : "All";
            boolean matchesStatus;

            if ("All".equalsIgnoreCase(activeTab)) {
                matchesStatus = !"Completed".equalsIgnoreCase(status)
                        && !"Done".equalsIgnoreCase(status)
                        && !"Cancelled".equalsIgnoreCase(status);
            } else if ("Preparing".equalsIgnoreCase(activeTab)) {
                matchesStatus = "Preparing".equalsIgnoreCase(status)
                        || "Ready".equalsIgnoreCase(status);
            } else if ("Completed".equalsIgnoreCase(activeTab)) {
                matchesStatus = "Completed".equalsIgnoreCase(status)
                        || "Done".equalsIgnoreCase(status);
            } else {
                matchesStatus = activeTab.equalsIgnoreCase(status);
            }

            // Date filter
            boolean matchesDate = true;
            if (order.date != null && !"all".equals(filterMode)) {
                try {
                    Date orderDate = DB_FMT.parse(order.date);
                    if (orderDate == null) {
                        matchesDate = false;
                    } else {
                        switch (filterMode) {
                            case "today":
                                matchesDate = todayKey.equals(DAY_FMT.format(orderDate));
                                break;
                            case "yesterday":
                                matchesDate = yesterdayKey.equals(DAY_FMT.format(orderDate));
                                break;
                            case "7":
                            case "30":
                            case "90":
                                matchesDate = rangeStart != null && !orderDate.before(rangeStart);
                                break;
                            case "custom":
                                // Compare millis directly
                                long t = orderDate.getTime();
                                matchesDate = t >= customStartMillis && t <= customEndMillis;
                                break;
                        }
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Date parse error: " + order.date);
                    matchesDate = false;
                }
            }

            // Search filter
            String oid  = order.orderId      != null ? order.orderId.toLowerCase()      : "";
            String cust = order.customerName != null ? order.customerName.toLowerCase() : "";
            boolean matchesSearch = oid.contains(query) || cust.contains(query);

            if (matchesStatus && matchesDate && matchesSearch)
                filteredList.add(order);
        }

        adapter.notifyDataSetChanged();
    }

    // ══════════════════════════════════════════════════════════════
    //  Navigation
    // ══════════════════════════════════════════════════════════════
    private void setupHeader() {
        View notifBtn = findViewById(R.id.admin_notification_btn);
        if (notifBtn != null)
            notifBtn.setOnClickListener(v -> AdminDialogHelper.showNotificationsDialog(this));
    }

    private void highlightCurrentTab(ImageView icon, TextView text) {
        if (icon != null && text != null) {
            int gold = Color.parseColor("#FFD700");
            icon.setColorFilter(gold);
            text.setTextColor(gold);
            text.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void setupBottomNav() {
        View footer = findViewById(R.id.footer_section);
        if (footer != null) {
            footer.findViewById(R.id.dashboard_nav).setOnClickListener(v ->
                    startActivity(new Intent(this, A2_Dashboard.class)));
            footer.findViewById(R.id.inventory_nav).setOnClickListener(v ->
                    startActivity(new Intent(this, A3_Inventory_Management.class)));
            footer.findViewById(R.id.orders_nav).setOnClickListener(v ->
                    Toast.makeText(this, "Already on Orders", Toast.LENGTH_SHORT).show());
            footer.findViewById(R.id.menu_nav).setOnClickListener(v ->
                    startActivity(new Intent(this, A5_MenuManagementActivity.class)));
            footer.findViewById(R.id.profile_nav).setOnClickListener(v ->
                    startActivity(new Intent(this, A6_Profile.class)));
        }
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