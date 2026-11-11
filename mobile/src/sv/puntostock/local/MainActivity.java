package sv.puntostock.local;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;

/** APK de prueba para servidor privado por Wi-Fi o loopback mediante USB. */
public final class MainActivity extends Activity {
  private WebView web;
  private ValueCallback<Uri[]> chooser;
  private byte[] pendingPdf;
  private LinearLayout shell;
  private static final String HOME = "http://127.0.0.1:14200/";
  private volatile String home = HOME;

  private boolean local(Uri uri) {
    Uri server = Uri.parse(home);
    return "http".equals(uri.getScheme()) && server.getHost().equals(uri.getHost()) && server.getPort() == uri.getPort();
  }

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    String defaultServer = HOME;
    try (java.io.InputStream in = getAssets().open("server.txt")) {
      byte[] bytes = new byte[512]; int count = in.read(bytes);
      String parsed = LocalServer.normalize(new String(bytes,0,count,"UTF-8"));
      if (parsed != null) defaultServer = parsed;
    } catch (java.io.IOException ignored) {}
    home = getPreferences(MODE_PRIVATE).getString("servidor",defaultServer);
    if (LocalServer.normalize(home) == null) home = HOME;
    web = new WebView(this);
    shell = new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL);
    shell.setOnApplyWindowInsetsListener((v, insets) -> {
      v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
      return insets;
    });
    LinearLayout toolbar = new LinearLayout(this);
    Button connection = new Button(this); connection.setText("Conexión");
    connection.setOnClickListener(v -> chooseServer()); toolbar.addView(connection);
    Button reload = new Button(this); reload.setText("Recargar");
    reload.setOnClickListener(v -> web.reload()); toolbar.addView(reload);
    shell.addView(toolbar);
    shell.addView(web,new LinearLayout.LayoutParams(-1,0,1));
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setDomStorageEnabled(true);
    web.getSettings().setAllowFileAccess(false);
    web.getSettings().setAllowContentAccess(false);
    web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    CookieManager.getInstance().setAcceptCookie(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
    web.addJavascriptInterface(new PdfDownloads(), "PuntoStockLocalPdf");
    web.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
        return !local(req.getUrl());
      }
      @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
        String scheme = req.getUrl().getScheme();
        if (local(req.getUrl()) || "data".equals(scheme) || "blob".equals(scheme)) return null;
        return new WebResourceResponse("text/plain", "UTF-8", 403, "Solo acceso local",
          java.util.Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
      }
      @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError error) {
        if (req.isForMainFrame()) runOnUiThread(() -> connectionHelp());
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
        new AlertDialog.Builder(MainActivity.this).setTitle("PuntoStock").setMessage(message)
          .setPositiveButton("Aceptar", (d,w) -> result.confirm())
          .setNegativeButton("Cancelar", (d,w) -> result.cancel())
          .setOnCancelListener(d -> result.cancel()).show();
        return true;
      }
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (chooser != null) chooser.onReceiveValue(null);
        chooser = callback;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*")
          .addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(intent, 10); }
        catch (android.content.ActivityNotFoundException e) { chooser.onReceiveValue(null); chooser = null; }
        return true;
      }
    });
    web.setDownloadListener((url, agent, disposition, mime, length) -> {
      if (url.startsWith("blob:")) {
        web.evaluateJavascript("fetch(" + org.json.JSONObject.quote(url) + ").then(r=>r.blob()).then(b=>{const f=new FileReader();f.onload=()=>PuntoStockLocalPdf.save(f.result);f.readAsDataURL(b);})", null);
      }
    });
    setContentView(shell);
    web.loadUrl(home);
  }

  private void chooseServer() {
    EditText input = new EditText(this);
    input.setSingleLine(true);input.setText(home);
    input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
    AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Servidor de tu red privada")
      .setMessage("Escribe la dirección que muestra la computadora. Ejemplo: http://192.168.1.10:14201")
      .setView(input).setPositiveButton("Conectar",null).setNegativeButton("Cancelar",null).create();
    dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
      String server = LocalServer.normalize(input.getText().toString());
      if (server == null) { input.setError("Usa una IPv4 privada y un puerto válido, sin rutas adicionales.");return; }
      web.stopLoading(); pendingPdf = null;
      home = server;
      getPreferences(MODE_PRIVATE).edit().putString("servidor",home).apply();
      web.clearHistory();setContentView(shell);web.loadUrl(home);dialog.dismiss();
    }));
    dialog.show();
  }

  private void connectionHelp() {
    LinearLayout panel = new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(32,80,32,32);
    TextView text = new TextView(this);
    text.setText("PuntoStock local\n\nNo se pudo conectar a " + home + "\n\nConecta el teléfono y la computadora al mismo Wi-Fi personal e inicia mobile/Iniciar-Wifi.ps1 en la computadora.\n\nLa app necesita el servidor encendido. No usa túneles ni servicios públicos.");
    text.setTextSize(18); panel.addView(text);
    Button retry = new Button(this); retry.setText("Volver a intentar");
    retry.setOnClickListener(v -> {setContentView(shell);web.loadUrl(home);});panel.addView(retry);
    Button config = new Button(this);config.setText("Cambiar servidor");config.setOnClickListener(v -> chooseServer());panel.addView(config);
    setContentView(panel);
  }

  private final class PdfDownloads {
    @JavascriptInterface public void save(String data) {
      if (data == null || data.length() > 24000000 || !data.startsWith("data:application/pdf;base64,")) return;
      try {
        byte[] bytes = Base64.decode(data.substring(data.indexOf(',')+1), Base64.DEFAULT);
        if (bytes.length < 5 || bytes[0]!=37 || bytes[1]!=80 || bytes[2]!=68 || bytes[3]!=70) return;
        runOnUiThread(() -> {
          if (pendingPdf != null) return;
          pendingPdf = bytes;
          Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/pdf").putExtra(Intent.EXTRA_TITLE,"PuntoStock-comprobante.pdf");
          startActivityForResult(save, 11);
        });
      } catch (IllegalArgumentException ignored) {}
    }
  }

  @Override protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request,result,data);
    if (request == 10 && chooser != null) {
      chooser.onReceiveValue(result == RESULT_OK && data != null && data.getData()!=null ? new Uri[]{data.getData()} : null);
      chooser = null;
    }
    if (request == 11 && pendingPdf != null) {
      try {
        if (result == RESULT_OK && data != null && data.getData()!=null) {
          try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {out.write(pendingPdf);}
        }
      } catch (java.io.IOException e) {
        new AlertDialog.Builder(this).setMessage("No se pudo guardar el PDF. Inténtalo de nuevo.").setPositiveButton("Aceptar",null).show();
      } finally {pendingPdf = null;}
    }
  }
  @Override public void onBackPressed() {
    if (web.canGoBack()) web.goBack(); else super.onBackPressed();
  }
  @Override protected void onDestroy() {
    if (chooser != null) chooser.onReceiveValue(null);
    web.removeJavascriptInterface("PuntoStockLocalPdf");web.destroy();super.onDestroy();
  }
}
