/**
 * Interface to specify timer subject behavior.
 *
 * Authors: Tom Choi, Kiya Govek, Ryan Gorey, Kiran Tomlinson
 */

package warlock;

interface TimerSubject {
    void addTimerObserver(TimerObserver observer);
    void removeTimerObserver(TimerObserver observer);
    void notifyTimerObservers();
}
