package components;

public class SmallPackage extends Package {
	private boolean acknowledge;
	
	// Updated constructor with customerId
	public SmallPackage(Priority priority, Address senderAddress, Address destinationAddress, boolean acknowledge, int customerId){
		super(priority, senderAddress, destinationAddress, customerId);
		this.acknowledge = acknowledge;
		System.out.println("Creating " + this);
	}
	
	public boolean isAcknowledge() {
		return acknowledge;
	}
	
	public void setAcknowledge(boolean acknowledge) {
		this.acknowledge = acknowledge;
	}
	
	@Override
	public String toString() {
		return "SmallPackage ["+ super.toString() +", acknowledge=" + acknowledge + "]";
	}
}