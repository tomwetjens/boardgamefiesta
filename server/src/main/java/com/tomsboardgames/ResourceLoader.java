package com.tomsboardgames;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceLoader {

    public static List<String> readLines(InputStream inputStream) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            return bufferedReader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
