package in.jvapps.disable_battery_optimization.utils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.Nullable;

import in.jvapps.disable_battery_optimization.managers.KillerManager;


public class BatteryOptimizationUtil {

    public static Intent getAppSettingsIntent(Context context) {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setData(Uri.fromParts("package", context.getApplicationContext().getPackageName(), null));
        return intent;
    }

    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        String packageName = context.getApplicationContext().getPackageName();
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            return true;
        }
        return powerManager.isIgnoringBatteryOptimizations(packageName);
    }

    public static Intent getIgnoreBatteryOptimizationsIntent(Context context) {
        if (Build.VERSION.SDK_INT < 23) {
            return null;
        }
        String sb = "package:" +
                context.getApplicationContext().getPackageName();
        @SuppressLint("BatteryLife") Intent intent = new Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse(sb));
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        return intent.resolveActivity(context.getPackageManager()) == null ? getAppSettingsIntent(context) : intent;
    }

    public static void showBatteryOptimizationDialog(
            final Context context,
            final KillerManager.Actions action,
            @Nullable final OnBatteryOptimizationAccepted positiveCallback,
            @Nullable final OnBatteryOptimizationCanceled negativeCallback) {
        if (KillerManager.isActionAvailable(context, action)) {
            if (positiveCallback != null)
                positiveCallback.onBatteryOptimizationAccepted();
        } else {
            if (positiveCallback != null)
                positiveCallback.onBatteryOptimizationAccepted();
        }
    }

    public interface OnBatteryOptimizationAccepted {
        void onBatteryOptimizationAccepted();
    }

    public interface OnBatteryOptimizationCanceled {
        void onBatteryOptimizationCanceled();
    }

}