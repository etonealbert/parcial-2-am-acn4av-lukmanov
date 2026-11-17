package com.example.albertup.ui;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Edge-to-edge helper shared by every screen. */
public final class SystemBars {

    private SystemBars() {
    }

    /** Pads {@code root} so content stays clear of system bars, cutouts and optionally the IME. */
    public static void applyPadding(@NonNull View root, boolean includeIme) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int types = WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout();
            if (includeIme) {
                types |= WindowInsetsCompat.Type.ime();
            }
            Insets bars = insets.getInsets(types);
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
