package com.gamevault.univibe;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class AnimacionUtils {

    public static void animarEstrella(View view) {
        if (view == null) return;

        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setRotation(0f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.4f, 1.0f);
        scaleX.setDuration(350);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.4f, 1.0f);
        scaleY.setDuration(350);

        ObjectAnimator rotation = ObjectAnimator.ofFloat(view, "rotation", 0f, -20f, 20f, 0f);
        rotation.setDuration(350);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, rotation);
        animatorSet.setInterpolator(new OvershootInterpolator(2.0f));
        animatorSet.start();
    }
}
