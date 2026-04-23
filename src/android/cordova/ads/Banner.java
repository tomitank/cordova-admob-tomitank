package admob.plus.cordova.ads;

import android.content.res.Configuration;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

import java.util.HashMap;

import admob.plus.cordova.ExecuteContext;
import admob.plus.cordova.Generated.Events;
import admob.plus.core.Context;

import static admob.plus.core.Helper.getParentView;
import static admob.plus.core.Helper.pxToDp;
import static admob.plus.core.Helper.removeFromParentView;

public class Banner extends AdBase {
    private static final String TAG = "AdMobPlus.Banner";
    private static int screenWidth = 0;
    private final AdSize adSize;
    private final int gravity;
    private Integer offset;
    private AdView mAdView;
    private AdView mAdViewOld = null;
    private FrameLayout frameLayout = null;
    private FrameLayout.LayoutParams origWebViewMargins;
    private ViewGroup.MarginLayoutParams origParentMargins;

    public Banner(ExecuteContext ctx) {
        super(ctx);
        this.adSize = ctx.optAdSize();
        this.gravity = "top".equals(ctx.optPosition()) ? Gravity.TOP : Gravity.BOTTOM;
        this.offset = ctx.optOffset();
    }

    private static void runJustBeforeBeingDrawn(final View view, final Runnable runnable) {
        final OnPreDrawListener preDrawListener = new OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                runnable.run();
                return true;
            }
        };
        view.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    @Override
    public boolean isLoaded() {
        return mAdView != null;
    }

    @Override
    public void load(Context ctx) {
        if (mAdView == null) {
            mAdView = createBannerView();
        }

        mAdView.loadAd(adRequest);
        ctx.resolve();
    }

    private AdView createBannerView() {
        AdView adView = new AdView(getActivity());
        adView.setAdUnitId(adUnitId);
        adView.setAdSize(adSize);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                emit(Events.AD_CLICK);
            }

            @Override
            public void onAdClosed() {
                emit(Events.AD_DISMISS);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                emit(Events.AD_LOAD_FAIL, error);
            }

            @Override
            public void onAdImpression() {
                emit(Events.AD_IMPRESSION);
            }

            @Override
            public void onAdLoaded() {
                if (mAdViewOld != null) {
                    removeBannerView(mAdViewOld);
                    mAdViewOld = null;
                }

                runJustBeforeBeingDrawn(adView, () -> emit(Events.BANNER_SIZE, computeAdSize()));

                emit(Events.AD_LOAD, computeAdSize());
            }

            @Override
            public void onAdOpened() {
                emit(Events.AD_SHOW);
            }
        });
        return adView;
    }

    @NonNull
    private HashMap<String, Object> computeAdSize() {
        int width = mAdView.getWidth();
        int height = mAdView.getHeight();

        return new HashMap<String, Object>() {{
            put("size", new HashMap<String, Object>() {{
                put("width", pxToDp(width));
                put("height", pxToDp(height));
                put("widthInPixels", width);
                put("heightInPixels", height);
            }});
        }};
    }

    @Override
    public void show(Context ctx) {
        if (mAdView.getParent() == null) {
            addBannerView();
        } else if (mAdView.getVisibility() == View.GONE) {
            mAdView.resume();
            mAdView.setVisibility(View.VISIBLE);
        } else {
            ViewGroup contentView = getContentView();
            if (getParentView(frameLayout) != contentView) {
                removeFromParentView(frameLayout);
                addBannerView();
            }
        }

        ctx.resolve();
    }

    @Override
    public void hide(Context ctx) {
        if (mAdView != null) {
            mAdView.pause();
            mAdView.setVisibility(View.GONE);
        }
        ctx.resolve();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int w = getActivity().getResources().getDisplayMetrics().widthPixels;
        if (w != screenWidth) {
            screenWidth = w;
            getActivity().runOnUiThread(this::reloadBannerView);
        }
    }

    private void reloadBannerView() {
        if (mAdView == null || mAdView.getVisibility() == View.GONE) return;

        pauseBannerViews();
        if (mAdViewOld != null) removeBannerView(mAdViewOld);
        mAdViewOld = mAdView;

        mAdView = createBannerView();
        mAdView.loadAd(adRequest);
        addBannerView();
    }

    @Override
    public void onPause(boolean multitasking) {
        pauseBannerViews();
        super.onPause(multitasking);
    }

    private void pauseBannerViews() {
        if (mAdView != null) mAdView.pause();
        if (mAdViewOld != null && mAdViewOld != mAdView) {
            mAdViewOld.pause();
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        resumeBannerViews();
    }

    private void resumeBannerViews() {
        if (mAdView != null) mAdView.resume();
        if (mAdViewOld != null) mAdViewOld.resume();
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            removeBannerView(mAdView);
            mAdView = null;
        }
        if (mAdViewOld != null) {
            removeBannerView(mAdViewOld);
            mAdViewOld = null;
        }
        if (frameLayout != null) {
            frameLayout.removeAllViews();
            removeFromParentView(frameLayout);
            frameLayout = null;
        }

        // Hack: revert original margins
        View webView = getWebView();
        ViewGroup wvParent = getParentView(webView);

        // cordova-android >= 15
        if (wvParent != null && origParentMargins != null)
        {
            ViewGroup.LayoutParams params = wvParent.getLayoutParams();
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
            marginParams.setMargins(
                origParentMargins.leftMargin,
                origParentMargins.topMargin,
                origParentMargins.rightMargin,
                origParentMargins.bottomMargin);
            wvParent.setLayoutParams(marginParams);
        }

        // cordova-android <= 14
        if (webView != null && origWebViewMargins != null)
        {
            FrameLayout.LayoutParams wvParams = (FrameLayout.LayoutParams) webView.getLayoutParams();
            wvParams.setMargins(
                origWebViewMargins.leftMargin,
                origWebViewMargins.topMargin,
                origWebViewMargins.rightMargin,
                origWebViewMargins.bottomMargin);
            webView.setLayoutParams(wvParams);
        }

        origWebViewMargins = null;
        origParentMargins = null;

        super.onDestroy();
    }

    private void removeBannerView(@NonNull AdView adView) {
        removeFromParentView(adView);
        adView.removeAllViews();
        adView.destroy();
    }

    private void addBannerView() {
        if (mAdView == null) return;

        // Added by Tamas Kuzmics (tomitank)
        // cordova-android 15+ edge-to-edge fix
        // cordova set margin to webView when edge-to-edge disabled
        // we need to consider the margin when calculating the banner position
        View webView = getWebView();
        FrameLayout.LayoutParams wvParams = (FrameLayout.LayoutParams) webView.getLayoutParams();

        if (getParentView(mAdView) == frameLayout && frameLayout != null) return;

        if (origWebViewMargins == null) {
            origWebViewMargins = new FrameLayout.LayoutParams(wvParams);
        }

        if (this.offset == null) {
            addBannerViewWithLinearLayout(webView, wvParams);
        } else {
            addBannerViewWithRelativeLayout(wvParams);
        }

        ViewGroup contentView = getContentView();
        if (contentView != null) {
            contentView.bringToFront();
            contentView.requestLayout();
            contentView.requestFocus();
        }
    }

    private void addBannerViewWithLinearLayout(View webView,FrameLayout.LayoutParams wvParams) {
        this.offset = 0; // Hack!
        addBannerViewWithRelativeLayout(wvParams);
        this.offset = null;

        final ViewGroup wvParent = getParentView(webView);
        final boolean hasDifferentParent = wvParent != null && wvParent != getContentView();

        // store parent original margins
        if (hasDifferentParent && origParentMargins == null) {
            ViewGroup.LayoutParams params = wvParent.getLayoutParams();
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
            origParentMargins = new ViewGroup.MarginLayoutParams(marginParams);
        }

        // Hack: add margin after banner height is determined
        mAdView.post(() -> {
            if (mAdView == null || wvParent == null) return;
            int bannerHeight = mAdView.getHeight();
            int topMargin    = isPositionTop() ? bannerHeight : 0;
            int bottomMargin = isPositionTop() ? 0 : bannerHeight;

            if (hasDifferentParent) { // cordova-android >= 15
                if (origParentMargins == null) return; // for safety
                ViewGroup.LayoutParams params = wvParent.getLayoutParams();
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
                marginParams.setMargins(
                    origParentMargins.leftMargin,
                    origParentMargins.topMargin + topMargin,
                    origParentMargins.rightMargin,
                    origParentMargins.bottomMargin + bottomMargin
                );
                wvParent.setLayoutParams(marginParams);
            } else { // set webView when contentView == parent (cordova-android <= 14)
                if (origWebViewMargins == null) return; // for safety
                wvParams.setMargins(
                    origWebViewMargins.leftMargin,
                    origWebViewMargins.topMargin + topMargin,
                    origWebViewMargins.rightMargin,
                    origWebViewMargins.bottomMargin + bottomMargin
                );
                webView.setLayoutParams(wvParams);
            }
        });
    }

    private void addBannerViewWithRelativeLayout(FrameLayout.LayoutParams wvParams) {
        FrameLayout.LayoutParams addParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        if (origWebViewMargins == null) return; // for safety
        addParams.gravity = isPositionTop() ? Gravity.TOP : Gravity.BOTTOM;
        int topOffset     = isPositionTop() ? this.offset + origWebViewMargins.topMargin : 0;
        int bottomOffset  = isPositionTop() ? 0 : this.offset + origWebViewMargins.bottomMargin;
        addParams.setMargins(0, topOffset, 0, bottomOffset);

        if (frameLayout == null) {
            frameLayout = new FrameLayout(getActivity());
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);

            ViewGroup contentView = getContentView();
            if (contentView != null)
            {
                contentView.addView(frameLayout, params);
            } else {
                Log.e(TAG, "Unable to find content view");
            }
        }

        removeFromParentView(mAdView);
        frameLayout.addView(mAdView, addParams);
        frameLayout.bringToFront();
    }

    private boolean isPositionTop() {
        return gravity == Gravity.TOP;
    }

    public enum AdSizeType {
        BANNER, LARGE_BANNER, MEDIUM_RECTANGLE, FULL_BANNER, LEADERBOARD, SMART_BANNER;

        @Nullable
        public static AdSize getAdSize(int adSize) {
            switch (AdSizeType.values()[adSize]) {
                case BANNER:
                    return AdSize.BANNER;
                case LARGE_BANNER:
                    return AdSize.LARGE_BANNER;
                case MEDIUM_RECTANGLE:
                    return AdSize.MEDIUM_RECTANGLE;
                case FULL_BANNER:
                    return AdSize.FULL_BANNER;
                case LEADERBOARD:
                    return AdSize.LEADERBOARD;
                case SMART_BANNER:
                    return AdSize.SMART_BANNER;
                default:
                    return null;
            }
        }
    }
}
