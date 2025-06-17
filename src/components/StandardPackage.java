package components;

public class StandardPackage extends Package {
	private double weight;
	
	// Updated constructor with customerId
	public StandardPackage(Priority priority, Address senderAddress, Address destinationAddress, double weight, int customerId) {
		super(priority, senderAddress, destinationAddress, customerId);
		this.weight = weight;
		System.out.println("Creating " + this);
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		return "StandardPackage ["+ super.toString()+", weight=" + weight + "]";
	}
}