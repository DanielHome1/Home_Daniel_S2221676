package org.me.gcu.home_daniel_s2221676;


import android.app.Application;
import android.util.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.AndroidViewModel;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

public class CurrencyViewModel extends AndroidViewModel{
    private String urlSource="https://www.fx-exchange.com/gbp/rss.xml";
    private MutableLiveData<ArrayList<CurrencyItem>> currencyItemsLive = new MutableLiveData<>(new ArrayList<CurrencyItem>());
    private MutableLiveData<ArrayList<String>> currencyDisplayListLive = new MutableLiveData<>(new ArrayList<String>());
    private MutableLiveData<String> rawDataDisplayLive = new MutableLiveData<>();
    private MutableLiveData<CurrencyItem> selectedItemLive = new MutableLiveData<>();
    private ArrayList<CurrencyItem> filteredCurrencyItems = new ArrayList<>();
    private android.os.Handler autoHandler = new android.os.Handler();
    private Runnable autoRunnable;
    private static int Refresh_Interval_ms = 60 * 60 * 1000; //set for 1 hour
    //private static int Refresh_Interval_ms = 10 * 1000; //set for 10 seconds for testing
    private HashMap<String, String> countryMap = new HashMap<>();
    private MutableLiveData<Bitmap> flagBitmapLive = new MutableLiveData<>();
    private HashMap<String, String> iso2Map = new HashMap<>();
    private String result;
    private CurrencyItem lastSelectedItem;
    public CurrencyViewModel(Application application) {
        super(application);
        loadCurrencyCountry();
        startProgress();
        startAutoRefresh();
    }

    private void loadCurrencyCountry() {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = getApplication().getResources().getXml(R.xml.currency_for_countries);
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xpp.getName().equalsIgnoreCase("currency")) {
                    String code = xpp.getAttributeValue(null, "code");
                    String country = xpp.getAttributeValue(null, "country");
                    String iso2 = xpp.getAttributeValue(null, "iso2");
                    if (code != null && country != null) {
                        countryMap.put(code.toUpperCase(), country);
                    }
                    if (code != null && iso2 != null && !iso2.isEmpty()) {
                        iso2Map.put(code.toUpperCase(), iso2.toLowerCase());
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException e) {
            Log.e("Parsing", "EXCEPTION" + e);
            //throw new RuntimeException(e);
        } catch (IOException e) {
            Log.e("Parsing","I/O EXCEPTION" + e);
            throw new RuntimeException(e);
        }
    }
    // Need separate thread to access the internet resource over network
    // Other neater solutions should be adopted in later iterations.
    public void startProgress()
    {
        new Thread(new Task(urlSource)).start();
    }
    private void startAutoRefresh() {
        autoRunnable = new Runnable() {
            @Override
            public void run() {
                startProgress();
                autoHandler.postDelayed(this, Refresh_Interval_ms);
            }
        };
        autoHandler.postDelayed(autoRunnable, Refresh_Interval_ms);
    }
    //getters for LiveData Stuff
    public LiveData<ArrayList<String>> getCurrencyDisplayListLive() {
        return currencyDisplayListLive;
    }
    public LiveData<String> getRawDataDisplayLive(){
        return rawDataDisplayLive;
    }
    public LiveData<CurrencyItem> getSelectedItemLive() {
        return selectedItemLive;
    }
    public LiveData<Bitmap> getFlagBitmapLive() {
        return flagBitmapLive;
    }
    public ArrayList<CurrencyItem> getFilteredCurrencyItems() {
        return filteredCurrencyItems;
    }
    public CurrencyItem getLastSelectedItem() {
        return lastSelectedItem;
    }

    public void selectItem(int position) {
        ArrayList<CurrencyItem> list = filteredCurrencyItems;
        if (list != null && position >= 0 && position < list.size()) {
            CurrencyItem item = list.get(position);
            lastSelectedItem = item;
            selectedItemLive.setValue(list.get(position));
        }
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        if (autoHandler != null && autoRunnable != null) {
            autoHandler.removeCallbacks(autoRunnable);
        }
    }

    public void clearSelectedItem() {
        selectedItemLive.setValue(null);
    }

    private class Task implements Runnable
    {
        private String url;
        public Task(String aurl){
            url = aurl;
        }
        @Override
        public void run(){
            result = "";
            URL aurl;
            URLConnection yc;
            BufferedReader in = null;
            String inputLine = "";


            Log.d("MyTask","in run");

            try
            {
                Log.d("MyTask","in try");
                aurl = new URL(url);
                yc = aurl.openConnection();
                in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
                while ((inputLine = in.readLine()) != null){
                    result = result + inputLine;
                }
                in.close();
            }
            catch (IOException ae) {
                Log.e("MyTask", "ioexception");
                rawDataDisplayLive.postValue("Unable to Update rates, no internet connection.");
                return;
            }

            if (result == null || result.isEmpty() || !result.contains("<?")|| !result.contains("</rss")) {
                rawDataDisplayLive.postValue("Unable to read Feed Data. Check Connection");
                return;
            }

            //Clean up any leading garbage characters
            int i = result.indexOf("<?"); //initial tag
            result = result.substring(i);

            //Clean up any trailing garbage at the end of the file
            i = result.indexOf("</rss>"); //final tag
            result = result.substring(0, i + 6);

            // Now that you have the xml data into result, you can parse it

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput( new StringReader( result ) );
                ArrayList<CurrencyItem> localCurrencyItems = new ArrayList<>();
                boolean inAnItem = false;
                CurrencyItem currencyItem = null;
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT)
                {
                    if(eventType == XmlPullParser.START_TAG)
                    {
                        if (xpp.getName().equalsIgnoreCase("item"))
                        {
                            inAnItem = true;

                            currencyItem = new CurrencyItem();
                            Log.d("MyTag","New Thing found!");
                        }
                        else if (xpp.getName().equalsIgnoreCase("title"))
                        {

                            String temp = xpp.nextText();
                            if(inAnItem){
                                currencyItem.setTitle(temp);
                                Log.d("MyTag","Title is " + temp);
                            }
                        }
                        else if (xpp.getName().equalsIgnoreCase("description"))
                        {
                            String temp = xpp.nextText();
                            if(inAnItem){
                                currencyItem.setDescription(temp);
                                Log.d("MyTag","Description is: " + temp);
                            }
                        }
                        else if (xpp.getName().equalsIgnoreCase("pubDate"))
                        {

                            String temp = xpp.nextText();
                            if(inAnItem){
                                currencyItem.setPubDate(temp);
                                Log.d("MyTag","PubDate is " + temp);
                            }
                        }
                        else if (xpp.getName().equalsIgnoreCase("link"))
                        {

                            String temp = xpp.nextText();
                            if(inAnItem){
                                currencyItem.setLink(temp);
                                Log.d("MyTag","Link is " + temp);
                            }
                        }
                    }
                    else if(eventType == XmlPullParser.END_TAG)
                    {
                        if (xpp.getName().equalsIgnoreCase("item"))
                        {
                            localCurrencyItems.add(currencyItem);
                            inAnItem = false;
                            Log.d("MyTag","Thing parsing completed!");
                        }
                        Log.d("MyTag", "Number of items parsed: " + localCurrencyItems.size());
                    }
                    eventType = xpp.next();
                }

                ArrayList<String> localDisplayList = new ArrayList<>();
                for (CurrencyItem item : localCurrencyItems){
                    localDisplayList.add("Exchange rate for " + item.getTitle()
                            + ":\n" + item.getDescription()
                            + "\nUpdated Data On: " + item.getPubDate()
                    );
                }
                filteredCurrencyItems = new ArrayList<>(localCurrencyItems);

                currencyItemsLive.postValue(localCurrencyItems);
                currencyDisplayListLive.postValue(localDisplayList);
                rawDataDisplayLive.postValue("All Currency Exchange Rates Updated.");

            } catch (XmlPullParserException e) {
                Log.e("Parsing","EXCEPTION" + e);
                //throw new RuntimeException(e);
            } catch (IOException e) {
                Log.e("Parsing","I/O EXCEPTION" + e);
                throw new RuntimeException(e);
            }

        }


    }
    public void loadFlagForCurrencyTitle(String title){
        String code = getCodeFromTitle(title);
        String iso2 = getIso2FromCode(code);
        if (iso2 == null || iso2.isEmpty()) {
            flagBitmapLive.postValue(null);
            return;
        }
        String flagUrl = "https://flagcdn.com/w80/"+ iso2 + ".png";

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(flagUrl);
                    URLConnection conn = url.openConnection();
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    is.close();
                    flagBitmapLive.postValue(bitmap);
                }catch (Exception e) {
                    Log.e("FlagLoad","Error loading the Flag" + e);
                    flagBitmapLive.postValue(null);
                }
            }
        }).start();
    }
    public void performSearch(String search) {
        String searchTerm = search == null ? "": search.trim().toLowerCase();
        ArrayList<CurrencyItem> items = currencyItemsLive.getValue();
        if (items == null) {items = new ArrayList<>();}

        ArrayList<String> displayList = new ArrayList<>();
        ArrayList<CurrencyItem> newFilteredItems = new ArrayList<>();

        if (searchTerm.isEmpty()){

            for (CurrencyItem item: items){
                displayList.add("Exchange rate for " + item.getTitle()
                        + ":\n" + item.getDescription()
                        + "\nUpdated Data On: " + item.getPubDate());
                newFilteredItems.add(item);
            }
        }else {
            for (CurrencyItem item: items){
                String code = getCodeFromTitle(item.getTitle());
                String lowCode =  code.toLowerCase();

                String country = getCountryFromCode(code);
                String lowCountry = country.toLowerCase();

                if (item.getTitle().toLowerCase().contains(searchTerm)
                        || item.getDescription().toLowerCase().contains(searchTerm)
                        || lowCountry.contains(searchTerm)
                        || lowCode.contains(searchTerm)){
                    displayList.add("Exchange rate for " + item.getTitle()
                            + ":\n" + item.getDescription()
                            + "\nUpdated Data On: " + item.getPubDate());
                    newFilteredItems.add(item);
                }
            }
        }
        filteredCurrencyItems = newFilteredItems;
        currencyDisplayListLive.postValue(displayList);
        if(displayList.isEmpty()){
            rawDataDisplayLive.postValue("No Results Found For: " + searchTerm);
        } else {
            rawDataDisplayLive.postValue("Showing Result For: " + searchTerm);
        }
    }
    private String getCodeFromTitle(String title) {
        if (title == null) return "";
        int lastOpenB = title.lastIndexOf('(');
        int lastCloseB = title.lastIndexOf(')');
        if (lastOpenB != -1 && lastCloseB != -1 && lastCloseB > lastOpenB){
            return title.substring(lastOpenB + 1, lastCloseB).trim();
        }
        return"";
    }

    private String getCountryFromCode(String code) {
        if (code == null) return "";
        String country = countryMap.get(code.toUpperCase());
        return country != null ? country : "";
    }
    private String getIso2FromCode(String code){
        if (code == null) return "";
        String iso2 = iso2Map.get(code.toUpperCase());
        return iso2 != null ? iso2 : "";
    }


    public double getRate(String description) {
        try{
            String[] parts = description.split("=");
            if (parts.length > 1) {
                String rateString = parts[1].trim().split(" ")[0];
                return Double.parseDouble(rateString);
            }
        } catch (Exception e) {}
        return -1.0;
    }
    public int getStrengthBand(String description){
        double rate = getRate(description);
        if (rate <= 0.5) return 1;
        else if (rate <= 1.0) return 2;
        else if (rate <= 2.0) return 3;
        else return 4;
    }
}

