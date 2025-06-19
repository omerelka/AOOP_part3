package components;

public class BranchCaretaker {
    private BranchMemento currentMemento;
    
    public void saveMemento(BranchMemento memento) {
        this.currentMemento = memento;
        System.out.println("Memento saved: " + memento);
    }
    
    public BranchMemento getMemento() {
        return currentMemento;
    }
    
    public boolean hasSavedState() {
        return currentMemento != null;
    }
    
    public void clearMemento() {
        if (currentMemento != null) {
            System.out.println("Clearing memento: " + currentMemento);
            currentMemento = null;
        }
    }
}