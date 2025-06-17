package components;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JPanel;

public class MainOffice implements Runnable{
	private static volatile MainOffice instance = null;
	
	// ThreadPool for customers
	ExecutorService customerExecutor = Executors.newFixedThreadPool(2);
	
	// File tracking with ReentrantReadWriteLock
	private final ReentrantReadWriteLock trackingFileLock = new ReentrantReadWriteLock();
	private static final String TRACKING_FILE = "tracking.txt";
	
	private int clock = 0;
	private Hub hub;
	private ArrayList<Package> packages = new ArrayList<Package>();
	private JPanel panel;
	private int maxPackages;
	private boolean threadSuspend = false;
	private boolean initialized = false;
	
	private MainOffice() {
	}
	
	public static MainOffice getInstance() {
		if (instance == null) {
			synchronized (MainOffice.class) {
				if (instance == null) {
					instance = new MainOffice();
				}
			}
		}
		return instance;
	}
	
	public synchronized void initialize(int branches, int trucksForBranch, JPanel panel, int maxPack) {
		if (initialized) {
			System.out.println("MainOffice already initialized");
			return;
		}
		
		this.panel = panel;
		this.maxPackages = maxPack;
		addHub(trucksForBranch);
		addBranches(branches, trucksForBranch);
		
		// Create 10 customers using ThreadPool
		createCustomers();
		
		this.initialized = true;
		System.out.println("\n\n========================== START ==========================");
	}
	
	// Method for writing to tracking file (MainOffice only)
	public void writeTrackingToFile(Package pkg, Tracking tracking) {
		trackingFileLock.writeLock().lock();
		try (FileWriter writer = new FileWriter(TRACKING_FILE, true)) {
			String nodeName = (tracking.node == null) ? "Customer" : tracking.node.getName();
			String line = String.format("%d,%d,%d,%s,%s%n", 
				pkg.getPackageID(), 
				pkg.getCustomerId(), 
				tracking.time, 
				nodeName, 
				tracking.status);
			writer.write(line);
			writer.flush();
		} catch (IOException e) {
			System.err.println("Error writing to tracking file: " + e.getMessage());
		} finally {
			trackingFileLock.writeLock().unlock();
		}
	}
	
	// Provide readLock for customers
	public Lock getTrackingReadLock() {
		return trackingFileLock.readLock();
	}
	
	public String getTrackingFileName() {
		return TRACKING_FILE;
	}
	
	// Create 10 customers using ThreadPool of size 2
	private void createCustomers() {
		for (int i = 0; i < 10; i++) {
			Customer customer = new Customer();
			customerExecutor.submit(customer);
		}
	}
	
	public static Hub getHub() {
		return getInstance().hub;
	}

	public static int getClock() {
		return getInstance().clock;
	}

	@Override
	public void run() {
		Thread hubThrad = new Thread(hub);
		hubThrad.start();
		for (Truck t : hub.listTrucks) {
			Thread trackThread = new Thread(t);
			trackThread.start();
		}
		for (Branch b: hub.getBranches()) {
			Thread branch = new Thread(b);
			for (Truck t : b.listTrucks) {
				Thread trackThread = new Thread(t);
				trackThread.start();
			}
			branch.start();
		}
		while(true) {
		    synchronized(this) {
                while (threadSuspend)
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
		    }
			tick();
		}
	}
	
	public void printReport() {
		for (Package p: packages) {
			System.out.println("\nTRACKING " +p);
			for (Tracking t: p.getTracking())
				System.out.println(t);
		}
	}
	
	public String clockString() {
		String s="";
		int minutes=clock/60;
		int seconds=clock%60;
		s+=(minutes<10) ? "0" + minutes : minutes;
		s+=":";
		s+=(seconds<10) ? "0" + seconds : seconds;
		return s;
	}
	
	public void tick() {
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(clockString());
		// if (clock++%5==0 && maxPackages>0) {
		// 	addPackage();
		// 	maxPackages--;
		// }
		clock++;
		panel.repaint();
	}
	
	public void branchWork(Branch b) {
		for (Truck t : b.listTrucks) {
			t.work();
		}
		b.work();
	}
	
	public void addHub(int trucksForBranch) {
		hub = new Hub();
		for (int i=0; i<trucksForBranch; i++) {
			Truck t = new StandardTruck();
			hub.addTruck(t);
		}
		Truck t=new NonStandardTruck();
		hub.addTruck(t);
	}
	
	public void addBranches(int branches, int trucks) {
		for (int i=0; i<branches; i++) {
			Branch branch=new Branch();
			for (int j=0; j<trucks; j++) {
				Van van = new Van();
				van.setParentBranch(branch);
				branch.addTruck(van);
			}
			hub.add_branch(branch);		
		}
	}
	
	public ArrayList<Package> getPackages(){
		return this.packages;
	}
	
	public void addPackage() {
		Random r = new Random();
		Package p;
		Branch br;
		Priority priority=Priority.values()[r.nextInt(3)];
		Address sender = new Address(r.nextInt(hub.getBranches().size()), r.nextInt(999999)+100000);
		Address dest = new Address(r.nextInt(hub.getBranches().size()), r.nextInt(999999)+100000);

		switch (r.nextInt(3)){
		case 0:
			p = new SmallPackage(priority, sender, dest, r.nextBoolean(), 0);
			br = hub.getBranches().get(sender.zip);
			br.addPackage(p);
			p.setBranch(br); 
			break;
		case 1:
			p = new StandardPackage(priority, sender, dest, (double)(r.nextFloat()+(r.nextInt(9)+1)), 0);
			br = hub.getBranches().get(sender.zip); 
			br.addPackage(p);
			p.setBranch(br); 
			break;
		case 2:
			p = new NonStandardPackage(priority, sender, dest, r.nextInt(1000), r.nextInt(500), r.nextInt(400), 0);
			hub.addPackage(p);
			break;
		default:
			return;
		}
		
		this.packages.add(p);
	}
	
	public synchronized void setSuspend() {
	   	threadSuspend = true;
		for (Truck t : hub.listTrucks) {
			t.setSuspend();
		}
		for (Branch b: hub.getBranches()) {
			for (Truck t : b.listTrucks) {
				t.setSuspend();
			}
			b.setSuspend();
		}
		hub.setSuspend();
	}

	public synchronized void setResume() {
	   	threadSuspend = false;
	   	notify();
	   	hub.setResume();
		for (Truck t : hub.listTrucks) {
			t.setResume();
		}
		for (Branch b: hub.getBranches()) {
			b.setResume();
			for (Truck t : b.listTrucks) {
				t.setResume();
			}
		}
	}
}