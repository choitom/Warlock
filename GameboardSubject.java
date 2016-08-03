/**
 * Interface to specify gameboard subject behavior.
 *
 * Authors: Tom Choi, Kiya Govek, Ryan Gorey, Kiran Tomlinson
 */

package warlock;

interface GameboardSubject {
    void addGameboardObserver(GameboardObserver observer);
    void removeGameboardObserver(GameboardObserver observer);
    void notifyGameboardObservers();
    void notifyShuffleObservers();
}