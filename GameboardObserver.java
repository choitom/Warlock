/**
 * Interface to specify gameboard observer behavior.
 *
 * Authors: Tom Choi, Kiya Govek, Ryan Gorey, Kiran Tomlinson
 */
package warlock;

import java.util.Stack;
import java.util.Queue;

interface GameboardObserver {
    void updateBoard(Queue<Stack<int[]>> highlightStages, Queue<Rune[][]> boardStages);
    void updateScore(int score);
    void displayShuffleMessage();
}