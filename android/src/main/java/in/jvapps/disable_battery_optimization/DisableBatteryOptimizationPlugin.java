package in.jvapps.disable_battery_optimization;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import in.jvapps.disable_battery_optimization.managers.KillerManager;

import java.util.List;

import in.jvapps.disable_battery_optimization.utils.BatteryOptimizationUtil;
import in.jvapps.disable_battery_optimization.utils.BatteryOptimizationUtil.OnBatteryOptimizationAccepted;
import in.jvapps.disable_battery_optimization.utils.BatteryOptimizationUtil.OnBatteryOptimizationCanceled;
import in.jvapps.disable_battery_optimization.utils.PrefKeys;
import in.jvapps.disable_battery_optimization.utils.PrefUtils;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import androidx.annotation.Nullable;

/**
 * DisableBatteryOptimizationPlugin
 */
public class DisableBatteryOptimizationPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler {

    private Context mContext;
    private Activity mActivity;

    // These are null when not using v2 embedding.
    private MethodChannel channel;

    private static final int REQUEST_DISABLE_BATTERY_OPTIMIZATIONS = 2244;
    private final String TAG = "BO:DisableOptimization";
    private static final String CHANNEL_NAME = "in.jvapps.disable_battery_optimization";


    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "disable_battery_optimization");
        channel.setMethodCallHandler(this);
        mContext = flutterPluginBinding.getApplicationContext();
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "showEnableAutoStart":
                try {
                    showAutoStartEnabler(() -> setManAutoStart(true), () -> setManAutoStart(false));
                    result.success(true);
                } catch (Exception ex) {
                    Log.e(TAG, "Exception in showEnableAutoStart. " + ex.toString());
                    result.success(false);
                }
                break;
            case "showDisableManBatteryOptimization":
                try {
                    showManBatteryOptimizationDisabler(false);
                    result.success(true);
                } catch (Exception ex) {
                    Log.e(TAG, "Exception in showDisableManBatteryOptimization. " + ex.toString());
                    result.success(false);
                }
                break;
            case "showDisableBatteryOptimization":
                try {
                    showIgnoreBatteryPermissions();
                    result.success(true);
                } catch (Exception ex) {
                    Log.e(TAG, "Exception in showDisableBatteryOptimization. " + ex.toString());
                    result.success(false);
                }
                break;
            case "disableAllOptimizations":
                try {
                    handleIgnoreAllBatteryPermission();
                    result.success(true);
                } catch (Exception ex) {
                    Log.e(TAG, "Exception in disableAllOptimizations. " + ex.toString());
                    result.success(false);
                }
                break;
            case "isAutoStartEnabled":
                result.success(getManAutoStart());
                break;
            case "isBatteryOptimizationDisabled":
                result.success(BatteryOptimizationUtil.isIgnoringBatteryOptimizations(mContext));
                break;
            case "isManBatteryOptimizationDisabled":
                result.success(getManBatteryOptimization());
                break;
            case "isAllOptimizationsDisabled":
                result.success(getManAutoStart() && BatteryOptimizationUtil.isIgnoringBatteryOptimizations(mContext) && getManBatteryOptimization());
                break;
            default:
                result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        mActivity = binding.getActivity();
        mContext = mActivity.getApplicationContext();
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        mActivity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        mActivity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        mActivity = null;
        channel.setMethodCallHandler(null);
    }

    private void showAutoStartEnabler(
        @Nullable final OnBatteryOptimizationAccepted positiveCallback,
        @Nullable final OnBatteryOptimizationCanceled negativeCallback) {
        BatteryOptimizationUtil.showBatteryOptimizationDialog(
                mActivity,
                KillerManager.Actions.ACTION_AUTOSTART,
                positiveCallback,
                negativeCallback
        );
    }

    private void showManBatteryOptimizationDisabler(boolean isRequestNativeBatteryOptimizationDisabler) {
        BatteryOptimizationUtil.showBatteryOptimizationDialog(
                mActivity,
                KillerManager.Actions.ACTION_POWERSAVING,
                () -> {
                    setManBatteryOptimization(true);
                    if (isRequestNativeBatteryOptimizationDisabler) {
                        showIgnoreBatteryPermissions();
                    }
                },
                () -> {
                    if (isRequestNativeBatteryOptimizationDisabler) {
                        showIgnoreBatteryPermissions();
                    }
                }
        );
    }

    private void showIgnoreBatteryPermissions() {
        if (!BatteryOptimizationUtil.isIgnoringBatteryOptimizations(mContext)) {
            final Intent ignoreBatteryOptimizationsIntent = BatteryOptimizationUtil.getIgnoreBatteryOptimizationsIntent(mContext);
            if (ignoreBatteryOptimizationsIntent != null) {
                mContext.startActivity(ignoreBatteryOptimizationsIntent);
            } else {
                Log.i(TAG, "Can't ignore the battery optimization as the intent is null");
            }
        } else {
            Log.i(TAG, "Battery optimization is already disabled");
        }
    }

    private void handleIgnoreAllBatteryPermission() {
        boolean isManBatteryOptimizationDisabled = getManBatteryOptimization();
        if (!getManAutoStart()) {
            showAutoStartEnabler(() -> {
                setManAutoStart(true);
                if (!isManBatteryOptimizationDisabled)
                    showManBatteryOptimizationDisabler(true);
                else
                    showIgnoreBatteryPermissions();
            }, () -> {
                if (!isManBatteryOptimizationDisabled)
                    showManBatteryOptimizationDisabler(true);
                else
                    showIgnoreBatteryPermissions();
            });
        } else {
            if (!isManBatteryOptimizationDisabled)
                showManBatteryOptimizationDisabler(true);
            else
                showIgnoreBatteryPermissions();
        }
    }

    public void setManBatteryOptimization(boolean val) {
        PrefUtils.saveToPrefs(mContext, PrefKeys.IS_MAN_BATTERY_OPTIMIZATION_ACCEPTED, val);
    }

    public boolean getManBatteryOptimization() {
        if (PrefUtils.hasKey(mContext, PrefKeys.IS_MAN_BATTERY_OPTIMIZATION_ACCEPTED)) {
            return (boolean) PrefUtils.getFromPrefs(mContext, PrefKeys.IS_MAN_BATTERY_OPTIMIZATION_ACCEPTED, false);
        } else {
            boolean isManBatteryAvailable = KillerManager.isActionAvailable(mContext, KillerManager.Actions.ACTION_POWERSAVING);
            PrefUtils.saveToPrefs(mContext, PrefKeys.IS_MAN_BATTERY_OPTIMIZATION_ACCEPTED, !isManBatteryAvailable);
            return !isManBatteryAvailable;
        }
    }

    public void setManAutoStart(boolean val) {
        PrefUtils.saveToPrefs(mContext, PrefKeys.IS_MAN_AUTO_START_ACCEPTED, val);
    }

    public boolean getManAutoStart() {
        if (PrefUtils.hasKey(mContext, PrefKeys.IS_MAN_AUTO_START_ACCEPTED)) {
            return (boolean) PrefUtils.getFromPrefs(mContext, PrefKeys.IS_MAN_AUTO_START_ACCEPTED, false);
        } else {
            boolean isAutoStartAvailable = KillerManager.isActionAvailable(mContext, KillerManager.Actions.ACTION_AUTOSTART);
            PrefUtils.saveToPrefs(mContext, PrefKeys.IS_MAN_AUTO_START_ACCEPTED, !isAutoStartAvailable);
            return !isAutoStartAvailable;
        }
    }
}
