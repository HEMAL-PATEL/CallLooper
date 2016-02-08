package pl.pzienowicz.calllooper;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class NumbersDialog extends Dialog {

    public static final String REFRESH_NUMBERS_COUNT = "REFRESH_NUMBERS_COUNT";

    SharedPreferences sharedPreferences;
    EditText phone1 = null;
    EditText phone2 = null;
    EditText phone3 = null;
    EditText phone4 = null;
    EditText phone5 = null;

    public NumbersDialog(Context context) {
        super(context);

        setCancelable(false);
        setContentView(R.layout.dialog_numbers);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        phone1 = (EditText) findViewById(R.id.phone1);
        phone2 = (EditText) findViewById(R.id.phone2);
        phone3 = (EditText) findViewById(R.id.phone3);
        phone4 = (EditText) findViewById(R.id.phone4);
        phone5 = (EditText) findViewById(R.id.phone5);

        Button saveBtn = (Button) findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveNumbers();
                dismiss();
            }
        });

        Button cancelBtn = (Button) findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        loadNumbers();
    }

    private void loadNumbers()
    {
        phone1.setText(sharedPreferences.getString("phone1", ""));
        phone2.setText(sharedPreferences.getString("phone2", ""));
        phone3.setText(sharedPreferences.getString("phone3", ""));
        phone4.setText(sharedPreferences.getString("phone4", ""));
        phone5.setText(sharedPreferences.getString("phone5", ""));
    }

    private void saveNumbers()
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("phone1", phone1.getText().toString());
        editor.putString("phone2", phone2.getText().toString());
        editor.putString("phone3", phone3.getText().toString());
        editor.putString("phone4", phone4.getText().toString());
        editor.putString("phone5", phone5.getText().toString());

        editor.apply();

        Intent i = new Intent(REFRESH_NUMBERS_COUNT);
        getContext().sendBroadcast(i);
    }

}
