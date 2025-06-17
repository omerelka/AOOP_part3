package components;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Branch implements Node, Runnable, Cloneable {
	private static int counter=0;
	private int branchId;
	private String branchName;
	protected ArrayList <Package> unsafeListPackages = new ArrayList<Package>();
	protected List<Package> listPackages = unsafeListPackages;
	protected ArrayList <Truck> listTrucks = new ArrayList<Truck>();
	private Point hubPoint;
	private Point branchPoint;
	protected boolean threadSuspend = false;
	
	// Semaphore for producer-consumer pattern
	private Semaphore workAvailable = new Semaphore(0);
	
	public Branch() {
		this("Branch "+counter);
	}
	
	public Branch(String branchName) {
		this.branchId=counter++;
		this.branchName=branchName;
		System.out.println("\nCreating "+ this);
	}
	
	public Branch(String branchName, Package[] plist, Truck[] tlist) {
		this.branchId=counter++;
		this.branchName=branchName;
		addPackages(plist);
		addTrucks(tlist);
	}
	
	// Clone method implementation
	@Override
	public Branch clone() throws CloneNotSupportedException {
		// Create new branch with incremented counter
		Branch clonedBranch = new Branch("Branch " + counter);
		
		// Clone trucks (deep copy with new IDs but same properties)
		for (Truck originalTruck : this.listTrucks) {
			Truck clonedTruck = cloneTruck(originalTruck);
			if (clonedTruck instanceof Van) {
				((Van) clonedTruck).setParentBranch(clonedBranch);
			}
			clonedBranch.addTruck(clonedTruck);
		}
		
		// Don't clone packages - new branch starts empty
		// Don't clone semaphore state - new branch starts fresh
		
		System.out.println("Cloned branch: " + this.branchName + " -> " + clonedBranch.branchName);
		return clonedBranch;
	}
	
	// Helper method to clone trucks using their clone() methods
	private Truck cloneTruck(Truck originalTruck) throws CloneNotSupportedException {
		return originalTruck.clone(); // Use the truck's own clone method
	}
	
	// Static method to get current counter (for external use)
	public static int getCurrentCounter() {
		return counter;
	}
	
	public synchronized List <Package> getPackages(){
		return this.listPackages;
	}
	
	// Getter for semaphore
	public Semaphore getWorkSemaphore() {
		return workAvailable;
	}
	
	public void printBranch() {
		System.out.println("\nBranch name: "+branchName);
		System.out.println("Packages list:");
		for (Package pack: listPackages)
			System.out.println(pack);
		System.out.println("Trucks list:");
		for (Truck trk: listTrucks)
			System.out.println(trk);
	}
	
	// FIXED: Add semaphore release and StandardTruck signaling
	public synchronized void addPackage(Package pack) {
		listPackages.add(pack);
		
		// Release semaphore permit if package needs processing by Van
		if (pack.getStatus() == Status.CREATION || pack.getStatus() == Status.DELIVERY) {
			workAvailable.release(); // Primary mechanism - semaphore
		}
		
		// Signal StandardTruck work if package needs branch transport
		if (pack.getStatus() == Status.BRANCH_STORAGE) {
			MainOffice.getHub().signalStandardTruckWork();
		}
		
		notifyAll(); // Additional coordination - wait/notify
	}
	
	public ArrayList <Truck> getTrucks(){
		return this.listTrucks;
	}
	
	public void addTruck(Truck trk) {
		listTrucks.add(trk);
	}
	
	public Point getHubPoint() {
		return hubPoint;
	}
	
	public Point getBranchPoint() {
		return branchPoint;
	}
	
	public synchronized void addPackages(Package[] plist) {
		for (Package pack: plist)
			addPackage(pack); // Use addPackage to trigger semaphore
	}
	
	public void addTrucks(Truck[] tlist) {
		for (Truck trk: tlist)
			listTrucks.add(trk);
	}

	public int getBranchId() {
		return branchId;
	}
	
	public String getName() {
		return branchName;
	}

	@Override
	public String toString() {
		return "Branch " + branchId + ", branch name:" + branchName + ", packages: " + listPackages.size()
				+ ", trucks: " + listTrucks.size();
	}

	@Override
	public synchronized void  collectPackage(Package p) {
		for (Truck v : listTrucks) {
			if (v.isAvailable()) {
				synchronized(v) {
					v.notify();
				}
				v.collectPackage(p);
				return;
			}
		}
	}

	@Override
	public synchronized void deliverPackage(Package p) {
		for (Truck v : listTrucks) {
			if (v.isAvailable()) {
				synchronized(v) {
					v.notify();
				}
				v.deliverPackage(p);
				return;
			}
		}	
	}

	@Override
	public void work() {	
		// This method is kept for compatibility but main work moved to Van
	}

	private boolean arePackagesInBranch() {
		for(Package p: listPackages) {
			if (p.getStatus() == Status.BRANCH_STORAGE)
				return true;
		}
		return false;
	}
	
	public void paintComponent(Graphics g, int y, int y2) {
		if (arePackagesInBranch())
			g.setColor(new Color(0,0,153));
		else
			g.setColor(new Color(51,204,255));
   		g.fillRect(20, y, 40, 30);
   		
   		g.setColor(new Color(0,102,0));
   		g.drawLine(60, y+15, 1120, y2);
   		branchPoint = new Point(60,y+15);
   		hubPoint = new Point(1120,y2);
	}

	// FIXED: Remove active package assignment - let Vans handle it via semaphore
	@Override
	public void run() {
		while(true) {
		    synchronized(this) {
                while (threadSuspend)
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
		    }
		    
		    // Just sleep - Vans will handle package processing via semaphore
		    try {
		    	Thread.sleep(300);
		    } catch (InterruptedException e) {
		    	e.printStackTrace();
		    }
		}
	}
	
	public synchronized void setSuspend() {
	   	threadSuspend = true;
	}

	public synchronized void setResume() {
	   	threadSuspend = false;
	   	notify();
	}
}