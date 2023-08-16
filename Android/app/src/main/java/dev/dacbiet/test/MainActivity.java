package dev.dacbiet.test;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcB;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Base64;
import android.util.Log;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import dev.dacbiet.opusenclient.Client;
import dev.dacbiet.opusenclient.NfcHandler;
import dev.dacbiet.opusenclient.actions.ActionException;
import dev.dacbiet.test.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    public static final String TAG = "CardReaderFragment";
    public static int FLAG_OPUS = NfcAdapter.FLAG_READER_NFC_B;
    public static int FLAG_MIFARE = NfcAdapter.FLAG_READER_NFC_A;

    private NfcAdapter mAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private ClientManager clientManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        this.clientManager = new ClientManager();

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE);

        // Setup an intent filter for all MIME based dispatches
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        IntentFilter td = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mFilters = new IntentFilter[] {
                ndef, td
        };

        // Setup a tech list for all NfcF tags
        mTechLists = new String[][] {
                new String[] { NfcB.class.getName() },
                new String[] { IsoDep.class.getName() }
        };

        new AlertDialog.Builder(this)
                .setTitle("Disclaimer")
                .setMessage("This app is a prototype. I am not responsible for any monetary losses that may occur.")
                .setIcon(R.drawable.baseline_warning_24)
                .setPositiveButton(R.string.accept, (dialog, whichButton) -> this.loadNewOpusEnLigneInstance())
                .setNegativeButton(R.string.decline, (dialog, whichButton) -> {
                    // kill app if refuse
                    this.finish(); //
                    System.exit(0);
                }).show();

    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdapter != null) {
            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            mAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_B,
                    options
            );
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        try {
            if (mAdapter != null) {
                mAdapter.disableReaderMode(this);
            }
        } catch(NullPointerException e) {

        }

    }

    @Override
    public void onTagDiscovered(Tag tag) {
        IsoDep OpusCard = IsoDep.get(tag);


        // if disconnect, kill client
        Log.i(TAG, "found tag from getParcelableExtra");
        try {
            if(OpusCard != null) {

                Log.i(TAG, "Opus detected");
                OpusCard.connect();

                // mega sus handling
                if(OpusCard.isConnected()) {
                    Log.i(TAG, "Opus connected");

                    NfcHandler nfcHandler = (data) -> {
                        try {
//                            System.out.println("NFC command data: " + ClientManager.bytesToHex(data));
                            byte[] resp = OpusCard.transceive(data);
//                            System.out.println("NFC response data: " + ClientManager.bytesToHex(resp));
                            return resp;
                        } catch (IOException e) {
                            if(e instanceof TagLostException) {
                                vibrate();
                            }
                            e.printStackTrace();
                        }
                        return null;
                    };


                    if (!clientManager.isRunning() && clientManager.getClient() != null) {
                        Thread t = new Thread(() -> {
                            clientManager.startClient(nfcHandler);
                            Client client = clientManager.getClient();

                            if (client == null) {
                                return;
                            }
                            client.start();

                            try {

                                client.awaitPreInit();
                                Log.i("S_CLIENT", "Begin execution of init actions...");
                                while (clientManager.isRunning() && !client.isFinished() && client.hasActions()) {
                                    if (!OpusCard.isConnected()) {
                                        throw new ActionException("Opus disconnected while executing initial actions.");
                                    }

                                    if (client.hasActions()) {
                                        client.executeAction();
                                    }
                                    // sleep
                                    Thread.sleep(10);
                                }
                                if (!clientManager.isRunning()) {
                                    throw new Exception("Client should stop (init)");
                                }

                                client.awaitInit();
                                Log.i("S_CLIENT", "Begin execution of actions...");
                                while (clientManager.isRunning() && !client.isFinished()) {
                                    if (!OpusCard.isConnected()) {
                                        throw new ActionException("Opus disconnected while executing actions.");
                                    }

                                    if (client.hasActions()) {
                                        client.executeAction();
                                    }
                                    // sleep
                                    Thread.sleep(10);
                                }
                                if (!clientManager.isRunning()) {
                                    throw new Exception("Client should stop ()");
                                }

                                if(!client.isFinished()) {
                                    client.shutdown(false);
                                }
                                clientManager.reset();

                                Log.i("S_CLIENT", "Thread dead");

                            } catch (InterruptedException e) {
                                System.out.println("Issue while executing client...");
                                e.printStackTrace();
                            } catch (ActionException e) {
                                System.out.println("Opus disconnection while running client...");
                                e.printStackTrace();
                            } catch (Exception e) {
                                System.out.println("Something happened to the client execution...");
                                e.printStackTrace();
                            } finally {
                                try {
                                    client.shutdown(false);

                                    OpusCard.close();
                                    vibrate();
                                } catch (IOException e) {

                                }
                            }

                        });
                        t.start();
                    } else {
                        // perhaps needs to be thread safe
                        clientManager.reset();
                    }

                }
                Log.i(TAG, "-----------------------------------------------------------------------");
            }
            else {
                Log.i(TAG, "null opus card idk");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            new AlertDialog.Builder(this)
                    .setTitle("Reset page?")
                    .setMessage("If OPUS en ligne is currently processing/performing some action, you may lose your progress/money. Are you sure you want to reset the page?")
                    .setIcon(R.drawable.baseline_warning_24)
                    .setPositiveButton(R.string.accept, (dialog, whichButton) -> this.loadNewOpusEnLigneInstance())
                    .setNegativeButton(R.string.decline, null).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadNewOpusEnLigneInstance() {
        WebView wb = binding.webView;

        wb.clearHistory();
        wb.clearFormData();
        wb.clearCache(true);
        android.webkit.CookieManager.getInstance().removeAllCookies(null);

        wb.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");
        wb.getSettings().setLoadsImagesAutomatically(true);
        wb.getSettings().setJavaScriptEnabled(true);
        wb.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        wb.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String u = request.getUrl().toString();
                Log.i(TAG, "URL = " + u);

                if(u.startsWith("smartcard://")) {
                    clientManager.createClient(u);
                    Log.i(TAG, "Created new client");
                } else {
                    wb.loadUrl(u);
                }

                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // Inject CSS when page is done loading
                injectCSS();
                super.onPageFinished(view, url);
            }
        });

        wb.loadUrl("https://opusenligne.ca/en/recharge");
        this.injectCSS();
    }

    private void injectCSS() {
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.inject_styles);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            binding.webView.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}