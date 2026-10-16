package com.arbergashi.charts.demo;

import com.arbergashi.charts.model.DefaultFinancialChartModel;

/** Keeps one live OHLC bar inside the visible demo window without growing its history. */
final class ShowcaseFinancialModel extends DefaultFinancialChartModel {
    private long liveUpdates;

    ShowcaseFinancialModel(DefaultFinancialChartModel source) {
        super(source.getName());
        for (int index = 0; index < source.getPointCount(); index++) {
            setOHLC(source.getX(index), source.getOpen(index), source.getHigh(index), source.getLow(index),
                    source.getClose(index), source.getVolume(index), source.getLabel(index));
            getProvenanceFlagsData()[index] = source.getProvenanceFlag(index);
            getSourceIdsData()[index] = source.getSourceId(index);
            getTimestampNanosData()[index] = source.getTimestampNanos(index);
        }
    }

    void updateLiveBar(double open, double high, double low, double close, double volume) {
        int index = getPointCount() - 1;
        if (index < 0) return;
        getOpenData()[index] = open;
        getHighData()[index] = high;
        getLowData()[index] = low;
        getCloseData()[index] = close;
        getVolumeData()[index] = volume;
        liveUpdates++;
        fireModelChanged();
    }

    @Override
    public long getUpdateStamp() {
        return super.getUpdateStamp() + liveUpdates;
    }
}
