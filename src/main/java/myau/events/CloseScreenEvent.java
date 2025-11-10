package myau.events;

import myau.event.events.callables.EventCancellable;

public class CloseScreenEvent extends EventCancellable {
    private final int windowId;

    public CloseScreenEvent(int windowId) {this.windowId = windowId;}

    public int getWindowId() {return this.windowId;}
}
