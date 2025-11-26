package org.me.gcu.home_daniel_s2221676;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class CurrencyStrengthAdapter extends ArrayAdapter{

    private ArrayList<String> items;
    private ArrayList<CurrencyItem> currencyItems;
    private CurrencyViewModel viewModel;

    public CurrencyStrengthAdapter(Context context, ArrayList<String> items, ArrayList<CurrencyItem> currencyItems, CurrencyViewModel viewModel) {
        super (context, 0, items);
        this.items = items;
        this.currencyItems = currencyItems;
        this.viewModel = viewModel;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            row = inflater.inflate(R.layout.list_currency_items, parent, false);
        }

        TextView textView = row.findViewById(R.id.text1);
        String text = items.get(position);
        textView.setText(text);

        if (currencyItems != null && position < currencyItems.size()) {
            CurrencyItem item = currencyItems.get(position);
            int band = viewModel.getStrengthBand(item.getDescription());
            int color;
            switch (band) {
                case 1: color = ContextCompat.getColor(getContext(),R.color.rate_strong);
                break;
                case 2: color = ContextCompat.getColor(getContext(),R.color.rate_good);
                    break;
                case 3: color = ContextCompat.getColor(getContext(),R.color.rate_weak);
                    break;
                case 4:
                default:
                    color = ContextCompat.getColor(getContext(),R.color.rate_poor);
                    break;
            }
            row.setBackgroundColor(color);
        } else {
            row.setBackgroundColor(0x00000000);
        }
        return row;
    }
    public void updateData (ArrayList<String> newTexts, ArrayList<CurrencyItem> newItems) {
        items.clear();
        items.addAll(newTexts);
        currencyItems.clear();
        currencyItems.addAll(newItems);
        notifyDataSetChanged();
    }
}
