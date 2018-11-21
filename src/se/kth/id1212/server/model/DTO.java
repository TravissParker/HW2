package se.kth.id1212.server.model;

public class DTO {
    private boolean gameWon;
    private String gameState;
    private int noLetters;
    private int remainingAttempts;
    private String guessedLetters;

    public DTO(Game game) {
        this.gameState = game.stringify();
        this.noLetters = game.getNoLetters();
        this.remainingAttempts = game.getAttemptsLeft();
        this.gameWon = game.allTrue();
        this.guessedLetters = game.getGuessedLetters();
        this.guessedLetters = guessedLetters.replace("[", "");
        this.guessedLetters = guessedLetters.replace("]", "");
    }

    public boolean gameWon() {
        return gameWon;
    }

    public String getGameState() {
        return gameState;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public String getGuessedLetters() {
        return guessedLetters;
    }

    public int getNoLetters() {
        return noLetters;
    }

}
