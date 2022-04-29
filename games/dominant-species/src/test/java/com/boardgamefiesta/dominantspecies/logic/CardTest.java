package com.boardgamefiesta.dominantspecies.logic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardTest {

    @Mock
    DominantSpecies game;

    @Test
    void intelligence() {
        var mammals = mock(Animal.class);
        var reptiles = mock(Animal.class);
        var birds = mock(Animal.class);
        var amphibians = mock(Animal.class);
        var arachnids = mock(Animal.class);
        var insects = mock(Animal.class);

        when(game.getAnimals()).thenReturn(Map.of(
                AnimalType.MAMMALS, mammals,
                AnimalType.REPTILES, reptiles,
                AnimalType.BIRDS, birds,
                AnimalType.AMPHIBIANS, amphibians,
                AnimalType.ARACHNIDS, arachnids,
                AnimalType.INSECTS, insects
        ));

        when(game.getCurrentAnimal()).thenReturn(AnimalType.BIRDS);

        var actionResult = Card.INTELLIGENCE.perform(game, new Random(0));

        assertThat(actionResult.getFollowUpActions().isEmpty()).isTrue();
        assertThat(actionResult.isCanUndo()).isTrue();

        verify(mammals).addActionPawn();
        verify(reptiles).addActionPawn();
        verify(birds).addActionPawn();
        verifyNoInteractions(amphibians);
        verifyNoInteractions(arachnids);
        verifyNoInteractions(insects);
    }

    @Test
    void parasitism() {
        var mammals = mock(Animal.class);
        var reptiles = mock(Animal.class);
        var birds = mock(Animal.class);
        var amphibians = mock(Animal.class);
        var arachnids = mock(Animal.class);
        var insects = mock(Animal.class);

        when(game.getAnimals()).thenReturn(Map.of(
                AnimalType.MAMMALS, mammals,
                AnimalType.REPTILES, reptiles,
                AnimalType.BIRDS, birds,
                AnimalType.AMPHIBIANS, amphibians,
                AnimalType.ARACHNIDS, arachnids,
                AnimalType.INSECTS, insects
        ));

        when(game.getCurrentAnimal()).thenReturn(AnimalType.BIRDS);

        var actionResult = Card.PARASITISM.perform(game, new Random(0));

        assertThat(actionResult.getFollowUpActions().isEmpty()).isTrue();
        assertThat(actionResult.isCanUndo()).isTrue();

        verifyNoInteractions(mammals);
        verifyNoInteractions(reptiles);
        verify(birds).addActionPawn();
        verify(amphibians).addActionPawn();
        verify(arachnids).addActionPawn();
        verify(insects).addActionPawn();
    }
}