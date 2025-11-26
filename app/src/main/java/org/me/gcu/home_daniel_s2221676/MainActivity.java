/*  Starter project for Mobile Platform Development - 1st diet 25/26
    You should use this project as the starting point for your assignment.
    This project simply reads the data from the required URL and displays the
    raw data in a TextField
*/

//
// Name                 Daniel Home
// Student ID           S2221676
// Programme of Study   BSc(Hons) Software Development
//

package org.me.gcu.home_daniel_s2221676;


import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;




public class MainActivity extends AppCompatActivity {
    private ViewSwitcher mainSwitcher;
    private TextView convertingTitle;
    private TextView convertingDescription;
    private TextView convertedResult;
    private EditText inputAmount;
    private ImageView flagImageView;
    private Button convertToOther;
    private Button convertFromOther;
    private Button returnButton;
    private TextView rawDataDisplay;
    private EditText searchText;
    private Button searchButton;
    private ArrayAdapter<String> currencyAdapter;
    private ArrayList<String> currencyDisplayList = new ArrayList<>();
    private CurrencyViewModel currencyViewModel;
    private ArrayList<CurrencyItem> filteredForAdapter = new ArrayList<>();
    private static final String Key_Child_Index = "mainSwitcherChildIndex";
    private static final String Key_Converted_Result = "convertedResult";
    private static final String Key_Input_Amount = "inputAmount";






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rawDataDisplay = findViewById(R.id.rawDataDisplay);
        ListView currencyListView = findViewById(R.id.currencyListView);
        mainSwitcher = findViewById(R.id.mainSwitcher);
        convertingTitle = findViewById(R.id.convertingTitle);
        convertingDescription = findViewById(R.id.convertingDescription);
        convertedResult = findViewById(R.id.convertedResult);
        inputAmount = findViewById(R.id.inputAmount);
        convertToOther = findViewById(R.id.convertToOther);
        convertFromOther = findViewById(R.id.convertFromOther);
        searchText = findViewById(R.id.searchText);
        searchButton = findViewById(R.id.searchButton);
        returnButton = findViewById(R.id.returnButton);
        flagImageView = findViewById(R.id.flagImageView);


        currencyViewModel = new ViewModelProvider(this).get(CurrencyViewModel.class);
        currencyViewModel.getCurrencyDisplayListLive().observe(this, list -> {
            currencyDisplayList.clear();
            if (list != null) {
                currencyDisplayList.addAll(list);
            }
            filteredForAdapter.clear();
            filteredForAdapter.addAll(currencyViewModel.getFilteredCurrencyItems());
            currencyAdapter.notifyDataSetChanged();
        });
        currencyViewModel.getSelectedItemLive().observe(this, item -> {
            if (item != null) {
                showConversionView(item);
                currencyViewModel.clearSelectedItem();
            }
        });
        currencyViewModel.getRawDataDisplayLive().observe(this, text -> {
            if (text != null) {
                rawDataDisplay.setText(text);
            }
        });
        currencyViewModel.getFlagBitmapLive().observe(this, bitmap -> {
            if (bitmap != null) {
                flagImageView.setImageBitmap(bitmap);
            } else {
                flagImageView.setImageDrawable(null);
            }
        });
        currencyAdapter = new CurrencyStrengthAdapter(this,
                currencyDisplayList,
                filteredForAdapter,
                currencyViewModel
        );
        currencyListView.setAdapter(currencyAdapter);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currencyViewModel.performSearch(searchText.getText().toString());
            }
        });

        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainSwitcher.setDisplayedChild(0);
            }
        });

        currencyListView.setOnItemClickListener((parent, view, position, id) -> {
            currencyViewModel.selectItem(position);
        });

        if (savedInstanceState != null) {
            int childIndex = savedInstanceState.getInt(Key_Child_Index, 0);
            mainSwitcher.setDisplayedChild(childIndex);
            if (childIndex == 1) {
                CurrencyItem item = currencyViewModel.getLastSelectedItem();
                showConversionView(item);

                String result = savedInstanceState.getString(Key_Converted_Result, "");
                String amount = savedInstanceState.getString(Key_Input_Amount, "");

                convertedResult.setText(result);
                inputAmount.setText(amount);
            }
        } else{
            mainSwitcher.setDisplayedChild(0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mainSwitcher != null) {
            outState.putInt(Key_Child_Index, mainSwitcher.getDisplayedChild());
        }

        if (convertedResult != null) {
            outState.putString(Key_Converted_Result, convertedResult.getText().toString());
        }
        if (inputAmount != null) {
            outState.putString(Key_Input_Amount, inputAmount.getText().toString());
        }
    }


        private void showConversionView (CurrencyItem item){
            convertingTitle.setText("Currency Converter For: " + item.getTitle());
            convertingDescription.setText(item.getDescription());
            inputAmount.setText("");
            convertedResult.setText("");

            currencyViewModel.loadFlagForCurrencyTitle(item.getTitle());

            convertToOther.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        double rate = currencyViewModel.getRate(item.getDescription());
                        double total = Double.parseDouble(inputAmount.getText().toString());
                        double convertedtotal = total * rate;
                        convertedResult.setText(String.format("GBP to Other: %.2f", convertedtotal));
                    } catch (RuntimeException e) {
                        convertedResult.setText("Incorrect Amount Input");
                    }
                }
            });
            convertFromOther.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        double rate = currencyViewModel.getRate(item.getDescription());
                        double total = Double.parseDouble(inputAmount.getText().toString());
                        double convertedtotal = total / rate;
                        convertedResult.setText(String.format("Other to GBP: %.2f", convertedtotal));
                    } catch (RuntimeException e) {
                        convertedResult.setText("Incorrect Amount Input");
                    }
                }
            });
            mainSwitcher.setDisplayedChild(1);
        }

}//