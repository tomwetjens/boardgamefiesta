package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.NonNull;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.stream.Collectors;

public class ObjectiveCards {

    private final Queue<ObjectiveCard> drawStack;
    private final Set<ObjectiveCard> available;

    private ObjectiveCards(Queue<ObjectiveCard> drawStack, Set<ObjectiveCard> available) {
        this.drawStack = drawStack;
        this.available = available;
    }

    ObjectiveCards(Random random) {
        this(createDrawStack(random), new HashSet<>());
        fill();
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("drawStack", serializer.fromCollection(drawStack, ObjectiveCard::serialize))
                .add("available", serializer.fromCollection(available, ObjectiveCard::serialize))
                .build();
    }

    static ObjectiveCards deserialize(JsonObject jsonObject) {
        return new ObjectiveCards(
                jsonObject.getJsonArray("drawStack").stream()
                        .map(ObjectiveCard::deserialize)
                        .collect(Collectors.toCollection(LinkedList::new)),
                jsonObject.getJsonArray("available").stream()
                        .map(ObjectiveCard::deserialize)
                        .collect(Collectors.toSet()));
    }

    static List<ObjectiveCard> createStartingObjectiveCardsDrawStack(@NonNull Random random, int playerCount) {
        List<ObjectiveCard> deck = new ArrayList<>(ObjectiveCard.STARTING_CARDS);
        Collections.shuffle(deck, random);

        while (deck.size() > playerCount) {
            deck.remove(0);
        }

        return new LinkedList<>(deck);
    }

    void remove(ObjectiveCard objectiveCard) {
        if (!available.remove(objectiveCard)) {
            throw new GWTException(GWTError.OBJECTIVE_CARD_NOT_AVAILABLE);
        }

        fill();
    }

    public Set<ObjectiveCard> getAvailable() {
        return Collections.unmodifiableSet(available);
    }

    public int getDrawStackSize() {
        return drawStack.size();
    }

    private void fill() {
        while (available.size() < 4 && !drawStack.isEmpty()) {
            available.add(drawStack.poll());
        }
    }

    private static Queue<ObjectiveCard> createDrawStack(Random random) {
        List<ObjectiveCard> deck = Arrays.asList(
                new ObjectiveCard(ObjectiveCard.Type.GAIN2_4HH),
                new ObjectiveCard(ObjectiveCard.Type.GAIN2_BGBL),
                new ObjectiveCard(ObjectiveCard.Type.GAIN2_SSH),
                new ObjectiveCard(ObjectiveCard.Type.GAIN2_333B),
                new ObjectiveCard(ObjectiveCard.Type.GAIN2_BBLBL),
                new ObjectiveCard(ObjectiveCard.Type.AUX_SF),
                new ObjectiveCard(ObjectiveCard.Type.AUX_SF),
                new ObjectiveCard(ObjectiveCard.Type.AUX_SF),
                new ObjectiveCard(ObjectiveCard.Type.AUX_SF),
                new ObjectiveCard(ObjectiveCard.Type.DRAW_5H),
                new ObjectiveCard(ObjectiveCard.Type.DRAW_333S),
                new ObjectiveCard(ObjectiveCard.Type.DRAW_BBH),
                new ObjectiveCard(ObjectiveCard.Type.DRAW_SGBL),
                new ObjectiveCard(ObjectiveCard.Type.DRAW_SGG),
                new ObjectiveCard(ObjectiveCard.Type.ENGINE_44SG),
                new ObjectiveCard(ObjectiveCard.Type.ENGINE_345),
                new ObjectiveCard(ObjectiveCard.Type.ENGINE_BBGG),
                new ObjectiveCard(ObjectiveCard.Type.ENGINE_BBLHH),
                new ObjectiveCard(ObjectiveCard.Type.ENGINE_SSHH),
                new ObjectiveCard(ObjectiveCard.Type.MOVE_34HH),
                new ObjectiveCard(ObjectiveCard.Type.MOVE_345),
                new ObjectiveCard(ObjectiveCard.Type.MOVE_BBHH),
                new ObjectiveCard(ObjectiveCard.Type.MOVE_SSBB),
                new ObjectiveCard(ObjectiveCard.Type.MOVE_SSBLBL)
        );
        Collections.shuffle(deck, random);
        return new LinkedList<>(deck);
    }

    public boolean isEmpty() {
        return available.isEmpty();
    }

    public ObjectiveCard draw() {
        if (drawStack.isEmpty()) {
            throw new GWTException(GWTError.OBJECTIVE_CARD_NOT_AVAILABLE);
        }
        return drawStack.poll();
    }

}
