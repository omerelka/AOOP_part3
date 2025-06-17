package components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import program.Main;

public class Customer implements Runnable, Observer {
    PackageIterator iterator;
    private static int customerCounter = 1;
    private final int customerId;
    private final Address customerAddress;
    private final ArrayList<Integer> myPackages = new ArrayList<>();
    private final ArrayList<Package> myPackageObjects = new ArrayList<>();
    private volatile int deliveredPackagesCount = 0;
    private volatile boolean allDelivered = false;
    
    public Customer() {
        customerId = customerCounter++;
        iterator = new PackageIterator(customerId, MainOffice.getInstance().getPackages());
        Random r = new Random();
        int branchCount = MainOffice.getHub().getBranches().size();
        customerAddress = new Address(r.nextInt(branchCount), r.nextInt(999999) + 100000);
        System.out.println("Creating Customer " + customerId);
    }
    
    @Override
    public void run() {
        createAllPackages();
        
        waitForAllDeliveries();
        
        System.out.println("Customer " + customerId + " finished - all packages delivered!");
    }
    
    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof Package) {
            Package deliveredPackage = (Package) arg;
            // Only react to MY packages
            if (deliveredPackage.getCustomerId() == this.customerId) {
                synchronized (this) {
                    deliveredPackagesCount++;
                    System.out.println("Customer " + customerId + " IMMEDIATE notification: Package " + 
                                     deliveredPackage.getPackageID() + " delivered! (" + 
                                     deliveredPackagesCount + "/" + myPackages.size() + ")");
                    
                    // Check if all packages are delivered
                    if (deliveredPackagesCount >= myPackages.size()) {
                        allDelivered = true;
                        this.notifyAll(); // Wake up the waiting thread
                    }
                }
            }
        }
    }
    
    private void createAllPackages() {
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep((new Random().nextInt(4) + 2) * 1000); // 2-5 seconds
                createPackage();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Customer " + customerId + " finished creating all 5 packages. Now waiting for deliveries...");
    }
    
    private void createPackage() {
        Random r = new Random();
        Package p = null;
        Branch br;
        Priority priority = Priority.values()[r.nextInt(3)];
        
        // Random destination
        Address dest = new Address(r.nextInt(MainOffice.getHub().getBranches().size()), 
                                 r.nextInt(999999) + 100000);
        
        switch (r.nextInt(3)) {
            case 0:
                p = new SmallPackage(priority, customerAddress, dest, r.nextBoolean(), customerId);
                br = MainOffice.getHub().getBranches().get(customerAddress.zip);
                br.addPackage(p);
                p.setBranch(br);
                break;
            case 1:
                p = new StandardPackage(priority, customerAddress, dest, 
                                      (double)(r.nextFloat() + (r.nextInt(9) + 1)), customerId);
                br = MainOffice.getHub().getBranches().get(customerAddress.zip);
                br.addPackage(p);
                p.setBranch(br);
                break;
            case 2:
                p = new NonStandardPackage(priority, customerAddress, dest, 
                                         r.nextInt(1000), r.nextInt(500), r.nextInt(400), customerId);
                MainOffice.getHub().addPackage(p);
                break;
        }
        
        if (p != null) {
            myPackages.add(p.getPackageID());
            myPackageObjects.add(p);
            p.addObserver(this);
            MainOffice.getInstance().getPackages().add(p);
            System.out.println("Customer " + customerId + " created package " + p.getPackageID() + 
                             " and registered as observer");
        }
    }
    
    private void waitForAllDeliveries() {
        synchronized (this) {
            while (!allDelivered) {
                try {
                    this.wait(10000); 
                    
                    if (!allDelivered) {
                        System.out.println("Customer " + customerId + " timeout - checking file as backup...");
                        checkPackageStatusFromFile();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void checkPackageStatusFromFile() {
    int deliveredCount = 0;
    Lock readLock = MainOffice.getInstance().getTrackingReadLock();
    
    readLock.lock();
    try {
        // Create fresh iterator to get all current packages belonging to this customer
        iterator = new PackageIterator(customerId, MainOffice.getInstance().getPackages());
        
        // For each of MY packages (using iterator)
        while(!iterator.isLastPackage()) {
            Package myPackage = iterator.nextPackage();
            if(myPackage != null) {
                // Check if THIS specific package appears as DELIVERED in the file
                if(isPackageDeliveredInFile(myPackage.getPackageID())) {
                    deliveredCount++;
                }
            }
        }
        
    } catch (Exception e) {
        System.err.println("Customer " + customerId + " error reading file: " + e.getMessage());
    } finally {
        readLock.unlock();
    }
    
    synchronized (this) {
        if (deliveredCount >= myPackages.size() && !allDelivered) {
            allDelivered = true;
            System.out.println("Customer " + customerId + " file check confirmed all packages delivered!");
            this.notifyAll();
        } else {
            System.out.println("Customer " + customerId + " file check: " + deliveredCount + 
                             "/" + myPackages.size() + " packages delivered");
        }
    }
}

// Helper method to check if a specific package ID is delivered in the tracking file
private boolean isPackageDeliveredInFile(int packageId) {
    try (BufferedReader reader = new BufferedReader(
            new FileReader(MainOffice.getInstance().getTrackingFileName()))) {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length >= 5) {
                int filePackageId = Integer.parseInt(parts[0]);
                int fileCustomerId = Integer.parseInt(parts[1]);
                String status = parts[4];
                
                // Check if this line is about our specific package and it's delivered
                if (filePackageId == packageId && 
                    fileCustomerId == this.customerId && 
                    "DELIVERED".equals(status)) {
                    return true;
                }
            }
        }
    } catch (Exception e) {
        System.err.println("Customer " + customerId + " error checking package " + packageId + ": " + e.getMessage());
    }
    return false;
}
    
    public int getCustomerId() {
        return customerId;
    }
    
    public int getDeliveredCount() {
        return deliveredPackagesCount;
    }
    
    public boolean isAllDelivered() {
        return allDelivered;
    }
}