package com.citisense.vidklopcic.citisense.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.util.AQI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class PollutantsAqiCardsFragment extends Fragment {
    HashMap<String, LinearLayout> mPollutantCards;
    ArrayList<CitiSenseStation> mStations;
    Context mContext;
    LayoutInflater mInflater;
    TextView mTemperatureText;
    TextView mAqiText;
    TextView mHumidityText;
    LinearLayout mPollutantsContainer;
    View mLayout;
    String mNoData;
    public PollutantsAqiCardsFragment() {
        mPollutantCards = new HashMap<>();
        mStations = new ArrayList<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pollutant_cards, container, false);
        mContext = view.getContext();
        mInflater = inflater;
        mTemperatureText = (TextView) view.findViewById(R.id.card_pollutants_title_temperature);
        mAqiText = (TextView) view.findViewById(R.id.card_polltants_title_aqi);
        mHumidityText = (TextView) view.findViewById(R.id.card_polltants_title_humidity);
        mPollutantsContainer = (LinearLayout) view.findViewById(R.id.pollutants_container);
        mNoData = getString(R.string.no_data);
        mLayout = view;
        return view;
    }


    public void hide() {
        mLayout.animate().alpha(0f).setDuration(200);
    }

    public void show() {
        mLayout.animate().alpha(1f).setDuration(200);
    }

    public void setSourceStations(ArrayList<CitiSenseStation> stations) {
        clearPollutants();
        if (stations == null) return;
        mStations = stations;
        refresh();
    }

    public void setSourceStations(CitiSenseStation station) {
        clearPollutants();
        if (station == null) return;
        mStations.clear();
        mStations.add(station);
        refresh();
    }

    public void setNoData() {
        mAqiText.setText(mNoData);
        mAqiText.setBackgroundColor(ContextCompat.getColor(mContext, R.color.dark_gray));
        setNoHumidity();
        setNoTemperature();
    }

    public void setNoHumidity() {
        mHumidityText.setText("/");
    }

    public void setNoTemperature() {
        mTemperatureText.setText("/");
    }

    public void refresh() {
        ArrayList<HashMap<String, Integer>> averages = CitiSenseStation.getAverages(mStations);
        if (averages == null) {
            setNoData();
            return;
        }
        int max_aqi = Collections.max(averages.get(CitiSenseStation.AVERAGES_POLLUTANTS).values());
        mAqiText.setText(AQI.toText(max_aqi));
        mAqiText.setBackgroundColor(ContextCompat.getColor(mContext, AQI.getColor(max_aqi)));

        HashMap<String, Integer> other = averages.get(CitiSenseStation.AVERAGES_OTHER);
        HashMap<String, Integer> pollutants = averages.get(CitiSenseStation.AVERAGES_POLLUTANTS);
        if (other.keySet().contains(Constants.CitiSenseStation.TEMPERATURE_KEY))
            mTemperatureText.setText(other.get(Constants.CitiSenseStation.TEMPERATURE_KEY).toString() + Constants.TEMPERATURE_UNIT);
        else
            setNoTemperature();
        if (other.keySet().contains(Constants.CitiSenseStation.HUMIDITY_KEY))
            mHumidityText.setText(other.get(Constants.CitiSenseStation.HUMIDITY_KEY).toString() + Constants.HUMIDITY_UNIT);
        else
            setNoHumidity();
        for (String pollutant : pollutants.keySet()) {
            addPollutant(pollutant, pollutants.get(pollutant));
        }
    }

    public void addPollutant(String name, Integer aqi) {
        if (mPollutantCards.keySet().contains(name)) {
            setCard(mPollutantCards.get(name), aqi);
            return;
        }
        LinearLayout pollutant_card = (LinearLayout) mInflater.inflate(R.layout.fragment_aqi_cards_pollutant_layout, mPollutantsContainer, false);
        ((TextView) pollutant_card.findViewById(R.id.pollutant_name)).setText(name);
        setCard(pollutant_card, aqi);
        mPollutantCards.put(name, pollutant_card);
        mPollutantsContainer.addView(pollutant_card);
    }

    public void clearPollutants() {
        for (String key : mPollutantCards.keySet()) {
            removePollutant(key);
        }
        mPollutantCards.clear();
    }
    public void removePollutant(String name) {
        mPollutantsContainer.removeView(mPollutantCards.get(name));
    }

    public void setCard(LinearLayout card, Integer aqi) {
        ((TextView) card.findViewById(R.id.pollutant_aqi)).setText(aqi.toString());
        ((TextView) card.findViewById(R.id.pollutant_name)).setTextColor(
                AQI.getLinearColor(aqi, mContext)
        );
    }
}
