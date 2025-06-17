package components;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class Hub extends Branch{
	
	private ArrayList<Branch> branches=new ArrayList<Branch>();
	private int currentIndex=0;
	
	// Semaphore for StandardTruck work coordination
	private Semaphore standardTruckWork = new Semaphore(0);
	
	public Hub() {
		super("HUB");
	}
	

	public ArrayList<Branch> getBranches() {
		return branches;
	}
	
	// Getter for StandardTruck work semaphore
	public Semaphore getStandardTruckWorkSemaphore() {
		return standardTruckWork;
	}

	
	public void add_branch(Branch branch) {
		branches.add(branch);
	}
	
	// FIXED: Only send truck when there's actual work, and signal semaphore
	public synchronized void sendTruck(StandardTruck t) {
		// Find a branch that has packages to collect
		Branch targetBranch = findBranchWithWork();
		if (targetBranch == null) return; // No work available
		
		synchronized(t) {
			t.notify();
		}
		t.setAvailable(false);
		t.setDestination(targetBranch);
		t.load(this, targetBranch, Status.BRANCH_TRANSPORT);
		t.setTimeLeft(((new Random()).nextInt(10)+1)*10);
		t.initTime = t.getTimeLeft();
		System.out.println(t.getName() + " is on it's way to " + targetBranch.getName() + ", time to arrive: "+t.getTimeLeft());	
	}
	
	// Find branch that has packages needing transport
	public Branch findBranchWithWork() {
		// Check all branches for packages that need transport
		for (Branch branch : branches) {
			synchronized(branch) {
				for (Package p : branch.getPackages()) {
					if (p.getStatus() == Status.BRANCH_STORAGE) {
						return branch; // Found work!
					}
				}
			}
		}
		
		// Check hub for packages that need delivery to branches
		synchronized(this) {
			for (Package p : this.getPackages()) {
				if (p.getStatus() == Status.HUB_STORAGE) {
					int destBranch = p.getDestinationAddress().zip;
					if (destBranch < branches.size()) {
						return branches.get(destBranch); // Found delivery work!
					}
				}
			}
		}
		
		return null; // No work found
	}
	
	// Signal that there's work for StandardTrucks
	public void signalStandardTruckWork() {
		standardTruckWork.release();
	}
	
	// Override addPackage to signal StandardTruck work when needed
	@Override
	public synchronized void addPackage(Package pack) {
		super.addPackage(pack); // Call parent method
		
		// Signal StandardTruck work if this package needs hub transport
		if (pack.getStatus() == Status.HUB_STORAGE) {
			signalStandardTruckWork();
		}
	}
	
	
	public synchronized void shipNonStandard(NonStandardTruck t) {
		for (Package p: listPackages) {
			if (p instanceof NonStandardPackage) {
				synchronized(t) {
					t.notify();
				}
				t.collectPackage(p);
				listPackages.remove(p);
				return;
			}
		}	
	}
	
	
	@Override
	public void work() {
		// Empty
	}
	
	
	// FIXED: Remove automatic truck sending, let trucks wait for work
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
		    
		    // Handle NonStandardTrucks only (they still work on-demand)
			for (Truck t : listTrucks) {
				if (t.isAvailable() && t instanceof NonStandardTruck) {
					shipNonStandard((NonStandardTruck)t);
				}	
			}
			
			// StandardTrucks will wait using semaphore - no active sending needed
			
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}