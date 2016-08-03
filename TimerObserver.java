/**
 * Interface to specify timer observer behavior.
 *
 * Authors: Tom Choi, Kiya Govek, Ryan Gorey, Kiran Tomlinson
 */

package warlock;

interface TimerObserver {
    void updateTimer(int time);
    void terminateGame();
}
