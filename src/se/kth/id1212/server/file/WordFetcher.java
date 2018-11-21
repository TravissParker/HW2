package se.kth.id1212.server.file;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;

/**
 * Gets and delivers word to the game.
 * */
public class WordFetcher implements Runnable {
    //Fixme: set relative path to project root
    private final String pathString = "C:\\Users\\mikae\\OneDrive\\IdeaProjects\\HW1\\words.txt";
    private final Path path = Paths.get(pathString);
    private List<String> library = new ArrayList<>();
    private boolean libraryReady = false;

    public WordFetcher() {
        new Thread(this).start();
    }

    public String supplyWord() {
        return drawWord();
   }

   private String drawWord() {
       int index = new Random().nextInt(library.size());
       return library.get(index);
   }

    @Override
    public void run() {
        //Setup the library
        try {
            library = Files.readAllLines(path);
            libraryReady = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isLibraryReady() {
        return libraryReady;
    }
}
