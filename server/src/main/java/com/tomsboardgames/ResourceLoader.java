package com.tomsboardgames;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ResourceLoader {

    public static List<String> readLines(URL resource) {
        try {
            return Files.readAllLines(Paths.get(resource.toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Could not read resource: " + resource);
        }
    }

}
