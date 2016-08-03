/**
 * Implementation of the timer. Plays the role of the model in MVC and of Subject in the Observer pattern.
 * The timer keeps track of the game time, and tells View wehn the time changes
 *
 * Authors: Tom Choi, Kiya Govek, Ryan Gorey, Kiran Tomlinson
 */

package warlock;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class TimerModel implements TimerModelInterface, TimerSubject{

    /**
     * INSTANCE VARIABLES
     */

    private ArrayList<TimerObserver> timerObservers;
    private int time;
    private long sysTimeWhenResumed;
    private int timerError;
    private Timer timer;

    /**
     * CONSTRUCTOR
     * Initiates the game board and the timer
     * @param time the time to start the timer with
     */
    public TimerModel(int time) {
        timerObservers = new ArrayList<TimerObserver>();
        this.time = time;
        timerError = 0;
        startTimer();
    }

    /**
     * PUBLIC METHODS
     */

    /**
     * Register an observer object to Timer Model
     * @param observer observer object
     */
    public void addTimerObserver(TimerObserver observer){
        timerObservers.add(observer);
    }

    /**
     * Remove an observer object from Timer Model subscription
     * @param observer observer object
     */
    public void removeTimerObserver(TimerObserver observer) {
        timerObservers.add(observer);
    }

    /**
     * Model pushes the changes to the timer to its observers
     */
    public void notifyTimerObservers() {
        for (TimerObserver observer: timerObservers) {
            if(time != 0) {
                observer.updateTimer(time);
            } else{
                observer.terminateGame();
            }
        }
    }

    /**
     * Stop the game timer
     */
    public void stopTimer() {
        timer.cancel();
        long sysTime = System.currentTimeMillis();
        if (sysTime - sysTimeWhenResumed < 1000) {
            timerError += sysTime - sysTimeWhenResumed;
        }

        if (timerError > 1000) {
            timerError = timerError - 1000;
            decrementTimer();
            notifyTimerObservers();
        }
    }

    /**
     * Create the game timer which decrements the time every second
     */
    public void startTimer() {
        sysTimeWhenResumed = System.currentTimeMillis();
        timer = new Timer();
        timer.schedule(
            new TimerTask() {
                public void run() {
                    decrementTimer();
                    notifyTimerObservers();
                }
            }, 1000, 1000);
    }


    /**
     * PRIVATE METHODS
     */

    /**
     * Decrement time variable, but not below 0
     */
    private void decrementTimer() {
        if (time > 0) {
            time--;
        }
    }

}