/**
 * Implementation of the gamebaord. Plays the role of the model in MVC and of Subject in the Observer pattern.
 * The GameboardModel keeps track of the state of the board, and performs all operations on the board. It notifies View when the state of the board changes.
 *
 * Authors: Tom Choi, Kiya Govek, Ryan Gorey, Kiran Tomlinson
 */
package warlock;


import java.io.*;
import java.util.*;

public class GameboardModel implements GameboardModelInterface, GameboardSubject {

    /**
     * INSTANCE VARIABLES
     */

    private Rune[][] board;
    private final int width;
    private final int height;
    private int score;
    private boolean justShuffled;
    private int hintsRemaining;
    private ArrayList<GameboardObserver> gameboardObservers;
    private Queue<Stack<int[]>> highlightStages;
    private Queue<Rune[][]> boardStages;

    /**
     * CONSTRUCTOR
     * Instantiates the gameboard and initializes all instance variables
     */
    public GameboardModel() {
        gameboardObservers = new ArrayList<GameboardObserver>();
        highlightStages = new LinkedList<Stack<int[]>>();
        boardStages = new LinkedList<Rune[][]>();

        score = 0;
        width = 8;
        height = 8;
        hintsRemaining = 3;

        instantiateBoard();
        justShuffled = false;
    }

    /**
     * PUBLIC METHODS
     */

    /**
     * Register an observer object to Gameboard Model
     * @param observer observer object
     */
    public void addGameboardObserver(GameboardObserver observer){
        gameboardObservers.add(observer);
    }

    /**
     * Remove an observer object from Gameboard Model subscription
     * @param observer observer object
     */
    public void removeGameboardObserver(GameboardObserver observer) {
        gameboardObservers.add(observer);
    }

    /**
     * Model pushes the changes to the game board to its observers
     */
    public void notifyGameboardObservers() {
        for (GameboardObserver observer: gameboardObservers) {
            observer.updateBoard(highlightStages, boardStages);
            observer.updateScore(score);
        }
    }

    /**
     * Model tells observers to display a shuffle message
     */
    public void notifyShuffleObservers(){
        for (GameboardObserver gameboardObserver : gameboardObservers) {
            gameboardObserver.displayShuffleMessage();
        }
    }

    public Rune[][] getBoard() {
        return board;
    }

    public int getHintsRemaining() { return hintsRemaining; }

    /**
     * User clicked on two runes, see if they should swap
     * If they make a match, swap them and clear board of matches
     * If not, swap them back
     * @return true if the swap results in a match, else false
     */
    public boolean handleSwap(int[] coord1, int[] coord2) {
        swapRunes(coord1, coord2);
        if (hasMatch()) {
            removeMatches(true);
            ensurePossibleMatch();
        } else {
            swapRunes(coord1, coord2);
            return false;
        }

        if (justShuffled) {
            boardStages = new LinkedList<Rune[][]>();
            highlightStages = new LinkedList<Stack<int[]>>();
            boardStages.offer(getDeepCopyOfRuneArray(board));
            notifyShuffleObservers();
        }

        notifyGameboardObservers();
        return true;
    }

    public int getScore() {
        return this.score;
    }

    /**
     * Gets the coordinates of two runes that can be swapped to make a match
     * @return returns the coordinates of the two runes to swap
     */
    public int[][] getHintCoords() {
        int[][] swapHints = getPossibleMatch();
        if (hintsRemaining > 0) {hintsRemaining -= 1;}
        return swapHints;
    }

    /**
     * Update the game score
     * @return true if the new score is a highscore, else false
     */
    public boolean recordScore(){
        int[] highScores = readHighScoresFromFile();
        int[] updatedScores = updateHighScores(highScores, score);

        try {
            BufferedWriter scoreWriter = new BufferedWriter(new FileWriter("warlock/resources/SCORE.txt"));
            for(int scoreIndex = 0; scoreIndex < updatedScores.length; scoreIndex++){
                scoreWriter.write("" + updatedScores[scoreIndex]);
                if(scoreIndex < updatedScores.length -1) {
                    scoreWriter.write(" ");
                }
            }
            scoreWriter.close();
        } catch(IOException e) {
            System.out.println("Error in writing the scores.");
        }

        return (score == updatedScores[0]);
    }

    /**
     * Get the high scores
     * @return an array containing the high scores
     */
    public int[] getHighScores(){
        return readHighScoresFromFile();
    }


    /**
     * PRIVATE METHODS
     */

    /**
     * Returns rune at a specific coordinates
     * @param row row of a rune
     * @param col column of a rune
     * @return rune
     */
    private Rune getRuneAt(int row, int col) {
        return board[row][col];
    }

    /**
     * Set the rune at a specific coordinate in the game board
     * @param row row of a rune
     * @param col row of a column
     * @param rune rune
     */
    private void setRuneAt(int row, int col, Rune rune) {
        board[row][col] = rune;
    }

    /**
     * Creates a new gameboard that has possible matches but no existing matches
     */
    private void instantiateBoard() {
        board = new Rune[width][height];
        // Add random rune to each coordinate of gameboard
        for(int row=0; row<height; row++) {
            for(int col=0; col<width; col++) {
                Rune newRune = new Rune();
                setRuneAt(row,col,newRune);
            }
        }
        removeMatches(false);
        ensurePossibleMatch();
    }



    /**
     * Removes all runes that are matched in sets of three or higher from the board. Replaces the matched
     * runes with new runes that fill in from the top.
     * @param shouldScore tells the method whether or not the removed runes should contribute to the score.
     */
    private void removeMatches(boolean shouldScore) {
        boardStages = new LinkedList<Rune[][]>();
        highlightStages = new LinkedList<Stack<int[]>>();

        while (hasMatch()) {
            Stack<int[]> currMatchedRunes = getMatchedRunes();
            if (shouldScore) {
                boardStages.offer(getDeepCopyOfRuneArray(board));
                highlightStages.offer(getDeepCopyOfIntArrayStack(currMatchedRunes));
            }

            setRunesToMatched(currMatchedRunes);

            if (shouldScore) {
                scoreMatches();
            }

            fillInRunes();

            if (shouldScore) {
                boardStages.offer(getDeepCopyOfRuneArray(board));
            }
        }
        // Still want to pass last board to observers, even if not scoring matches
        if (!shouldScore) {
            boardStages.offer(getDeepCopyOfRuneArray(board));
        }
    }

    /**
     * Makes sure that the board has at least one potential match (also known as a Swapportunity (TM)). Any
     * removed runes here do not contribute to the user's score.
     */
    private void ensurePossibleMatch() {
        justShuffled = hasNoPossibleMatches();

        while (hasNoPossibleMatches()) {
            shuffleBoard();
            removeMatches(false);
        }
    }

    /**
     * Swap the runes in the game board
     * @param coord1 a coordinate of a clicked rune
     * @param coord2 a coordinate of the other rune to swap
     */
    private void swapRunes(int[] coord1, int[] coord2) {
        Rune tempRune = getRuneAt(coord1[0], coord1[1]);
        setRuneAt(coord1[0],coord1[1],getRuneAt(coord2[0],coord2[1]));
        setRuneAt(coord2[0],coord2[1],tempRune);
    }

    /**
     * Adds to the user's score based on matches in the board
     */
    private void scoreMatches() {
        float matchedRuneCounter = 0;

        for (int row = 0; row < this.height; row++) {
            for (int col = 0; col < this.width; col++) {
                Rune rune = getRuneAt(row,col);
                if (rune.getIsMatched()) {
                    matchedRuneCounter++;
                }
            }
        }

        // increment score depending on number of gems matched
        if (matchedRuneCounter == 3) {
            score += 30;
        } else if (matchedRuneCounter == 4) {
            score += 50;
        } else {
            score += matchedRuneCounter * 20;
        }
    }

    /**
     * Shuffles the positions of the existing runes on the board
     */
    private void shuffleBoard() {
        Rune[] boardRunes = new Rune[64];
        for (int row = 0; row < this.height; row++) {
            for (int col = 0; col < this.width; col++) {
                boardRunes[row*8 + col] = getRuneAt(row, col);
            }
        }

        shuffleArray(boardRunes);

        for (int row = 0; row < this.height; row++) {
            for (int col = 0; col < this.width; col++) {
                setRuneAt(row, col, boardRunes[row*8 + col]);
            }
        }
    }

    /**
     * Randomly reshuffles and array of runes
     * code for shuffling the array credited to Stack Overflow user Dan Bray, found at:
     * http://stackoverflow.com/questions/1519736/random-shuffling-of-an-array
     * @param array array to be shuffled
     */
    private static void shuffleArray(Rune[] array) {
        int index;
        Rune temp;
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--)
        {
            index = random.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    /**
     * Move runes down to fill void resulted from matches.
     * Then, populate the top with new runes.
     */
    private void fillInRunes() {
        for (int row = this.width-1; row>=0; row--) {
            for (int col = 0; col < this.height; col++) {
                if (getRuneAt(row,col).getIsMatched()) {
                    moveRuneFromAbove(row, col);
                }
            }
        }
    }

    /**
     * Move one rune into coordinate from spot above
     * @param row row
     * @param column column
     */
    private void moveRuneFromAbove(int row, int column) {
        int tempRow = row-1;
        while (tempRow>=0 && getRuneAt(tempRow, column).getIsMatched()) {
            tempRow -= 1;
        }
        if (tempRow < 0) {
            Rune newRune = new Rune();
            setRuneAt(row, column, newRune);
        }
        else {
            Rune runeToMove = getRuneAt(tempRow, column);
            setRuneAt(row, column, runeToMove);
            Rune tempRune = new Rune();
            setRuneAt(tempRow, column, tempRune);
            // not actually matched, but will be cleared up afterward
            tempRune.setIsMatched(true);
        }
    }

    /**
     * Set all runes that are in matches to "matched"
     * @param matches Stack of rune coords that are matched
     */
    private void setRunesToMatched(Stack<int[]> matches) {
        while (! matches.empty()) {
            int[] coord = matches.pop();
            int row = coord[0];
            int col = coord[1];
            Rune matchedRune = getRuneAt(row,col);
            matchedRune.setIsMatched(true);
        }
    }

    /**
     * Check the entire board for matches
     * @return stack of rune coords that are matched
     */
    private Stack<int[]> getMatchedRunes() {
        Stack<int[]> matchedRunes = new Stack<int[]>();

        // Adds horizontal and vertical matches to the matchedRunes stack
        matchedRunes = findHorizontalMatches(matchedRunes);
        matchedRunes = findVerticalMatches(matchedRunes);

        return matchedRunes;
    }

    /**
     * Checks to see whether or not there are any matched runes on the board.
     * @return true if at least one match, false if no matches
     */
    private boolean hasMatch() {
        return !(getMatchedRunes().isEmpty());
    }

    /**
     * Go up through each column in the board to check for vertical matches of at least 3 runes in a row
     * @param matchedRunes stack of rune coords that are matched
     * @return vertical matches
     */
    private Stack<int[]> findVerticalMatches(Stack<int[]> matchedRunes) {

        for (int col = 0; col < this.width; col++) {
            int consecMatches = 1;
            int row = 1;

            while (row < this.height) {
                if (getRuneAt(row,col).getTypeName().equals(getRuneAt(row - 1,col).getTypeName())) {
                    consecMatches++;
                } else {
                    recordVerticalMatch(matchedRunes, consecMatches, row, col);
                    consecMatches = 1;
                }
                row++;
            }
            recordVerticalMatch(matchedRunes, consecMatches, row, col);
        }
        return matchedRunes;
    }

    /**
     * Record the coordinates of matches to matches stack
     * @param matchedRunes stack of rune coords that are matched
     * @param consecMatches number of runes in this match
     * @param row current row
     * @param col current col
     */
    private void recordVerticalMatch(Stack<int[]> matchedRunes, int consecMatches, int row, int col) {
        if (consecMatches >= 3) {
            while (consecMatches > 0) {
                int[] coord = new int[2];
                coord[0] = row - consecMatches;
                coord[1] = col;
                matchedRunes.push(coord);
                consecMatches--;
            }
        }
    }

    /**
     * Go up through each row in the board to check for horizontal matches of at least 3 runes in a row
     * @param matchedRunes stack of rune coords that are matched
     * @return horizontal matches
     */
    private Stack<int[]> findHorizontalMatches(Stack<int[]> matchedRunes) {

        for (int row = 0; row < this.height; row++) {
            int col = 1;
            int consecMatches = 1;

            while (col < this.width) {
                if (getRuneAt(row,col).getTypeName().equals(getRuneAt(row,col -1).getTypeName())) {
                    consecMatches++;
                } else {
                    recordHorizontalMatch(matchedRunes, consecMatches, row, col);
                    consecMatches = 1;
                }
                col++;
            }
            recordHorizontalMatch(matchedRunes, consecMatches, row, col);
        }
        return matchedRunes;
    }

    /**
     * Record the coordinates of matches to matches stack
     * @param matchedRunes stack of rune coords that are matched
     * @param consecMatches number of runes in this match
     * @param row current row
     * @param col current col
     */
    private void recordHorizontalMatch(Stack<int[]> matchedRunes, int consecMatches, int row, int col) {
        if (consecMatches >= 3) {
            while (consecMatches > 0) {
                int[] coord = new int[2];
                coord[0] = row;
                coord[1] = col - consecMatches;
                matchedRunes.push(coord);
                consecMatches--;
            }
        }
    }

    /**
     * Returns the coordinates of two gems that can be swapped to make a match, if there is one.
     * Otherwise, returns two null arrays.
     * @return possible match coordinates
     */
    private int[][] getPossibleMatch() {
        int[][] possibleMoveForMatch = new int[][] {null, null};
        Stack<int[]> matchedRunes;

        // Check if any swaps on the board will create a match, and record it if it does.
        // Stop after finding one match, or no all swaps are exhausted
        for (int row = 0; row < this.height; row++) {
            for (int col = 0; col < this.width; col++) {
                if (col >= 1) {
                    swapWithAdjacentLeft(row, col);
                    matchedRunes = getMatchedRunes();
                    swapWithAdjacentLeft(row, col);

                    // if swapping the rune with the rune to its left made a match, record their coordinates
                    if (!matchedRunes.isEmpty()) {
                        possibleMoveForMatch = new int[][] {{row, col-1}, {row, col}};
                        break;
                    }
                }

                if (row >= 1) {
                    swapWithAdjacentAbove(row, col);
                    matchedRunes = getMatchedRunes();
                    swapWithAdjacentAbove(row, col);

                    // if swapping the rune with the rune above it made a match, record their coordinates
                    if (!matchedRunes.isEmpty()) {
                        possibleMoveForMatch = new int[][] {{row - 1, col}, {row, col}};
                        break;
                    }
                }
            }
        }
        return possibleMoveForMatch;
    }

    /**
     * Checks whether or not two runes may be swapped to make a match, and returns true if it can.
     * A possible match is also known in the industry as a Swapportunity (TM) - Credit to Kiran Tomlinson [ thanks, Ryan :) ]
     * @return true if there is a swapportunity, else false
     */
    private boolean hasNoPossibleMatches() {
        return getPossibleMatch()[0] == null;
    }

    /**
     * Swap a rune with the rune to its left. Takes the row and column of the rune to be swapped with the rune to its left.
     * @param row row index of rune to the left
     * @param col col index of rune to the left
     */
    private void swapWithAdjacentLeft(int row, int col) {
        Rune temp = board[row][col];
        board[row][col] = board[row][col-1];
        board[row][col-1] = temp;
    }

    /**
     * Swap a rune with the rune above it. Takes the row and column of the rune to be swapped with the rune above.
     * @param row row index of rune above
     * @param col col index of rune above
     */
    private void swapWithAdjacentAbove(int row, int col) {
        Rune temp = board[row][col];
        board[row][col] = board[row-1][col];
        board[row-1][col] = temp;
    }

    /**
     * Compare a new score with the current high scores and update new high scores
     * @param highScores a list of current high scores
     * @param newScore a new score
     * @return a list of updated new high scores
     */
    private int[] updateHighScores(int[] highScores, int newScore){

        for (int testIndex = 0; testIndex < highScores.length; testIndex++) {
            if (newScore > highScores[testIndex]) {
                int previous = newScore;

                for (int replaceIndex = testIndex; replaceIndex < highScores.length; replaceIndex++) {
                    int temp = highScores[replaceIndex];
                    highScores[replaceIndex] = previous;
                    previous = temp;
                }

                break;
            }
        }

        return highScores;
    }

    /**
     * Retrieves a list of current high scores from the SCORE.txt file
     * @return a list of current high scores
     */
    private int[] readHighScoresFromFile(){
        int[] highScores = new int[5];
        try{
            BufferedReader scoreReader = new BufferedReader(new FileReader("warlock/resources/SCORE.txt"));
            String[] scoreList = (scoreReader.readLine()).split(" ");
            for(int scoreIndex = 0; scoreIndex < scoreList.length; scoreIndex++){
                highScores[scoreIndex] = Integer.parseInt(scoreList[scoreIndex]);
            }
            scoreReader.close();
        } catch(FileNotFoundException e){
            return new int[] {0, 0, 0, 0, 0};
        } catch (IOException e) {
            return new int[] {0, 0, 0, 0, 0};
        }
        return highScores;
    }

    /**
     * Creates a deep copy of the current board array
     * @return a new 2D rune array of the board
     */
    private Rune[][] getDeepCopyOfRuneArray(Rune[][] runeArray) {
        Rune[][] deepCopy = new Rune[width][height];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Rune newRune = new Rune(runeArray[row][col]);
                deepCopy[row][col] = newRune;
            }
        }

        return deepCopy;
    }

    /**
     * Creates a deep copy of an array of matched runes.
     * @param currMatchedRunes stack of rune coords that are matched to be copied
     * @return a deep copy of currMatchedRunes
     */
    private Stack<int[]> getDeepCopyOfIntArrayStack(Stack<int[]> currMatchedRunes) {
        Stack<int[]> deepCopy = new Stack<>();

        for (int[] matchedRune : currMatchedRunes) {
            int[] newMatchedRune = new int[] {matchedRune[0], matchedRune[1]};
            deepCopy.push(newMatchedRune);
        }

        return deepCopy;
    }
}