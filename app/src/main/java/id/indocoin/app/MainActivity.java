package id.indocoin.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private static final String APP_URL = "https://indocoin.id";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        webView = findViewById(R.id.webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setAllowFileAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUserAgentString(settings.getUserAgentString() + " IndoCoinApp/1.0");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Handle semua deep link wallet
                if (url.startsWith("trust:") ||
                    url.startsWith("tpdapp:") ||
                    url.startsWith("metamask:") ||
                    url.startsWith("wc:") ||
                    url.startsWith("tokenpocket:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        // App tidak terinstall, buka Play Store
                        openPlayStore(url);
                    }
                    return true;
                }

                // Handle link.trustwallet.com dan metamask.app.link
                if (url.contains("link.trustwallet.com") ||
                    url.contains("metamask.app.link") ||
                    url.contains("tokenpocket.pro")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) { }
                    return true;
                }

                // Tetap di dalam app untuk indocoin.id
                if (url.contains("indocoin.id")) return false;

                // Link lain buka di browser
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) { }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefresh.setRefreshing(false);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        swipeRefresh.setOnRefreshListener(() -> webView.reload());
        swipeRefresh.setColorSchemeColors(0xFFC8922A);
        webView.loadUrl(APP_URL);
    }

    private void openPlayStore(String url) {
        String pkg = "";
        if (url.startsWith("trust:")) pkg = "com.wallet.crypto.trustapp";
        else if (url.startsWith("tpdapp:") || url.startsWith("tokenpocket:")) pkg = "vip.mytokenpocket";
        else if (url.startsWith("metamask:")) pkg = "io.metamask";

        if (!pkg.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + pkg));
                startActivity(intent);
            } catch (Exception e) { }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onResume() { super.onResume(); webView.onResume(); }

    @Override
    protected void onPause() { super.onPause(); webView.onPause(); }
}
