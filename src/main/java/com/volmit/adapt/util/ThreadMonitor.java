/*------------------------------------------------------------------------------
 -   Adapt is a Skill/Integration plugin  for Minecraft Bukkit Servers
 -   Copyright (c) 2022 Arcane Arts (Volmit Software)
 -
 -   This program is free software: you can redistribute it and/or modify
 -   it under the terms of the GNU General Public License as published by
 -   the Free Software Foundation, either version 3 of the License, or
 -   (at your option) any later version.
 -
 -   This program is distributed in the hope that it will be useful,
 -   but WITHOUT ANY WARRANTY; without even the implied warranty of
 -   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 -   GNU General Public License for more details.
 -
 -   You should have received a copy of the GNU General Public License
 -   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 -----------------------------------------------------------------------------*/

package com.volmit.adapt.util;

import com.volmit.adapt.Adapt;

/**
 * Not particularly efficient or perfectly accurate but is great at fast thread
 * switching detection
 *
 * @author dan
 */
public class ThreadMonitor extends Thread {
    private final Thread monitor;
    private boolean running;
    private State lastState;
    private final ChronoLatch cl;
    private PrecisionStopwatch st;
    int cycles = 0;
    private final RollingSequence sq = new RollingSequence(3);

    private ThreadMonitor(Thread monitor) {
        running = true;
        st = PrecisionStopwatch.start();
        this.monitor = monitor;
        lastState = State.NEW;
        cl = new ChronoLatch(1000);
        start();
    }

    public void run() {
        while(running) {
            try {
                Thread.sleep(0);
                State s = monitor.getState();
                if(lastState != s) {
                    cycles++;
                    pushState(s);
                }

                lastState = s;

                if(cl.flip()) {
                    Adapt.info("Cycles: " + Form.f(cycles) + " (" + Form.duration(sq.getAverage(), 2) + ")");
                }
            } catch(Throwable e) {
                running = false;
                break;
            }
        }
    }

    public void pushState(State s) {
        if(s != State.RUNNABLE) {
            if(st != null) {
                sq.put(st.getMilliseconds());
            }
        } else {

            st = PrecisionStopwatch.start();
        }
    }

    public void unbind() {
        running = false;
    }

    public static ThreadMonitor bind(Thread monitor) {
        return new ThreadMonitor(monitor);
    }
}
