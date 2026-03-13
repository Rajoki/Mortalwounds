package com.rajoki.injuryplugin.utils;


//Easy util used for colors of text,models, etc for health display
public final class HealthColorUtil {

    public static String getHealthColor(float current, float max) {
        float percent = (max > 0) ? (current / max) * 100f : 100f;

        if (percent <= 0) return "#000000";
        if (percent <= 25) return "#FF0000";
        if (percent <= 50) return "#FFA500";
        if (percent <= 75) return "#FFFF00";
        if (percent <= 90) return "#5aff08";
        return "#00FF00";
    }
}
