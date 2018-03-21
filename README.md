# RubberBandView

A custom view that simulates the behaviour of a rubber band for Android. More details [here](https://engineering.scalable.capital/2018/02/25/rubberbandview-a-custom-view.html).

![](screenshots/rubber_loading.gif)

## Usage

### Minimum setup

In your layout file:

```
<capital.scalable.droid.rubberbandview.RubberBandView
    android:id="@+id/rubberBandView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### Max selectable value

The API of the custom view is similar to that of Android's [ProgressBar](https://developer.android.com/reference/android/widget/ProgressBar.html). One must set the max selectable value, using:

```
RubberBandView rubberbandView = findViewById(R.id.rubberBandView);
rubberBandView.setMaxSelection(10);
```

This way, the user can select any value from the range [0,10].

### RubberBandListener

To receive updates to the selected value:

```
rubberBandView.setListener(new RubberBandListener() {
    @Override
    public void onSelectionChanged(int value) {
        Toast.makeText(MainActivity.this, "Selection changed to " + value, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSelectionFinished(int value) {
        Toast.makeText(MainActivity.this, "Selection ended with value " + value, Toast.LENGTH_SHORT).show();
    }
});
```

The `onSelectionFinished` event is triggered as soon as the user has lifted their finger off the view. Note that the value returned in that case is the final selected one.

### Vibration animation

The view comes with a nudge animation, that encourages the user to interact with it.

![](screenshots/rubber_vibration.gif)

The default animation uses a [ValueAnimator](https://developer.android.com/reference/android/animation/ValueAnimator.html), which looks like this:

```
vibrationAnimator = ValueAnimator.ofFloat(-1f, 1f, -0.5f, 0.5f, 0);
vibrationAnimator.setInterpolator(new DecelerateInterpolator());
vibrationAnimator.setRepeatCount(3);
vibrationAnimator.setDuration(200 / 3);
vibrationAnimator.setStartDelay(5000);
```

You can supply your own animator, using:

`rubberBandView.setVibrationAnimator(customAnimator)`

To turn the animation off completely, simply set the vibration animator to `null`.

### UI Customization

These are the parameters that can be modified through xml:

```
<capital.scalable.droid.rubberbandview.RubberBandView
    app:loosenessRatio="0.2"
    app:rubberColor="@color/colorAccent"
    app:minRubberWidth="4dp"
    app:maxRubberWidth="5dp"
    app:vibrationPeakAmplitude="3dp"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

- `loosenessRatio`: how loose the rubber needs to be. Must be in [0,1[ range. Defaults to 0.2
- `rubberColor`: the color of the rubber. Defaults to the theme's `colorAccent`
- `minRubberWidth`: the minimum width of the rubber band, in dp. This will be the width when the rubber band is pulled to its max.
- `maxRubberWidth`: the maximum width of the rubber band, in dp. This will be the width when the rubber band is at rest.
- `vibrationPeakAmplitude`: the maximum displacement of the rubber band when vibrating, in dp.


## Installation

``` groovy
repositories {
    maven { url "https://jitpack.io" }
}


dependencies {
    implementation "com.github.ScaCap:RubberBandView:1.0.0"
}
```

## License

RubberBandView is Open Source software released under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).
