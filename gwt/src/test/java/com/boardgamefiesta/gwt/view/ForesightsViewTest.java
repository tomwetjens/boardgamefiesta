package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.Foresights;
import com.boardgamefiesta.gwt.logic.KansasCitySupply;
import com.boardgamefiesta.gwt.logic.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForesightsViewTest {

    @Mock
    Foresights foresights;

    @Test
    void oneNullInColumn() {
        when(foresights.choices(0)).thenReturn(Arrays.asList(null, new KansasCitySupply.Tile(Worker.COWBOY)));
        when(foresights.choices(1)).thenReturn(Arrays.asList(null, new KansasCitySupply.Tile(Worker.COWBOY)));
        when(foresights.choices(2)).thenReturn(Arrays.asList(null, new KansasCitySupply.Tile(Worker.COWBOY)));

        var view = new ForesightsView(foresights);

        assertThat(view.getChoices().get(0).get(0)).isNull();
        assertThat(view.getChoices().get(0).get(1).getWorker()).isEqualTo(Worker.COWBOY);
    }
}