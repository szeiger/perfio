package perfio.internal;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class Trigger {
  private final AtomicInteger state = new AtomicInteger();
  private final Thread waiter;
  
  private static final int STATE_NONE = 0;
  private static final int STATE_LOCKED = 1;
  private static final int STATE_PENDING = 2;

  public Trigger(Thread waiter) { this.waiter = waiter; }

  public void fire() {
    //try { Thread.sleep(100); } catch (InterruptedException e) { throw new RuntimeException(e); }
    //System.out.println("fire() "+state.get());
    while(true) {
      switch(state.get()) {
        case STATE_PENDING -> { return; }
        case STATE_NONE -> {
          if(state.compareAndSet(STATE_NONE, STATE_PENDING)) return;
        }
        case STATE_LOCKED -> {
          state.set(STATE_PENDING);
          LockSupport.unpark(waiter);
        } 
      }
    }
  }
  
  public void await() throws InterruptedException {
    //Thread.sleep(100);
    //System.out.println("await() "+state.get());
    while(true) {
      switch(state.get()) {
        case STATE_PENDING -> {
          state.set(STATE_NONE);
          return;
        }
        case STATE_NONE -> {
          if(state.compareAndSet(STATE_NONE, STATE_LOCKED)) LockSupport.park();
        }
        case STATE_LOCKED -> LockSupport.park();
      }
    }
  }
}
