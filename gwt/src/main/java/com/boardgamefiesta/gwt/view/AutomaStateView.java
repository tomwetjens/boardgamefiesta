package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.Garth;
import com.boardgamefiesta.gwt.logic.Worker;
import lombok.Value;

@Value
public class AutomaStateView {

    Worker specialization;

    AutomaStateView(Garth automaState) {
        specialization = automaState.getSpecialization();
    }

}
