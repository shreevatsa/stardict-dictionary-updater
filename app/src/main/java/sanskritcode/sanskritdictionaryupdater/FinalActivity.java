package sanskritcode.sanskritdictionaryupdater;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.util.List;

public class FinalActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (dictIndexStore == null) {
            dictIndexStore = (DictIndexStore) getIntent().getSerializableExtra("dictIndexStore");
        }
        setContentView(R.layout.activity_final);
        TextView topText = findViewById(R.id.final_act_textView);
        topText.setText(getString(R.string.finalMessage));
        final StringBuilder failures = new StringBuilder("");
        for (DictInfo dictInfo: dictIndexStore.dictionariesSelectedMap.values()) {
            //noinspection StatementWithEmptyBody
            if (dictInfo.status == DictStatus.DOWNLOAD_FAILURE || dictInfo.status == DictStatus.EXTRACTION_FAILURE ) {
                failures.append("\n").append(DictNameHelper.getNameWithoutAnyExtension(dictInfo.url));
            } else {
            }
        }
        if (failures.length() > 0) {
            topText.append("\n" + "Failed on:" + failures);
            Log.w(getLocalClassName(), failures.toString());
        }
        StringBuilder successes = new StringBuilder("");
        for (DictInfo dictInfo: dictIndexStore.dictionariesSelectedMap.values()) {
            //noinspection StatementWithEmptyBody
            if (dictInfo.status == DictStatus.EXTRACTION_SUCCESS) {
            } else {
                successes.append("\n").append(DictNameHelper.getNameWithoutAnyExtension(dictInfo.url));
            }
        }
        if (successes.length() > 0) {
            topText.append("\n" + "Succeeded on:" + successes);
            Log.i(getLocalClassName(), successes.toString());
        }

        Button button = findViewById(R.id.final_act_button);
        button.setText(R.string.buttonValQuit);
        if (failures.length() == 0) {
            button.setEnabled(true);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finishAffinity();
                }
            });
        } else {
            button.setEnabled(false);
        }

        final BaseActivity thisActivity = this;
        Button button_2 = findViewById(R.id.final_act_button_2);
        button_2.setText(R.string.PROBLEM_SEND_LOG);
        button_2.setVisibility(View.VISIBLE);
        button_2.setEnabled(true);
        button_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thisActivity.sendLoagcatMail();
                finishAffinity();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, GetUrlActivity.class);
        intent.putExtra("dictIndexStore", dictIndexStore);
        // intent.putStringArrayListExtra();
        startActivity(intent);
    }

    public void buttonPressed1(@SuppressWarnings("UnusedParameters") View v) {
        finish();
    }
}