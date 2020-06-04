package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.istanbul.logic.Place;
import lombok.Getter;

import java.util.List;

@Getter
public class PostOfficeView extends PlaceView {

    private final List<Boolean> indicators;

    PostOfficeView(Place.PostOffice postOffice) {
        super(postOffice);

        this.indicators = postOffice.getIndicators();
    }
}
