package se.kth.id1212.client.net;

import static common.Constants.DATA_DELIMITER;
import static common.Constants.NEW_LINE;
import static common.Constants.TYPE_INDEX;

public class MessageProcessor {

    public String processMsg(String data) {
        String[] splitData = data.split(DATA_DELIMITER);

        String returnValue;
        switch (common.Command.valueOf(splitData[TYPE_INDEX])) {
            case GUESS:
                returnValue = splitData[1] + " guessed: " + splitData[2];
                break;
            case STATE:
                String state = splitData[1];
                String outlook;
                int attemptsLeft = Integer.parseInt(splitData[2]);
                boolean gameWon = Boolean.parseBoolean(splitData[3]);

                if (attemptsLeft < 1 & !gameWon)
                    outlook = "Game over, better luck next time..."+ NEW_LINE;
                else if (gameWon)
                    outlook = "Good job, you won!"+ NEW_LINE;
                else
                    outlook = attemptsLeft + " attempts to go."+ NEW_LINE;

                returnValue = splitData[5] + " letter word: " + state +
                        NEW_LINE + outlook +
                        NEW_LINE + "Previously guessed:" +
                        NEW_LINE + splitData[4];
                break;
            case USER:
                returnValue = splitData[1] + " changed name to " + splitData[2] + NEW_LINE;
                break;
            case START:
                returnValue = splitData[1] + " started a new game!" + NEW_LINE;
                break;
            case DISCONNECT:
                returnValue = splitData[1] + " left the game :("+ NEW_LINE;
                break;
            case SCORE:
                returnValue = "Your score is " + splitData[1];
                break;
            case RUNNING:
                returnValue = "A game is already running, use the GUESS command to play."+ NEW_LINE;
                break;
            case NOT_RUNNING:
                returnValue = "The game hasn't been started, use the START command to play."+ NEW_LINE;
                break;
            case RULES:
                returnValue = splitData[1];
                break;
            default:
                returnValue = "ERROR";
        }
        return returnValue;
    }
}
