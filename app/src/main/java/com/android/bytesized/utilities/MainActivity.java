package com.android.bytesized.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    // Filter modes
    public enum FilterType { ALL, SYSTEM, USER, RECYCLE_BIN }
    private FilterType currentFilter = FilterType.ALL;

    // App Data Model
    public static class AppInfoModel {
        String appName;
        String packageName;
        Drawable icon;
        boolean isSystemApp;
        boolean isUninstalled; // True if uninstalled system app (Recycle Bin)
        boolean isSelected;    // For batch selection

        public AppInfoModel(String appName, String packageName, Drawable icon, boolean isSystemApp, boolean isUninstalled) {
            this.appName = appName;
            this.packageName = packageName;
            this.icon = icon;
            this.isSystemApp = isSystemApp;
            this.isUninstalled = isUninstalled;
            this.isSelected = false;
        }
    }

    private final ArrayList<AppInfoModel> masterList = new ArrayList<>();
    private final ArrayList<AppInfoModel> filteredList = new ArrayList<>();
    private ArrayAdapter<AppInfoModel> adapter;

    private Button btnBatchAction;
    private Button btnSelectAll;
    private EditText searchEdit;
    private String currentSearchQuery = "";

    private static final int SHIZUKU_CODE = 1001;

    private final Shizuku.OnRequestPermissionResultListener onRequestPermissionResultListener =
        (requestCode, grantResult) -> {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Shizuku Permission Granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Shizuku Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = findViewById(R.id.packageListView);
        searchEdit = findViewById(R.id.searchEdit);
        btnBatchAction = findViewById(R.id.btnBatchAction);
        btnSelectAll = findViewById(R.id.btnSelectAll);

        Button btnFilterAll = findViewById(R.id.btnFilterAll);
        Button btnFilterSystem = findViewById(R.id.btnFilterSystem);
        Button btnFilterUser = findViewById(R.id.btnFilterUser);
        Button btnFilterRecycleBin = findViewById(R.id.btnFilterRecycleBin);

        // Custom List Adapter
        adapter = new ArrayAdapter<AppInfoModel>(this, R.layout.list_item_app, filteredList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_app, parent, false);
                }

                AppInfoModel item = getItem(position);
                if (item != null) {
                    ImageView iconView = convertView.findViewById(R.id.appIcon);
                    TextView nameView = convertView.findViewById(R.id.appNameText);
                    TextView pkgView = convertView.findViewById(R.id.packageNameText);
                    TextView badgeView = convertView.findViewById(R.id.badgeText);
                    CheckBox checkBox = convertView.findViewById(R.id.appCheckBox);

                    iconView.setImageDrawable(item.icon);
                    nameView.setText(item.appName);
                    pkgView.setText(item.packageName);

                    // Configure Checkbox
                    checkBox.setOnCheckedChangeListener(null);
                    checkBox.setChecked(item.isSelected);
                    checkBox.setOnCheckedChangeListener((cb, isChecked) -> {
                        item.isSelected = isChecked;
                        updateBatchButtonState();
                    });

                    // Badge Styling
                    if (item.isUninstalled) {
                        badgeView.setText("TRASH");
                        badgeView.setBackgroundColor(Color.parseColor("#EF4444"));
                    } else if (item.isSystemApp) {
                        badgeView.setText("SYSTEM");
                        badgeView.setBackgroundColor(Color.parseColor("#3B82F6"));
                    } else {
                        badgeView.setText("USER");
                        badgeView.setBackgroundColor(Color.parseColor("#10B981"));
                    }
                }
                return convertView;
            }
        };

        listView.setAdapter(adapter);

        Shizuku.addRequestPermissionResultListener(onRequestPermissionResultListener);

        // Initialize Filter Listeners
        btnFilterAll.setOnClickListener(v -> setFilter(FilterType.ALL));
        btnFilterSystem.setOnClickListener(v -> setFilter(FilterType.SYSTEM));
        btnFilterUser.setOnClickListener(v -> setFilter(FilterType.USER));
        btnFilterRecycleBin.setOnClickListener(v -> setFilter(FilterType.RECYCLE_BIN));

        // Select All Listener
        btnSelectAll.setOnClickListener(v -> {
            boolean targetState = !areAllSelected();
            for (AppInfoModel app : filteredList) {
                app.isSelected = targetState;
            }
            adapter.notifyDataSetChanged();
            updateBatchButtonState();
        });

        // Batch Action Listener
        btnBatchAction.setOnClickListener(v -> executeBatchAction());

        // Single Click Listener
        listView.setOnItemClickListener((parent, view, position, id) -> {
            AppInfoModel selectedApp = filteredList.get(position);
            if (selectedApp.isUninstalled) {
                confirmRestore(selectedApp);
            } else {
                confirmUninstall(selectedApp);
            }
        });

        // Search TextWatcher
        searchEdit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFilterAndSearch();
            }
            public void afterTextChanged(Editable s) {}
        });

        loadInstalledPackages();
        checkShizukuPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(onRequestPermissionResultListener);
    }

    private void setFilter(FilterType filter) {
        this.currentFilter = filter;
        applyFilterAndSearch();
    }

    private void checkShizukuPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(SHIZUKU_CODE);
                }
            } else {
                Toast.makeText(this, "Shizuku service is NOT running!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Shizuku error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadInstalledPackages() {
        masterList.clear();
        PackageManager pm = getPackageManager();

        // Fetch installed apps AND uninstalled system apps (MATCH_UNINSTALLED_PACKAGES)
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);

        for (ApplicationInfo appInfo : packages) {
            String appName = appInfo.loadLabel(pm).toString();
            String packageName = appInfo.packageName;
            Drawable icon = appInfo.loadIcon(pm);

            boolean isSystemApp = (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            boolean isInstalled = (appInfo.flags & ApplicationInfo.FLAG_INSTALLED) != 0;

            // An app is in the Recycle Bin if it is a system app removed for User 0
            boolean isUninstalledSystemApp = isSystemApp && !isInstalled;

            // Only add active installed apps OR uninstalled system apps
            if (isInstalled || isUninstalledSystemApp) {
                masterList.add(new AppInfoModel(appName, packageName, icon, isSystemApp, isUninstalledSystemApp));
            }
        }

        applyFilterAndSearch();
    }

    private void applyFilterAndSearch() {
        filteredList.clear();
        String q = currentSearchQuery.toLowerCase().trim();

        for (AppInfoModel app : masterList) {
            boolean matchesSearch = app.packageName.toLowerCase().contains(q) || app.appName.toLowerCase().contains(q);
            if (!matchesSearch) continue;

            switch (currentFilter) {
                case ALL:
                    if (!app.isUninstalled) filteredList.add(app);
                    break;
                case SYSTEM:
                    if (app.isSystemApp && !app.isUninstalled) filteredList.add(app);
                    break;
                case USER:
                    if (!app.isSystemApp && !app.isUninstalled) filteredList.add(app);
                    break;
                case RECYCLE_BIN:
                    if (app.isUninstalled) filteredList.add(app);
                    break;
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateBatchButtonState();
    }

    private void updateBatchButtonState() {
        int count = 0;
        for (AppInfoModel app : filteredList) {
            if (app.isSelected) count++;
        }

        if (currentFilter == FilterType.RECYCLE_BIN) {
            btnBatchAction.setText("Restore (" + count + ")");
            btnBatchAction.setBackgroundColor(Color.parseColor("#10B981")); // Green
        } else {
            btnBatchAction.setText("Uninstall (" + count + ")");
            btnBatchAction.setBackgroundColor(Color.parseColor("#EF4444")); // Red
        }

        btnSelectAll.setText(areAllSelected() ? "Deselect All" : "Select All");
    }

    private boolean areAllSelected() {
        if (filteredList.isEmpty()) return false;
        for (AppInfoModel app : filteredList) {
            if (!app.isSelected) return false;
        }
        return true;
    }

    private void confirmUninstall(AppInfoModel app) {
        String msg = app.isSystemApp 
            ? "Uninstall " + app.appName + "?\n(System app: Can be restored anytime via Recycle Bin)"
            : "Uninstall " + app.appName + "?\n(User app: APK will be deleted and requires Play Store re-download)";

        new AlertDialog.Builder(this)
            .setTitle("Confirm Uninstallation")
            .setMessage(msg)
            .setPositiveButton("Uninstall", (dialog, which) -> uninstallWithShizuku(app))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void confirmRestore(AppInfoModel app) {
        new AlertDialog.Builder(this)
            .setTitle("Restore System App")
            .setMessage("Re-enable " + app.appName + " for User 0?")
            .setPositiveButton("Restore", (dialog, which) -> restoreWithShizuku(app))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void uninstallWithShizuku(AppInfoModel app) {
        if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Shizuku is not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = executeShizukuCommand(new String[]{"pm", "uninstall", "--user", "0", app.packageName});

        if (success) {
            if (app.isSystemApp) {
                app.isUninstalled = true;
                app.isSelected = false;
                View rootView = findViewById(android.R.id.content);
                Snackbar.make(rootView, "Moved " + app.appName + " to Recycle Bin", Snackbar.LENGTH_LONG)
                    .setAction("UNDO", v -> restoreWithShizuku(app))
                    .setActionTextColor(0xFF38BDF8)
                    .show();
            } else {
                masterList.remove(app);
                View rootView = findViewById(android.R.id.content);
                Snackbar.make(rootView, "Uninstalled " + app.appName, Snackbar.LENGTH_LONG)
                    .setAction("REINSTALL", v -> openPlayStore(app.packageName))
                    .setActionTextColor(0xFF38BDF8)
                    .show();
            }
            applyFilterAndSearch();
        } else {
            Toast.makeText(this, "Failed to uninstall " + app.appName, Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreWithShizuku(AppInfoModel app) {
        boolean restored = executeShizukuCommand(new String[]{"pm", "install-existing", "--user", "0", app.packageName});

        if (restored) {
            app.isUninstalled = false;
            app.isSelected = false;
            Toast.makeText(this, "Restored: " + app.appName, Toast.LENGTH_SHORT).show();
            applyFilterAndSearch();
        } else {
            Toast.makeText(this, "Failed to restore " + app.appName, Toast.LENGTH_SHORT).show();
        }
    }

    private void executeBatchAction() {
        ArrayList<AppInfoModel> selectedApps = new ArrayList<>();
        for (AppInfoModel app : filteredList) {
            if (app.isSelected) selectedApps.add(app);
        }

        if (selectedApps.isEmpty()) {
            Toast.makeText(this, "No apps selected", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isRecycleBin = (currentFilter == FilterType.RECYCLE_BIN);
        String actionTitle = isRecycleBin ? "Batch Restore" : "Batch Uninstall";

        new AlertDialog.Builder(this)
            .setTitle(actionTitle)
            .setMessage("Process " + selectedApps.size() + " selected application(s)?")
            .setPositiveButton("Proceed", (dialog, which) -> {
                for (AppInfoModel app : selectedApps) {
                    if (isRecycleBin) {
                        restoreWithShizuku(app);
                    } else {
                        uninstallWithShizuku(app);
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void openPlayStore(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            startActivity(intent);
        }
    }

    private boolean executeShizukuCommand(String[] command) {
        try {
            Method newProcessMethod = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            newProcessMethod.setAccessible(true);

            Process process = (Process) newProcessMethod.invoke(null, command, null, null);
            return process != null && process.waitFor() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
