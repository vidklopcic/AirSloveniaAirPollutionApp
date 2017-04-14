package com.vidklopcic.airsense.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vidklopcic.airsense.R;
import com.vidklopcic.airsense.data.Constants;
import com.vidklopcic.airsense.data.entities.MeasuringStation;
import com.vidklopcic.airsense.util.AQI;
import com.vidklopcic.airsense.util.Conversion;
import com.vidklopcic.airsense.util.anim.AqiBarAnimation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OverviewBarChart extends Fragment implements PullUpBase {
    public static final String LAYOUT_ARG = "layout_arg";
    private Integer mHeight;

    public interface OnFragmentLoadedListener {
        void onLoaded();
    }
    LinearLayout mAQILabelsContainer;
    private RelativeLayout mAqiChartContainer;
    private LinearLayout mAQIBarsContainer;
    private HashMap<String, ArrayList<LinearLayout>> mAQIBars;
    private HashMap<String, Integer> mAQIBarsVals;
    private Context mContext;
    private LayoutInflater mInflater;
    private int mChartRange = Constants.AQI.BAR_OFFSET;
    private Integer mAQIBarsContainerHeight;
    private List<MeasuringStation> mStations;
    private OnFragmentLoadedListener mOnLoadedListener;

    public OverviewBarChart() {
        // Required empty public constructor
    }

    public void setOnLoadedListener(OnFragmentLoadedListener listener) {
        mOnLoadedListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAQIBars = new HashMap<>();
        mAQIBarsVals = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view;
        Bundle args = getArguments();
        if (args != null && args.getInt(LAYOUT_ARG, -1) != -1) {
            view = inflater.inflate(args.getInt(LAYOUT_ARG), container, false);
        } else {
            view = inflater.inflate(R.layout.fragment_aqi_bar_chart, container, false);
        }
        if (mHeight != null) {
            mAQIBarsContainerHeight = null;
        }
        mAqiChartContainer = (RelativeLayout) view.findViewById(R.id.aqi_overview_chart_container);
        mAQIBarsContainer = (LinearLayout) view.findViewById(R.id.aqi_chart_bars_layout);
        mAQILabelsContainer = (LinearLayout) view.findViewById(R.id.aqi_overview_x_labels_container);
        mContext = view.getContext();
        mInflater = inflater;

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mAQIBarsContainer.post(new Runnable() {
                    public void run() {
                        if (mAQIBarsContainerHeight == null) {
                            if (mStations != null) {
                                update(mStations);
                            }
                            if (mHeight == null) {
                                mAQIBarsContainerHeight = mAQIBarsContainer.getHeight();
                            } else {
                                mAQIBarsContainerHeight = mHeight - (int)getResources().getDimension(R.dimen.pollutant_label_bar_height)*2;
                                setHeight(mHeight);
                            }
                            setChartRange((float) (mChartRange-Constants.AQI.BAR_OFFSET));
                        }
                    }
                });
            }
        });
        if (mOnLoadedListener != null) mOnLoadedListener.onLoaded();
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void setHeight(int height) {
        mHeight = height;
        View view = getView();
        if (view != null) {
            view.getLayoutParams().height = height;
        }
    }

    public void addBar(Integer aqi, String label_text) {
        if (aqi+Constants.AQI.BAR_OFFSET > mChartRange) {
            setChartRange(aqi.floatValue());
        }
        ArrayList<LinearLayout> bar_label = new ArrayList<>();
        LinearLayout bar = createBar(0);
        LinearLayout label = createXLabel(label_text, 0);
        mAQIBarsContainer.addView(bar);
        mAQILabelsContainer.addView(label);
        bar.startAnimation(
                new AqiBarAnimation(
                        this, label, bar.findViewById(R.id.aqi_bar_content), getAqiFromLabel(label), aqi));
        bar_label.add(bar);
        bar_label.add(label);
        mAQIBars.put(label_text, bar_label);
        mAQIBarsVals.put(label_text, aqi);
    }

    public void removeBar(String key) {
        mAQIBarsContainer.removeView(mAQIBars.get(key).get(0));
        mAQILabelsContainer.removeView(mAQIBars.get(key).get(1));
        mAQIBars.remove(key);
    }

    public void setBarAqi(String key, Integer aqi) {
        if (mAQIBarsVals.get(key).equals(aqi))
            return;
        mAQIBarsVals.put(key, aqi);
        LinearLayout label = mAQIBars.get(key).get(1);
        LinearLayout bar = mAQIBars.get(key).get(0);
        View bar_content = bar.findViewById(R.id.aqi_bar_content);
        bar.startAnimation(new AqiBarAnimation(this, label, bar_content, getAqiFromLabel(label), aqi));
    }

    private LinearLayout createXLabel(String name, Integer aqi) {
        LinearLayout label = (LinearLayout) mInflater.inflate(R.layout.fragment_aqi_bar_chart_label_layout, mAQILabelsContainer, false);
        TextView label_text = (TextView) label.findViewById(R.id.aqi_text);
        label_text.setText(name);
        TextView label_subtitle = (TextView) label.findViewById(R.id.aqi_subtitle);
        label_subtitle.setText(aqi.toString());
        return label;
    }

    private LinearLayout createBar(int aqi) {
        LinearLayout bar = (LinearLayout) mInflater.inflate(R.layout.fragment_aqi_bar_chart_bar_layout, mAQIBarsContainer, false);
        View bar_content = bar.findViewById(R.id.aqi_bar_content);
        setBarAqi(bar_content, (float) aqi);
        return bar;
    }

    public void setBarAqi(View bar, Float aqi) {
        Float percentage = aqi / Constants.AQI.SUM;
        bar.setBackgroundColor(AQI.getLinearColor(aqi.intValue(), mContext));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bar.getLayoutParams();
        params.weight = percentage;
        bar.setLayoutParams(params);
        if (aqi+Constants.AQI.BAR_OFFSET > mChartRange) {
            setChartRange(aqi);
        }
    }

    public void setLabelAqi(LinearLayout label, Integer aqi) {
        int old_aqi = getAqiFromLabel(label);
        if (getMaxAqi() ==  old_aqi && old_aqi > aqi) {
            setChartRange(Float.valueOf(aqi));
        }
        TextView subtitle = (TextView)label.findViewById(R.id.aqi_subtitle);
        subtitle.setText(aqi.toString());
        if (getContext() != null) {
            label.setBackgroundColor((AQI.getLinearColor(aqi, getContext()) & 0x00FFFFFF) | 0xDD000000);
        }
    }

    private void setChartRange(Float max_aqi) {
        mChartRange = (int) (max_aqi+Constants.AQI.BAR_OFFSET);
        if (mAQIBarsContainerHeight == null) return;
        Float height_increase = Constants.AQI.SUM / (max_aqi+Constants.AQI.BAR_OFFSET);    // max_axi+x.. x = margin
        Integer top_margin = (int) -(height_increase * mAQIBarsContainerHeight - mAQIBarsContainerHeight);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mAqiChartContainer.getLayoutParams();
        params.setMargins(0, top_margin, 0, 0);
        mAqiChartContainer.setLayoutParams(params);
    }

    private int getAqiFromLabel(LinearLayout label) {
        return Integer.valueOf((String) ((TextView) label.findViewById(R.id.aqi_subtitle)).getText());
    }

    private int getMaxAqi() {
        int max_aqi = 0;
        for(ArrayList<LinearLayout> l: mAQIBars.values()) {
            int aqi = getAqiFromLabel(l.get(1));
            if (aqi > max_aqi) max_aqi = aqi;
        }
        return max_aqi;
    }

    public ArrayList<HashMap<String, Integer>> update(List<MeasuringStation> stations) {
        if (mAQIBars == null) return null;
        ArrayList<HashMap<String, Integer>> averages = MeasuringStation.getAverages(stations);
        mStations = stations;
        if (averages == null || averages.get(MeasuringStation.AVERAGES_POLLUTANTS).size() == 0) return null;
        List<String> bar_pollutants = new ArrayList<>(mAQIBars.keySet());
        for (String parameter : bar_pollutants)
            if (!averages.get(MeasuringStation.AVERAGES_POLLUTANTS).containsKey(parameter)) removeBar(parameter);
        HashMap<String, Integer> aqi_averages = averages.get(MeasuringStation.AVERAGES_POLLUTANTS);
        for (String pollutant_name : aqi_averages.keySet()) {
            if (mAQIBars.containsKey(pollutant_name)) setBarAqi(pollutant_name, aqi_averages.get(pollutant_name));
            else addBar(aqi_averages.get(pollutant_name), pollutant_name);
        }
        return averages;
    }
}
