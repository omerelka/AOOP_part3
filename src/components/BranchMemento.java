package components;

public class BranchMemento {
    private final int originalBranchCount;
    private final Branch originalBranch;
    
    public BranchMemento(int originalBranchCount, Branch originalBranch) {
        this.originalBranchCount = originalBranchCount;
        this.originalBranch = originalBranch;
    }
    
    public int getOriginalBranchCount() {
        return originalBranchCount;
    }
    
    public Branch getOriginalBranch() {
        return originalBranch;
    }
    
    @Override
    public String toString() {
        return "BranchMemento{originalBranchCount=" + originalBranchCount + 
               ", originalBranch=" + (originalBranch != null ? originalBranch.getName() : "null") + "}";
    }
}