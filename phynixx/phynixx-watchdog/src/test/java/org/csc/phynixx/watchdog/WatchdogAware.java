/**
 * 
 */
package org.csc.phynixx.watchdog;

import org.csc.phynixx.generator.IDGenerator;
import org.csc.phynixx.logger.IPhynixxLogger;
import org.csc.phynixx.logger.PhynixxLogManager;
import org.csc.phynixx.watchdog.log.ConditionViolatedLog;

/**
 * 
 * this class starts both a thread and a watchdog watching itsself.
 * It can must be explicitly killed.
 * The watchdog is killed, if the C'tor parameter removeConditionOnExit is true.  
 * 
 * If not the registry waits to the condition to be finalized and the condition to become useless.
 * 
 * @author christoph
 *
 */
public class WatchdogAware extends TimeoutCondition implements Runnable, IWatchedCondition
{
	
	private static final IDGenerator ID_GENERATOR= new IDGenerator(); 
	
	private static long CHECK_INTERVAL= 10; 

	private IPhynixxLogger log= PhynixxLogManager.getLogger(this.getClass());
	private long idleTime= -1l;
	
	private volatile boolean killed= false;
	
	private volatile byte conditionFailedCounter= 0; 
	private volatile boolean shutdown= false; 
	
	private IWatchdog watchdog= null; 
	
	private String id= null; 
	
	
	/**
	 * @param idleTime idle time of this runnable
	 * @param timeout timeout of the wachdog condition
	 */
	public WatchdogAware(long idleTime, long timeout) {
		super(timeout);
		this.idleTime = idleTime;		
		this.id= "WatchdogAware["+ID_GENERATOR.generateLong()+"]";
	}
	
	public void kill() {
		this.killed= true;
	}
	
	
	public boolean isKilled() {
		return this.killed;
	}
	
	public long getIdleTime() {
		return idleTime;
	}	
	
	
	
	public IWatchdog getWatchdog() {
		return watchdog;
	}
	
	
	
	public synchronized int getConditionFailedCounter() {
		return new Byte(this.conditionFailedCounter).intValue();
	}

	public synchronized boolean isShutdown() {
		return shutdown;
	}

	/**
	 * registers an Condition just counting the calls to check the it
	 * 
	 * @param wd
	 */
	private void registerWatching() 
	{
		IWatchdog wd= WatchdogRegistry.getTheRegistry().createWatchdog(CHECK_INTERVAL);
		this.setActive(true);		
		wd.registerCondition(this);		
		this.watchdog= wd;
		return;
	}
	
	public void run() 
	{
		this.killed= false;
		
		this.registerWatching();
		

		log.info(this.id+" started (idletime="+idleTime+" checkInterval="+super.getTimeout()+")");
	
		// System.out.println("Thread "+Thread.currentThread()+" is Killed "+ killed);
		while( !this.isKilled()  ) 
		{
			// wait until it time to check the conditions
			long start= System.currentTimeMillis();
			long waiting= this.idleTime;

			while( waiting > 0) {
				try {
					Thread.sleep(waiting);
				} catch (InterruptedException e) {
				} finally {
					waiting= this.idleTime - (System.currentTimeMillis()-start); 
				}
			}
			
			// log.debug("WatchdogAware "+id+" - Thread " + Thread.currentThread()+" -- live time "+ System.currentTimeMillis()+" interval="+(System.currentTimeMillis() - start) );
		}
		log.info(this.id+" is killed");
		this.shutdown= true; 
		
	}

	protected void finalize() throws Throwable {
		log.error(this.id+" is finalized");
		super.finalize();
	}
	
	public void conditionViolated() 
	{	
		this.resetCondition();		
		this.conditionFailedCounter++;		
		log.info(new ConditionViolatedLog(this, this.id));
	}	
	
		
}