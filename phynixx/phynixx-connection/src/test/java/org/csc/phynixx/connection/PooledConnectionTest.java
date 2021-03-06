package org.csc.phynixx.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.csc.phynixx.common.TestUtils;
import org.csc.phynixx.common.TmpDirectory;
import org.csc.phynixx.exceptions.DelegatedRuntimeException;
import org.csc.phynixx.logger.IPhynixxLogger;
import org.csc.phynixx.logger.PhynixxLogManager;
import org.csc.phynixx.loggersystem.ILoggerFactory;
import org.csc.phynixx.loggersystem.PerTransactionStrategy;
import org.csc.phynixx.loggersystem.channellogger.FileChannelLoggerFactory;
import org.csc.phynixx.test_connection.ActionInterruptedException;
import org.csc.phynixx.test_connection.ITestConnection;
import org.csc.phynixx.test_connection.TestConnection;
import org.csc.phynixx.test_connection.TestConnectionFactory;
import org.csc.phynixx.test_connection.TestConnectionStatusManager;


public class PooledConnectionTest extends TestCase
{
    private IPhynixxLogger logger= PhynixxLogManager.getLogger(this.getClass());

	private PooledConnectionFactory factory= null; 
	
	private RecoveryListener recoveryListner= new RecoveryListener();
	
	private static final int POOL_SIZE= 30; 
	

	private TmpDirectory tmpDir= null; 
	
	protected void setUp() throws Exception 
	{
		// configuring the log-system (e.g. log4j)
		TestUtils.configureLogging(); 
		
		this.tmpDir= new TmpDirectory("howllogger");
		
		GenericObjectPool.Config cfg= new GenericObjectPool.Config();
		cfg.maxActive=POOL_SIZE;
		this.factory= new PooledConnectionFactory(new TestConnectionFactory(),cfg);

		ILoggerFactory loggerFactory= new FileChannelLoggerFactory("mt",this.tmpDir.getDirectory());
		PerTransactionStrategy strategy= new PerTransactionStrategy("abcd",loggerFactory); 
		
		this.factory.setLoggerSystemStrategy(strategy);
		this.recoveryListner= new RecoveryListener();
		this.factory.setConnectionProxyDecorator(this.recoveryListner);
		
	}

	protected void tearDown() throws Exception 
	{
		TestConnectionStatusManager.clear();


		// delete all tmp files ...
		this.tmpDir.clear();
		
		this.factory= null;
	}

	private static interface IActOnConnection {
		void doWork(ITestConnection con) ; 
	}
	

	private static List exceptions= Collections.synchronizedList(new ArrayList());
	
	private class Runner implements Runnable 
	{
		
		private IActOnConnection actOnConnection= null; 		
		
		public Runner(IActOnConnection actOnConnection) {
			this.actOnConnection = actOnConnection;
		}

		public void run() 
		{				
			ITestConnection con= null;
			int repeats= 1; //(int) (System.currentTimeMillis() % 13)+1;
			try {	
				Object poolObj= PooledConnectionTest.this.factory.getConnection();	
				
				try {
					con= (ITestConnection) poolObj;
				} catch(ClassCastException e) {
					throw new ClassCastException("Expected ITestConnection; return " + poolObj.getClass());
					}
			
				for( int i=0; i< repeats;i++) {
					long millis= (long) ( System.currentTimeMillis() % 133); 
					Thread.currentThread().sleep(millis);
					try {
						this.actOnConnection.doWork((ITestConnection)con);
					} catch (ActionInterruptedException e) {
					} catch(DelegatedRuntimeException e) {
						if( !(e.getRootCause() instanceof ActionInterruptedException) ) {
							throw e;
						}					
					}
				}
			}	catch( Throwable ex) {
				ex.printStackTrace();
				exceptions.add(new DelegatedRuntimeException("Thread " + Thread.currentThread(),ex));
			}finally {			
				if( con!=null) {
					con.close();
				}
			} 
		}
	};
	
	private void startRunners(IActOnConnection actOnConnection, int numThreads) throws Exception 
	{
		exceptions.clear();
		
		Set workers= new HashSet(); 
		for ( int i=0; i <numThreads; i++) 
		{
			Runner runner= new Runner(actOnConnection);
			Thread worker=new Thread(runner);
			workers.add(worker);
			worker.start();	
		}
				
		// wait until all threads are ready ..
		for (Iterator iterator = workers.iterator(); iterator.hasNext();) {
			Thread worker = (Thread)iterator.next();
			worker.join();	
		}
		
		if( exceptions.size()>0 ) {
			for( int i=0; i< exceptions.size();i++) {
				Exception ex= (Exception) exceptions.get(i);
				ex.printStackTrace();
			}
			throw new AssertionFailedError("Error occurred");
		}
		
		
	}
	
	

   public void testGoodCase() throws Exception {
		
		final int[] counter= new int[1];
		
		IActOnConnection actOnConnection= new IActOnConnection() {
			public void doWork(ITestConnection con) {
				con.act(5);
				con.act(7);
				synchronized (counter ) {
					counter[0]= con.getCurrentCounter();
				}
				
				con.rollback();
			}			
		};	
		
		this.startRunners(actOnConnection, POOL_SIZE*4);
		
		// nothing has to be recoverd ...
		
		
	}
   
   

   public void testInterruptedRollback() throws Exception {
		
		final int[] counter= new int[1];
		
		IActOnConnection actOnConnection= new IActOnConnection() {
			public void doWork(ITestConnection con) {
				con.act(5);
				con.act(7);
				TestConnection coreCon= (TestConnection) ( (IPhynixxConnectionProxy)con).getConnection();
				coreCon.setInterruptFlag(true);
				con.rollback();
			}			
		};	
		
		this.startRunners(actOnConnection, POOL_SIZE*4);
		
		this.factory.recover();
		
		// nothing has to be recoverd ...
		this.recoveryListner.recoveredConnections= POOL_SIZE*4;
		
		
	}
   
   
   private Properties loadHowlConfig() throws Exception
	{
      Properties howlprop= new Properties();
      howlprop.put("listConfig", "false");
      howlprop.put("bufferSize", "32");
      howlprop.put("minBuffers", "1");
      howlprop.put("maxBuffers", "1");
      howlprop.put("maxBlocksPerFile", "100");
      howlprop.put("logFileDir", this.tmpDir.getDirectory().getAbsolutePath());
      howlprop.put("logFileName", "test1");
      howlprop.put("maxLogFiles", "2");
      
      return howlprop;
	}
	
}
