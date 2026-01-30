package com.arbergashi.charts.api;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

import com.arbergashi.charts.core.geometry.TextAnchor;
/**
 * Axis configuration.
 * <p>
 * Provides formatting and behavior flags for axis rendering.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class AxisConfig {

    private boolean showGrid = true;
    private int requestedTickCount = 6;
    private String labelFormatPattern = null;
    private String unitSuffix = ""; /* For units like %, CHF, m/s */
    private boolean autoScale = true;
    private boolean inverted = false;
    private Locale locale = Locale.getDefault();
    private Double fixedMin = null;
    private Double fixedMax = null;
    private Double unitsPerPixel = null;
    private Double medicalMmPerUnit = null;
    private TextAnchor labelAnchor = null;

    private transient NumberFormat cachedNumberFormat;
    private transient DecimalFormat cachedDecimalFormat;
    private transient String lastPattern;
    private transient Locale lastLocale;

    public AxisConfig() {
        /* Default constructor */
    }

    /**
     * Formats an axis value based on the selected pattern and locale.
     *
     * @param value The value to format.
     * @return The formatted value as a string.
     */
    public String getFormattedValue(double value) {
        if (labelFormatPattern != null && !labelFormatPattern.isEmpty()) {
            DecimalFormat df = getDecimalFormat();
            return df.format(value) + (unitSuffix.isEmpty() ? "" : " " + unitSuffix);
        }

        NumberFormat nf = getNumberFormat();
        return nf.format(value) + (unitSuffix.isEmpty() ? "" : " " + unitSuffix);
    }

    /**
     * Retrieves a cached or newly created DecimalFormat instance configured with the current locale
     * and label format pattern. If the configuration changes (locale or pattern), the cached instance
     * is invalidated and a new one is created.
     *
     * @return a DecimalFormat instance configured with the current locale and label format pattern.
     */
    private DecimalFormat getDecimalFormat() {
        if (cachedDecimalFormat == null || !Objects.equals(labelFormatPattern, lastPattern) || !Objects.equals(locale, lastLocale)) {
            cachedDecimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(locale);
            cachedDecimalFormat.applyPattern(labelFormatPattern);
            lastPattern = labelFormatPattern;
            lastLocale = locale;
        }
        return cachedDecimalFormat;
    }

    /**
     * Retrieves a cached or newly created NumberFormat instance configured with the current locale.
     * If the configuration changes (locale), the cached instance is invalidated and a new one is created.
     *
     * @return a NumberFormat instance configured with the current locale.
     */
    private NumberFormat getNumberFormat() {
        if (cachedNumberFormat == null || !Objects.equals(locale, lastLocale)) {
            cachedNumberFormat = NumberFormat.getNumberInstance(locale);
            cachedNumberFormat.setMaximumFractionDigits(2);
            lastLocale = locale;
            lastPattern = null;
        }
        return cachedNumberFormat;
    }

    /**
     * Determines whether the grid is displayed on the axis.
     *
     * @return true if the grid is enabled and should be displayed; false otherwise.
     */
    public boolean isShowGrid() {
        return showGrid;
    }

    /**
     * Enables or disables grid rendering for this axis.
     *
     * @param showGrid true to show the grid
     */
    public AxisConfig setShowGrid(boolean showGrid){
        this.showGrid = showGrid;
        return this;
        
    }

    /**
     * Retrieves the requested tick count for the axis.
     *
     * @return the requested tick count, or 0 if not set.
     */
    public int getRequestedTickCount() {
        return requestedTickCount;
    }

    /**
     * Sets the requested tick count for this axis.
     *
     * @param count desired tick count
     */
    public AxisConfig setRequestedTickCount(int count) {
        this.requestedTickCount = count;
        return this;
    }

    /**
     * Alias for {@link #setRequestedTickCount(int)}.
     *
     * @param count desired tick count
     */
    public AxisConfig setTicks(int count) {
        setRequestedTickCount(count);
        return this;
    }

    /**
     * Retrieves the label format pattern for the axis.
     *
     * @return the label format pattern, or null if not set.
     */
    public String getLabelFormatPattern() {
        return labelFormatPattern;
    }

    /**
     * Sets the label format pattern for axis values.
     *
     * @param pattern DecimalFormat pattern (null clears the pattern)
     */
    public AxisConfig setLabelFormatPattern(String pattern){
        this.labelFormatPattern = pattern;
        return this;
        
    }

    /**
     * Retrieves the unit suffix for the axis.
     *
     * @return the unit suffix, or an empty string if not set.
     */
    public String getUnitSuffix() {
        return unitSuffix;
    }

    /**
     * Sets the unit suffix appended to formatted values.
     *
     * @param unitSuffix unit suffix (null becomes empty)
     */
    public AxisConfig setUnitSuffix(String unitSuffix){
        this.unitSuffix = Objects.requireNonNullElse(unitSuffix, "");
        return this;
        
    }

    /**
     * Returns the preferred label anchor for axis tick labels, or null if unset.
     */
    public TextAnchor getLabelAnchor() {
        return labelAnchor;
    }

    /**
     * Sets the preferred label anchor for axis tick labels.
     */
    public AxisConfig setLabelAnchor(TextAnchor labelAnchor) {
        this.labelAnchor = labelAnchor;
        return this;
    }

    /**
     * Returns the preferred label anchor or a provided default if unset.
     */
    public TextAnchor getLabelAnchorOrDefault(TextAnchor fallback) {
        return labelAnchor != null ? labelAnchor : fallback;
    }

    /**
     * Determines whether the axis should automatically adjust its scale based on data.
     *
     * @return true if auto-scaling is enabled; false otherwise.
     */
    public boolean isAutoScale() {
        return autoScale;
    }

    /**
     * Enables or disables auto-scaling for this axis.
     *
     * @param autoScale true to enable auto-scaling
     */
    public AxisConfig setAutoScale(boolean autoScale){
        this.autoScale = autoScale;
        return this;
        
    }

    /**
     * Sets a fixed data range for the axis.
     *
     * @param min fixed minimum value
     * @param max fixed maximum value
     */
    public AxisConfig setFixedRange(double min, double max){
        this.fixedMin = min;
        this.fixedMax = max;
        return this;
        
    }

    /**
     * Returns whether a fixed range is configured.
     */
    public boolean isFixedRange() {
        return fixedMin != null && fixedMax != null;
    }

    /**
     * Returns the fixed minimum value if configured.
     */
    public Double getFixedMin() {
        return fixedMin;
    }

    /**
     * Returns the fixed maximum value if configured.
     */
    public Double getFixedMax() {
        return fixedMax;
    }

    /**
     * Sets the data units per pixel for a fixed physical scale (e.g., mm/s).
     * A null value disables fixed scale behavior.
     *
     * <p>See {@link com.arbergashi.charts.util.ChartScale} for the physical scaling
     * contract used to keep millimeter measurements stable on screen.</p>
     *
     * @param unitsPerPixel data units per pixel
     */
    public AxisConfig setUnitsPerPixel(Double unitsPerPixel){
        this.unitsPerPixel = unitsPerPixel;
        return this;
        
    }

    /**
     * Returns the configured units per pixel, or null if not set.
     */
    public Double getUnitsPerPixel() {
        return unitsPerPixel;
    }

    /**
     * Returns the medical millimeter-per-unit scale if configured.
     */
    public Double getMedicalMmPerUnit() {
        return medicalMmPerUnit;
    }

    /**
     * Resolves the effective units-per-pixel value using the provided physical density.
     *
     * <p>If a medical scale is configured, it takes precedence and is converted to
     * units-per-pixel based on the physical pixels-per-millimeter value.</p>
     */
    public Double getResolvedUnitsPerPixel(double pixelsPerMillimeter) {
        if (medicalMmPerUnit != null
                && Double.isFinite(medicalMmPerUnit)
                && medicalMmPerUnit > 0.0
                && Double.isFinite(pixelsPerMillimeter)
                && pixelsPerMillimeter > 0.0) {
            double unitsPerMm = 1.0 / medicalMmPerUnit;
            return unitsPerMm / pixelsPerMillimeter;
        }
        return unitsPerPixel;
    }

    /**
     * Convenience for medical/physical scaling. This sets units-per-pixel using
     * a mm-per-unit ratio (e.g., 25 mm/s or 10 mm/mV).
     *
     * @param mmPerUnit millimeters per data unit
     */
    public AxisConfig setMedicalScale(double mmPerUnit) {
        if (!Double.isFinite(mmPerUnit) || mmPerUnit <= 0.0) {
            this.medicalMmPerUnit = null;
            this.unitsPerPixel = null;
        } else {
            this.medicalMmPerUnit = mmPerUnit;
            this.unitsPerPixel = 1.0 / mmPerUnit;
        }
        return this;
    }

    /**
     * Sets whether this axis is inverted.
     *
     * @param inverted true to invert the axis direction
     */
    public AxisConfig setInverted(boolean inverted) {
        this.inverted = inverted;
        return this;
    }

    /**
     * Returns whether this axis is inverted.
     */
    public boolean isInverted() {
        return inverted;
    }

    /**
     * Sets the locale for the axis.
     *
     * @param locale the locale to use for formatting
     */
    public AxisConfig setLocale(Locale locale) {
        this.locale = Objects.requireNonNull(locale);
        return this;
    }

    /**
     * Returns the locale used for formatting.
     *
     * @return locale for number formatting
     */
    public Locale getLocale() {
        return locale;
    }
}
