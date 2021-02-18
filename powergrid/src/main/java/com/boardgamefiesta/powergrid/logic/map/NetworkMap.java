package com.boardgamefiesta.powergrid.logic.map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface NetworkMap {

    NetworkMap GERMANY = NetworkMap.load(NetworkMap.class.getResourceAsStream("/germany.yaml"));

    String getName();

    Set<? extends Area> getAreas();

    Set<? extends City> getCities();

    Stream<? extends Connection> getConnections(City from);

    private static NetworkMap load(InputStream inputStream) {
        var loadingConfig = new LoaderOptions();
        loadingConfig.setMaxAliasesForCollections(1000);

        var yamlNetworkMap = new Yaml(loadingConfig).loadAs(inputStream, YamlNetworkMap.class);
        yamlNetworkMap.createInverseConnections();

        return yamlNetworkMap;
    }

    default Path shortestPath(City source, City target, Set<? extends Area> areas) {
        var unvisited = getCities().stream()
                .filter(city -> areas.contains(city.getArea()))
                .collect(Collectors.toSet());

        var dist = new HashMap<City, Integer>();
        var prev = new HashMap<City, Connection>();

        dist.put(source, 0);

        while (!unvisited.isEmpty()) {
            var current = unvisited.stream()
                    .min(Comparator.comparingInt(city -> dist.getOrDefault(city, Integer.MAX_VALUE)))
                    .orElseThrow();

            if (current == target) {
                // found
                var path = new LinkedList<Connection>();

                var from = current;
                while (from != source) {
                    var connection = prev.get(from);
                    path.addFirst(connection);
                    from = connection.getFrom();
                }

                return new Path(path);
            }

            unvisited.remove(current);

            getConnections(current)
                    .filter(connection -> areas.contains(connection.getTo().getArea()))
                    .forEach(connection -> {
                        var alt = dist.getOrDefault(current, Integer.MAX_VALUE) + connection.getCost();
                        if (alt < dist.getOrDefault(connection.getTo(), Integer.MAX_VALUE)) {
                            dist.put(connection.getTo(), alt);
                            prev.put(connection.getTo(), connection);
                        }
                    });
        }

        throw new IllegalStateException("no path from " + source + " to " + target);
    }

    default boolean isReachable(City source, City target) {
        return isReachable(source, target, new HashSet<>());
    }

    private boolean isReachable(City source, City target, Set<City> visited) {
        if (source == target) {
            return true;
        }

        visited.add(source);

        return getConnections(source).anyMatch(connection -> {
            if (!visited.contains(connection.getTo())) {
                return isReachable(connection.getTo(), target, visited);
            }
            return false;
        });
    }

    default City getCity(String name) {
        return getCities().stream().filter(city -> city.getName().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("city not found: " + name));
    }

    default Area getArea(String name) {
        return getAreas().stream().filter(area -> area.getName().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("area not found: " + name));
    }

    default void validateAreas(Set<Area> areas) {

    }

    default Set<Area> randomAreas(int n, Random random) {
        var result = new HashSet<Area>();

        var area = getAreas().stream().skip(random.nextInt(getAreas().size() - 1)).findFirst().orElseThrow();
        result.add(area);

        var possibleAreas = new HashSet<Area>();

        while (result.size() < n) {
            getAdjacentAreas(area).forEach(possibleAreas::add);

            possibleAreas.removeAll(result);

            area = possibleAreas.stream().skip(random.nextInt(possibleAreas.size() - 1)).findFirst().orElseThrow();
            result.add(area);
        }

        return result;
    }

    private Stream<Area> getAdjacentAreas(Area area) {
        return getCities().stream()
                .filter(city -> city.getArea() == area)
                .flatMap(city -> getConnections(city))
                .map(Connection::getTo)
                .map(City::getArea);
    }

    @Data
    class YamlNetworkMap implements NetworkMap {
        String name;
        Set<YamlArea> areas;
        Set<YamlCity> cities;
        Set<YamlConnection> connections;

        void createInverseConnections() {
            new HashSet<>(connections).forEach(connection -> connections.add(connection.inverse()));
        }

        @Override
        public Stream<? extends Connection> getConnections(City from) {
            return connections.stream().filter(connection -> connection.getFrom() == from);
        }
    }

    @Data
    class YamlArea implements Area {
        String name;

        @Override
        public String toString() {
            return name;
        }
    }

    @Data
    class YamlCity implements City {
        String name;
        YamlArea area;

        @Override
        public String toString() {
            return name;
        }
    }

    @Data
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor
    class YamlConnection implements Connection {
        YamlCity from;
        YamlCity to;
        int cost;

        YamlConnection inverse() {
            return new YamlConnection(to, from, cost);
        }

        @Override
        public String toString() {
            return from + "-(" + cost + ")->" + to;
        }
    }
}
