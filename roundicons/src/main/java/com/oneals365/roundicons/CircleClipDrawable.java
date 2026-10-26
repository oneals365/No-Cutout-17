package com.oneals365.roundicons;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;

/** Draws any drawable clipped to the circle inscribed in its bounds; tint etc. pass through. */
public class CircleClipDrawable extends DrawableWrapper {
    private final Path clip = new Path();

    public CircleClipDrawable(Drawable d) { super(d); }

    @Override
    protected void onBoundsChange(Rect b) {
        super.onBoundsChange(b);
        clip.reset();
        float r = Math.min(b.width(), b.height()) / 2f;
        clip.addCircle(b.exactCenterX(), b.exactCenterY(), r, Path.Direction.CW);
    }

    @Override
    public void draw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipPath(clip);
        super.draw(canvas);
        canvas.restoreToCount(save);
    }
}
