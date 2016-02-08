package pl.pzienowicz.calllooper;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatPreferenceActivity {

    BroadcastReceiver bcr = null;
    SharedPreferences sharedPreferences = null;
    Button callBtn = null;
    Thread thread = null;

    boolean isCallingActive = false;

    int redialAfterSeconds  = 2;
    int endCallAfterSeconds = 10;
    boolean speakerEnabled = false;

    public static final int PERMISSIONS_CALLING = 1;
    public static String[] REQUEST_CALLING_PERMISSION = {
            Manifest.permission.CALL_PHONE
    };

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            Context context = preference.getContext();

            if(preference.getClass() == SwitchPreference.class) {
                stringValue = Boolean.parseBoolean(stringValue) ? context.getString(R.string.enabled) : context.getString(R.string.disabled);
            }

            if (preference.getKey().equals("redialAfter") || preference.getKey().equals("endCallAfter")) {
                stringValue = context.getString(R.string.seconds, stringValue);
            }

            preference.setSummary(stringValue);
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        if(preference.getClass() == SwitchPreference.class) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getBoolean(preference.getKey(), false));
        } else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_general);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        bindPreferenceSummaryToValue(findPreference("endCallAfter"));
        bindPreferenceSummaryToValue(findPreference("redialAfter"));
        bindPreferenceSummaryToValue(findPreference("speakerEnabled"));

        callBtn = (Button) findViewById(R.id.callBtn);
        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                call();
            }
        });

        refreshAddedNumbersCounter();

        bcr = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                refreshAddedNumbersCounter();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(NumbersDialog.REFRESH_NUMBERS_COUNT);

        registerReceiver(bcr, filter);
    }

    private ArrayList<String> getAddedNumbers() {
        ArrayList<String> numbers = new ArrayList<>();
        for (int i = 1; i < 6; i++) {
            String n = sharedPreferences.getString("phone" + i, "");
            if (n.length() > 0) {
                numbers.add(n);
            }
        }

        return numbers;
    }

    private void refreshAddedNumbersCounter() {
        int addedNumbers = getAddedNumbers().size();
        Preference preference = findPreference("manageNumbers");
        preference.setSummary(String.valueOf(addedNumbers));
    }

    @Override
    public void onDestroy() {
        if (bcr != null) {
            unregisterReceiver(bcr);
        }

        super.onDestroy();
    }

    private void call() {
        if (!verifyCallingPermissions()) {
            return;
        }

        isCallingActive = !isCallingActive;

        callBtn.setText(isCallingActive ? R.string.stop_calling : R.string.call);
        callBtn.setBackgroundResource(isCallingActive ? R.drawable.style_stop_call_btn : R.drawable.style_call_btn);

        endCallAfterSeconds = Integer.parseInt(sharedPreferences.getString("endCallAfter", "10"));
        redialAfterSeconds = Integer.parseInt(sharedPreferences.getString("redialAfter", "2"));
        speakerEnabled = sharedPreferences.getBoolean("speakerEnabled", false);

        if(isCallingActive) {
//            Toast.makeText(getApplicationContext(), R.string.start_calling, Toast.LENGTH_LONG).show();

            thread = new Thread() {
                @Override
                public void run() {
                    ArrayList<String> numbers = getAddedNumbers();

                    while(isCallingActive) {
                        for(String number : numbers) {
                            if(!isCallingActive) {
                                return;
                            }

                            startCall(number);

                            try {
                                Thread.sleep(500);
                                if(speakerEnabled) {
                                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                    audioManager.setMode(AudioManager.MODE_IN_CALL);
                                    audioManager.setSpeakerphoneOn(true);
                                }

                                Thread.sleep(endCallAfterSeconds * 1000);
                                endCall();
                                Thread.sleep(redialAfterSeconds * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            };

            thread.start();
        } else {
//            Toast.makeText(getApplicationContext(), R.string.stop_calling, Toast.LENGTH_LONG).show();

            thread.interrupt();
        }
    }

    public boolean verifyCallingPermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    REQUEST_CALLING_PERMISSION,
                    PERMISSIONS_CALLING
            );

            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_CALLING: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    call();
                }
            }
        }
    }

    private void startCall(String number) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + number));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        callIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(callIntent);
    }

    private void endCall() {
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        Class<?> myClass = null;
        try {
            myClass = Class.forName(telephonyManager.getClass().getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Method method = null;
        try {
            assert myClass != null;
            method = myClass.getDeclaredMethod("getITelephony");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        assert method != null;
        method.setAccessible(true);
        ITelephony telephonyService = null;
        try {
            telephonyService = (ITelephony) method.invoke(telephonyManager);
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        assert telephonyService != null;
        telephonyService.endCall();
    }

}
