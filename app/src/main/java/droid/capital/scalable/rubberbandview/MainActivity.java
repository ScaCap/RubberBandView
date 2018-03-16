package droid.capital.scalable.rubberbandview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import capital.scalable.droid.rubberbandview.RubberBandListener;
import capital.scalable.droid.rubberbandview.RubberBandView;

public class MainActivity extends AppCompatActivity {

    private TextView selectedValue;
    private RubberBandView rubberBandView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectedValue = findViewById(R.id.selectedValue);
        rubberBandView = findViewById(R.id.valueSelector);
        final EditText inputMaxValue = findViewById(R.id.inputMaxSelection);

        rubberBandView.setMaxSelection(10);
        updateSelectedValue(rubberBandView.getSelection());

        inputMaxValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    try {
                        int maxValue = Integer.valueOf(inputMaxValue.getText().toString());
                        rubberBandView.setMaxSelection(maxValue);
                    } catch (NumberFormatException e) {}
                }
                return false;
            }
        });

        rubberBandView.setListener(new RubberBandListener() {
            @Override
            public void onSelectionChanged(int value) {
                updateSelectedValue(value);
            }

            @Override
            public void onSelectionFinished(int value) {
                Toast.makeText(MainActivity.this, "Selection ended", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSelectedValue(int value) {
        selectedValue.setText("Selected value = " + value);
    }
}
