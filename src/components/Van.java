package components;

import java.awt.Color;
import java.awt.Graphics;

public class Van extends Truck {
	
	// Reference to parent branch for semaphore access
	private Branch parentBranch;
	
	public Van() {
		super();
		System.out.println("Creating " + this);
	}
	
	public Van(String licensePlate,String truckModel) {
		super(licensePlate,truckModel);
	}
	
	// Clone implementation for Van
	@Override
	public Van clone() throws CloneNotSupportedException {
		Van clonedVan = new Van();
		this.copyPropertiesTo(clonedVan);
		
		// Don't copy parent branch - will be set by Branch.clone()
		clonedVan.parentBranch = null;
		
		System.out.println("Cloned Van: " + this.getTruckID() + " -> " + clonedVan.getTruckID());
		return clonedVan;
	}
	
	// Method to set parent branch
	public void setParentBranch(Branch branch) {
		this.parentBranch = branch;
	}
	
	// Get parent branch
	public Branch getParentBranch() {
		return parentBranch;
	}

	@Override
	public String toString() {
		return "Van ["+ super.toString() + "]";
	}
	
	@Override
	public synchronized void deliverPackage(Package p) {
		this.getPackages().add(p);
		setAvailable(false);
		int time=(p.getDestinationAddress().street%10+1)*10;
		this.setTimeLeft(time);
		this.initTime = time;
		p.setStatus(Status.DISTRIBUTION);
		p.addTracking(new Tracking(MainOffice.getClock(), this, p.getStatus()));
		System.out.println("Van "+ this.getTruckID() + " is delivering package " + p.getPackageID() + ", time left: "+ this.getTimeLeft()  );
	}
	
	// Helper method to find package that needs processing
	private Package findPackageToProcess() {
		if (parentBranch == null) return null;
		
		synchronized(parentBranch) {
			for (Package p : parentBranch.getPackages()) {
				if (p.getStatus() == Status.CREATION || p.getStatus() == Status.DELIVERY) {
					return p;
				}
			}
		}
		return null;
	}
	
	// FIXED: Use semaphore-based consumer pattern and fix Van return logic
	@Override
	public void run() {
		while(true) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		    synchronized(this) {
                while (threadSuspend)
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
		    }
		    
			if (!this.isAvailable()) {
				// Handle current work
				this.setTimeLeft(this.getTimeLeft()-1);
				if (this.getTimeLeft()==0){
					Branch branch=null;
					for (Package p : this.getPackages()) {
						if (p.getStatus()==Status.COLLECTION) {
							// FIXED: Return to parent branch, not destination branch
							branch = parentBranch;
							if (branch != null) {
								synchronized(branch) {
									p.setStatus(Status.BRANCH_STORAGE);
									System.out.println("Van " + this.getTruckID() + " has collected package " +p.getPackageID()+" and arrived back to branch " + branch.getBranchId());
									branch.addPackage(p);
								}
							}
						}
						else if (p.getStatus()==Status.DISTRIBUTION) {
							p.setStatus(Status.DELIVERED);
							// FIXED: For delivery, remove from destination branch properly
							Branch destBranch = MainOffice.getHub().getBranches().get(p.getDestinationAddress().zip);
							if (destBranch != null) {
								synchronized(destBranch) {
									destBranch.listPackages.remove(p);
									System.out.println("Van " + this.getTruckID() + " has delivered package "+p.getPackageID() + " to the destination");
									if (p instanceof SmallPackage && ((SmallPackage)p).isAcknowledge()) {
										System.out.println("Acknowledge sent for package "+p.getPackageID());
									}
								}
							}
							branch = null; // No branch for delivery tracking
						}
						p.addTracking(new Tracking(MainOffice.getClock(), branch, p.getStatus()));
					}
					this.getPackages().removeAll(getPackages());
					this.setAvailable(true);
				}
			}
			else {
				// Consumer logic using semaphore + wait/notify
				if (parentBranch != null) {
					try {
						// Primary mechanism: wait for semaphore permit
						parentBranch.getWorkSemaphore().acquire();
						
						// Find and process package
						Package packageToProcess = findPackageToProcess();
						if (packageToProcess != null) {
							if (packageToProcess.getStatus() == Status.CREATION) {
								collectPackage(packageToProcess);
							} else if (packageToProcess.getStatus() == Status.DELIVERY) {
								deliverPackage(packageToProcess);
							}
						}
						
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					// Fallback: traditional wait if no parent branch set
					synchronized(this) {
						try {
							wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	@Override
	public void work() {
		// Keep empty for compatibility
	}

	@Override
	public void paintComponent(Graphics g) {
		if (isAvailable()) return;
		Package p = this.getPackages().get(getPackages().size()-1);	
		Point start=null;
		Point end=null;
		if (p.getStatus()==Status.COLLECTION) {
			start = p.getSendPoint();
			end = p.getBInPoint();
		}
		else if (p.getStatus()==Status.DISTRIBUTION) {
			start = p.getBOutPoint();
			end = p.getDestPoint();
		}
		
		if (start!=null) {
			int x2 = start.getX();
			int y2 = start.getY();
			int x1 = end.getX();
			int y1 = end.getY();
				
			double ratio = (double) this.getTimeLeft()/this.initTime;
			double length = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
			int dX = (int) (ratio*(x2-x1));
			int dY = (int) (ratio*(y2-y1));
				
			g.setColor(Color.BLUE);
			g.fillRect(dX+x1-8, dY+y1-8, 16, 16); 
			g.setColor(Color.BLACK);
			g.fillOval(dX+x1-12, dY+y1-12, 10, 10);
			g.fillOval(dX+x1, dY+y1, 10, 10);
			g.fillOval(dX+x1, dY+y1-12, 10, 10);
			g.fillOval(dX+x1-12, dY+y1, 10, 10);
		}
	}
}