package id.indocoin.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
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
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUserAgentString(settings.getUserAgentString() + " IndoCoinApp/1.0");

        // Inject WalletConnect bridge
        webView.addJavascriptInterface(new WalletBridge(), "AndroidWallet");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Handle WalletConnect deep links
                if (url.startsWith("wc:") || url.startsWith("walletconnect:")) {
                    tryOpenWallet(url);
                    return true;
                }

                // Handle Trust Wallet deep link
                if (url.startsWith("trust:") || url.startsWith("trust-wallet:")) {
                    tryOpenWallet(url);
                    return true;
                }

                // Handle MetaMask deep link
                if (url.startsWith("metamask:")) {
                    tryOpenWallet(url);
                    return true;
                }

                // Stay in app for indocoin.id
                if (url.contains("indocoin.id")) {
                    return false;
                }

                // Open other links in browser
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    // ignore
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefresh.setRefreshing(false);

                // Inject JavaScript to handle wallet connection
                injectWalletScript(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        swipeRefresh.setOnRefreshListener(() -> webView.reload());
        swipeRefresh.setColorSchemeColors(0xFFC8922A);
        webView.loadUrl(APP_URL);
    }

    private void injectWalletScript(WebView view) {
        String script =
            "window.isIndoCoinApp = true;" +
            "window.openWallet = function(uri) {" +
            "  AndroidWallet.openWallet(uri);" +
            "};" +
            // Override alert for MetaMask detection
            "if (!window.ethereum) {" +
            "  window.ethereum = {" +
            "    isMetaMask: false," +
            "    isIndoCoinApp: true," +
            "    request: function(args) {" +
            "      return new Promise(function(resolve, reject) {" +
            "        reject(new Error('Please use WalletConnect'));" +
            "      });" +
            "    }" +
            "  };" +
            "}";
        view.evaluateJavascript("(function(){" + script + "})()", null);
    }

    private void tryOpenWallet(String uri) {
        // Try Trust Wallet first
        Intent trustIntent = getPackageManager()
            .getLaunchIntentForPackage("com.wallet.crypto.trustapp");
        if (trustIntent != null) {
            Intent wcIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            wcIntent.setPackage("com.wallet.crypto.trustapp");
            try {
                startActivity(wcIntent);
                return;
            } catch (Exception e) { }
        }

        // Try MetaMask
        Intent mmIntent = getPackageManager()
            .getLaunchIntentForPackage("io.metamask");
        if (mmIntent != null) {
            Intent wcIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            wcIntent.setPackage("io.metamask");
            try {
                startActivity(wcIntent);
                return;
            } catch (Exception e) { }
        }

        // Open chooser
        try {
            Intent chooser = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(Intent.createChooser(chooser, "Pilih Wallet"));
        } catch (Exception e) { }
    }

    public class WalletBridge {
        @JavascriptInterface
        public void openWallet(String uri) {
            runOnUiThread(() -> tryOpenWallet(uri));
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }
}
