/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.vrbrowser.ui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;

import org.mozilla.gecko.util.ThreadUtils;
import org.mozilla.vrbrowser.R;
import org.mozilla.vrbrowser.ui.widgets.TooltipWidget;
import org.mozilla.vrbrowser.ui.widgets.UIWidget;
import org.mozilla.vrbrowser.ui.widgets.WidgetPlacement;
import org.mozilla.vrbrowser.utils.ViewUtils;

public class UIButton extends AppCompatImageButton implements CustomUIButton {

    private enum State {
        NORMAL,
        PRIVATE,
        ACTIVE,
        NOTIFICATION
    }

    private ColorStateList mTintColorList;
    private Drawable mPrivateModeBackground;
    private Drawable mActiveModeBackground;
    private Drawable mBackground;
    private @IdRes int mTintColorListRes;
    private @IdRes int mPrivateModeTintColorListRes;
    private @IdRes int mActiveModeTintColorListRes;
    private @IdRes int mNotificationModeTintColorListRes;
    private TooltipWidget mTooltipView;
    private String mTooltipText;
    private State mState;
    private int mTooltipDelay;
    private float mTooltipDensity;
    private @LayoutRes int mTooltipLayout;
    private boolean mCurvedTooltip = true;
    private ViewUtils.TooltipPosition mTooltipPosition;

    public UIButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.imageButtonStyle);
    }

    public UIButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.UIButton, defStyleAttr, 0);
        mTintColorListRes = attributes.getResourceId(R.styleable.UIButton_tintColorList, 0);
        if (mTintColorListRes != 0) {
            setTintColorList(mTintColorListRes);
        }
        mPrivateModeBackground = attributes.getDrawable(R.styleable.UIButton_privateModeBackground);
        mActiveModeBackground = attributes.getDrawable(R.styleable.UIButton_activeModeBackground);
        mPrivateModeTintColorListRes = attributes.getResourceId(R.styleable.UIButton_privateModeTintColorList, 0);
        mActiveModeTintColorListRes = attributes.getResourceId(R.styleable.UIButton_activeModeTintColorList, 0);
        mNotificationModeTintColorListRes = attributes.getResourceId(R.styleable.UIButton_notificationModeTintColorList, 0);
        mTooltipDelay = attributes.getInt(R.styleable.UIButton_tooltipDelay, getResources().getInteger(R.integer.tooltip_delay));
        mTooltipPosition = ViewUtils.TooltipPosition.fromId(attributes.getInt(R.styleable.UIButton_tooltipPosition, ViewUtils.TooltipPosition.BOTTOM.ordinal()));
        TypedValue densityValue = new TypedValue();
        getResources().getValue(R.dimen.tooltip_default_density, densityValue, true);
        mTooltipDensity = attributes.getFloat(R.styleable.UIButton_tooltipDensity, densityValue.getFloat());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            TypedArray arr = context.obtainStyledAttributes(attrs, new int [] {android.R.attr.tooltipText});
            mTooltipText = arr.getString(0);
        }
        mTooltipLayout = attributes.getResourceId(R.styleable.UIButton_tooltipLayout, R.layout.tooltip);
        attributes.recycle();

        mBackground = getBackground();

        mState = State.NORMAL;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public String getTooltip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return mTooltipText;
        } else {
            return getTooltipText() == null ? null : getTooltipText().toString();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    public void setTooltip(String text) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            mTooltipText = text;
        } else {
            setTooltipText(text);
        }

        if (mTooltipView != null && mTooltipView.isVisible()) {
            mTooltipView.setText(text);
        }
    }

    public void setCurvedTooltip(boolean aEnabled) {
        mCurvedTooltip = aEnabled;
        if (mTooltipView != null) {
            mTooltipView.setCurvedMode(aEnabled);
        }
    }

    public void setTooltipText(@NonNull String text) {
        mTooltipText = text;
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (getTooltip() != null) {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                ThreadUtils.postDelayedToUiThread(mShowTooltipRunnable, mTooltipDelay);

            } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                ThreadUtils.removeCallbacksFromUiThread(mShowTooltipRunnable);
                ThreadUtils.postToUiThread(mHideTooltipRunnable);
            }
        }

        return super.onHoverEvent(event);
    }

    public void setTintColorList(int aColorListId) {
        mTintColorList = getContext().getResources().getColorStateList(
                aColorListId,
                getContext().getTheme());
        if (mTintColorList != null) {
            int color = mTintColorList.getColorForState(getDrawableState(), 0);
            setColorFilter(color);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mTintColorList != null && mTintColorList.isStateful()) {
            int color = mTintColorList.getColorForState(getDrawableState(), 0);
            setColorFilter(color);
        }
    }

    @Override
    public void setPrivateMode(boolean isPrivateMode) {
        if (isPrivateMode) {
            setPrivate();

        } else {
            setNormal();
        }
    }

    public void setActiveMode(boolean isActive) {
        if (isActive) {
            setActive();

        } else {
            setNormal();
        }
    }

    public void setNotificationMode(boolean isNotification) {
        if (isNotification) {
            setNotification();

        } else {
            setNormal();
        }
    }

    public boolean isActive() {
        return mState == State.ACTIVE;
    }

    public boolean isPrivate() {
        return mState == State.PRIVATE;
    }

    private void setPrivate() {
        mState = State.PRIVATE;
        if (mPrivateModeBackground != null) {
            setBackground(mPrivateModeBackground);
        }

        if (mPrivateModeTintColorListRes != 0) {
            setTintColorList(mPrivateModeTintColorListRes);
        }
    }

    private void setNormal() {
        mState = State.NORMAL;
        if (mBackground != null) {
            setBackground(mBackground);
        }

        if(mTintColorListRes != 0) {
            setTintColorList(mTintColorListRes);
        }
    }

    private void setActive() {
        mState = State.ACTIVE;
        if (mActiveModeBackground != null) {
            setBackground(mActiveModeBackground);
        }

        if (mActiveModeTintColorListRes != 0) {
            setTintColorList(mActiveModeTintColorListRes);
        }
    }

    private void setNotification() {
        mState = State.NOTIFICATION;
        if (mActiveModeTintColorListRes != 0) {
            setTintColorList(mNotificationModeTintColorListRes);
        }
    }

    private Runnable mShowTooltipRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTooltipView != null && mTooltipView.isVisible()) {
                return;
            }

            if (mTooltipView == null) {
                mTooltipView = new TooltipWidget(getContext(), mTooltipLayout);
            }
            mTooltipView.setCurvedMode(mCurvedTooltip);
            mTooltipView.setText(getTooltip());

            Rect offsetViewBounds = new Rect();
            getDrawingRect(offsetViewBounds);
            UIWidget parent = ViewUtils.getParentWidget(UIButton.this);
            parent.offsetDescendantRectToMyCoords(UIButton.this, offsetViewBounds);

            float ratio = WidgetPlacement.viewToWidgetRatio(getContext(), parent);

            mTooltipView.getPlacement().parentHandle = parent.getHandle();
            mTooltipView.getPlacement().density = mTooltipDensity;
            // At the moment we only support showing tooltips on top or bottom of the target view
            if (mTooltipPosition == ViewUtils.TooltipPosition.BOTTOM) {
                mTooltipView.getPlacement().anchorY = 1.0f;
                mTooltipView.getPlacement().parentAnchorY = 0.0f;
                mTooltipView.getPlacement().translationX = (offsetViewBounds.left + UIButton.this.getWidth() / 2.0f) * ratio;
                mTooltipView.getPlacement().translationY = -offsetViewBounds.top * ratio;

            } else {
                mTooltipView.getPlacement().anchorY = 0.0f;
                mTooltipView.getPlacement().parentAnchorY = 1.0f;
                mTooltipView.getPlacement().translationX = (offsetViewBounds.left + UIButton.this.getHeight() / 2.0f) * ratio;
                mTooltipView.getPlacement().translationY = offsetViewBounds.top * ratio;
            }

            mTooltipView.setCurvedMode(false);
            mTooltipView.show(UIWidget.CLEAR_FOCUS);
        }
    };

    private Runnable mHideTooltipRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTooltipView != null) {
                mTooltipView.hide(UIWidget.REMOVE_WIDGET);
            }
        }
    };

}
