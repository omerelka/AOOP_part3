package components;

public class NonStandardPackage extends Package {
	private int width, length, height;	
	
	// Updated constructor with customerId
	public NonStandardPackage(Priority priority, Address senderAddress, Address destinationAddress, int width, int length, int height, int customerId) {
		super(priority, senderAddress, destinationAddress, customerId);
		this.width = width;
		this.length = length;
		this.height = height;	
		System.out.println("Creating " + this);
	}
	
	public int getWidth() {
		return width;
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	public int getLength() {
		return length;
	}
	
	public void setLength(int length) {
		this.length = length;
	}
	
	public int getHeight() {
		return height;
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
	
	@Override
	public String toString() {
		return "NonStandardPackage ["+super.toString() + ", width=" + width + ", length=" + length + ", height=" + height + "]";
	}
}