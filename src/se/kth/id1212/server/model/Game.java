package se.kth.id1212.server.model;

import se.kth.id1212.server.file.WordFetcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Game {
    private char[] letters;
    private List<Character> guessedLetters;
    private boolean[] guessed;
    private int attemptsLeft;
    private int noLetters;
    private WordFetcher wf = new WordFetcher();
    private final String UNKNOWN_LETTER = "_";
    private final String rules =
            "Guess a letter in the places of the word where it says _, " +
                    "if the guess is correct then _ will be replaced by the " +
                    "correct guess." +
                    "\nYou may also guess the entire word. Each incorrect guess (letter or word)" +
                    " will count as an attempt. \nGuessing the same letter twice is " +
                    "free, but guessing the same word twice is not free. " +
                    "\nCollectively the players have the same number of attempts as there are " +
                    "letters in the word. ";

    public void start() throws Exception {
        if (!wf.isLibraryReady()) {
            //Fixme: this must be thrown to the view
            throw new Exception("The library of words is not done reading yet, try again in a while.");
        }
        String word = wf.supplyWord().toUpperCase();
        guessedLetters = new ArrayList<>();

        //Prints the chosen word to the server side
        System.out.println("Word: " + word);

        letters = word.toCharArray();
        noLetters = letters.length;
        guessed = new boolean[noLetters]; // initiated to false
        attemptsLeft = noLetters;
    }

    //Fixme: return void
    public DTO makeGuess(String word) {
        char[] guess = word.toUpperCase().toCharArray();

        boolean singleLetter = (guess.length == 1);
        if (singleLetter) {
            if (guessedLetters.contains(guess[0])) {
                return new DTO(this);
            }

            guessedLetters.add(guess[0]);
            if (!guessLetter(guess[0]))
                attemptsLeft--;

            return new DTO(this);
        }
        else {
            if (!guessWord(guess))
                attemptsLeft--;

            return new DTO(this);
        }
    }

    private boolean guessWord(char[] guess) {
        boolean correctGuess = false;
        if (Arrays.equals(guess, letters)) {
            correctGuess = true;
            for (int i = 0; i < guessed.length; i++)
                guessed[i] = true;
        }

        return correctGuess;
    }

    private boolean guessLetter(char letter) {
        boolean correctGuess = false;
        for (int i = 0; i < noLetters; i++)
            if (letters[i] == letter) {
                guessed[i] = true;
                correctGuess = true;
            }

        return correctGuess;
    }

    boolean allTrue() {
        for (boolean value: guessed)
            if (!value)  return false;

        return true;
    }

    String stringify() {
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < noLetters; i++) {
            if (guessed[i])
                r.append(String.valueOf(letters[i])).append(" ");
            else
                r.append(UNKNOWN_LETTER).append(" ");
        }

        return r.toString().trim();
    }

    int getNoLetters() {
        return noLetters;
    }

    int getAttemptsLeft() {
        return attemptsLeft;
    }

    String getGuessedLetters() {
        return guessedLetters.toString();
    }

    public DTO getDTO() {
        return new DTO(this);
    }

    public String getRules() {
        return rules;
    }
}
