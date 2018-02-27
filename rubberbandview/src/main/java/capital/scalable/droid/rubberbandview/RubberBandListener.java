package capital.scalable.droid.rubberbandview;

public interface RubberBandListener {
    /**
     * Called every time the selected value changes
     * @param value the selected value
     */
    void onSelectionChanged(int value);

    /**
     * Called when the user stops changing the selected value
     * @param value the selected value
     */
    void onSelectionFinished(int value);
}
