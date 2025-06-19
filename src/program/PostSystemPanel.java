package program;

import components.*;
import components.Package;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class PostSystemPanel extends JPanel implements ActionListener {
   private static final long serialVersionUID = 1L;
   private Main frame;
   private JPanel p1;
   private JButton[] b_num;
   private String[] names = {"Create system","Start","Stop","Resume","All packages info","Branch info","Clone Branch","Restore","Report"};
   private JScrollPane scrollPane;
   private boolean isTableVisible = false;
   private boolean isTable2Visible = false;
   private int colorInd = 0;
   private boolean started = false;
   private MainOffice game = null;
   private int packagesNumber;
   private int branchesNumber;
   private int trucksNumber;
	
   
   public PostSystemPanel(Main f) {
	    frame = f;
	    isTableVisible = false;
	    setBackground(new Color(255,255,255));
	    p1=new JPanel();
		p1.setLayout(new GridLayout(1,9,0,0)); // Changed from 7 to 9
		p1.setBackground(new Color(0,150,255));
		b_num=new JButton[names.length];
		
		for(int i=0;i<names.length;i++) {
		    b_num[i]=new JButton(names[i]);
		    b_num[i].addActionListener(this);
		    b_num[i].setBackground(Color.lightGray);
		    p1.add(b_num[i]);		
		}

		setLayout(new BorderLayout());
		add("South", p1);
   }	
   
   
   public void createNewPostSystem(int branches, int trucks, int packages) {
	   if (started) return;
	   game = MainOffice.getInstance();
	   game.initialize(branches,trucks,this,packages);
	   packagesNumber = packages;
	   trucksNumber = trucks;
	   branchesNumber = branches;
	   
	   repaint();
   }

   public void paintComponent(Graphics g) {
    super.paintComponent(g);	
       	
    if (game==null) return;
    
    Hub hub = game.getHub();
    ArrayList<Branch> branches = hub.getBranches();
    
    int branchOffset = 403/(branchesNumber-1);
    int y=100;
    int y2=246;
    int offset2 = 140/(branchesNumber-1);
    
    g.setColor(new Color(0,102,0));
    g.fillRect(1120, 216, 40, 200);
    
    for (Branch br: branches) {
        br.paintComponent(g,y,y2);
        y+=branchOffset;
        y2+=offset2;
    }
    
    int packageOffset = Math.max(80, 403/(branchesNumber-1));
    
    int x = 150;
    int offset3 = (1154-300)/(packagesNumber-1);
    
    for (Package p: game.getPackages()) {
        p.paintComponent(g,x,packageOffset);
        x+=offset3;
    }
    
    for (Branch br: branches) {
        for(Truck tr: br.getTrucks()) {
            tr.paintComponent(g);
        }
    }
    
    for(Truck tr: hub.getTrucks()) {
        tr.paintComponent(g);
    }
}

   
   public void setColorIndex(int ind) {
	   this.colorInd = ind;
	   repaint();
   }
   
   
   public void setBackgr(int num) {
	   switch(num) {
	   case 0:
		   setBackground(new Color(255,255,255));
		   break;
	   case 1:
		   setBackground(new Color(0,150,255));
		   break;

	   }
	   repaint();
   }
   
   
   
   public void add(){
	   CreatePostSystemlDialog dial = new CreatePostSystemlDialog(frame,this,"Create post system");
	   dial.setVisible(true);
   }
   
   

   public void start() {
	   if (game==null || started) return;
	   Thread t = new Thread(game);
	   started = true;
	   t.start();
   }
   
	public void resume() {
		if (game == null) return;
		game.setResume();
   }

 	public void stop() {
 		if (game == null) return;
	    game.setSuspend();
 	}

 	
    public void info() {
 	   if (game == null || !started) return;
	   if(isTable2Visible == true) {
		   scrollPane.setVisible(false);
		   isTable2Visible = false;
	   }
 	   if(isTableVisible == false) {
 			 int i=0;
 			 String[] columnNames = {"Package ID", "Sender", "Destination", "Priority", "Staus"};
 			 ArrayList<Package> packages = game.getPackages();
 			 String [][] data = new String[packages.size()][columnNames.length];
 			 for(Package p : packages) {
 		    	  data[i][0] = ""+p.getPackageID();
 		    	  data[i][1] = ""+p.getSenderAddress();
 		    	  data[i][2] = ""+p.getDestinationAddress();
 		    	  data[i][3] = ""+p.getPriority();
 		    	  data[i][4] = ""+p.getStatus();
 		    	  i++;
 			 }
 			 JTable table = new JTable(data, columnNames);
 		     scrollPane = new JScrollPane(table);
 		     scrollPane.setSize(450,table.getRowHeight()*(packages.size())+24);
 		     add( scrollPane, BorderLayout.CENTER );
 		     isTableVisible = true;
 	   }
 	   else
 		   isTableVisible = false;
 	   
 	   scrollPane.setVisible(isTableVisible);
       repaint();
    }
    
   
   public void branchInfo() {
	   if (game == null || !started) return;
   
	   if(scrollPane!=null) scrollPane.setVisible(false);
	   isTableVisible = false;
	   isTable2Visible = false;
	   String[] branchesStrs = new String[game.getHub().getBranches().size()+1];
	   branchesStrs[0] = "Sorting center";
	   for(int i=1; i<branchesStrs.length; i++)
		   branchesStrs[i] = "Branch "+i;
	   JComboBox cb = new JComboBox(branchesStrs);
	   String[] options = { "OK", "Cancel" };
	   String title = "Choose branch";
	   int selection = JOptionPane.showOptionDialog(null, cb, title,
	        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
	        options[0]);

	   if (selection==1) return;
	   //System.out.println(cb.getSelectedIndex());
	   if(isTable2Visible == false) {
			 int i=0;
			 String[] columnNames = {"Package ID", "Sender", "Destination", "Priority", "Staus"};
			 Branch branch;
			 List<Package> packages = null;
			 int size=0;
			 if(cb.getSelectedIndex()==0) {
				 packages = game.getHub().getPackages();
				 size = packages.size();
			 }
			 else {
				 packages = game.getHub().getBranches().get(cb.getSelectedIndex()-1).getPackages();
				 size = packages.size();
				 int diff = 0;
				 for(Package p : packages) {
					 if (p.getStatus()==Status.BRANCH_STORAGE) {
						 diff++;
					 }
				 }
				 size = size - diff/2;
			 }
			 String [][] data = new String[size][columnNames.length];
			 for(Package p : packages) {
				 boolean flag = false;
				 for(int j=0; j<i; j++)
					 if (data[j][0].equals(""+p.getPackageID())) {
						 flag = true;
						 break;
					 }
				 if (flag) continue;
		    	 data[i][0] = ""+p.getPackageID();
		    	 data[i][1] = ""+p.getSenderAddress();
		    	 data[i][2] = ""+p.getDestinationAddress();
		    	 data[i][3] = ""+p.getPriority();
		    	 data[i][4] = ""+p.getStatus();
		    	 i++;
			 }
			 JTable table = new JTable(data, columnNames);
		     scrollPane = new JScrollPane(table);
		     scrollPane.setSize(450,table.getRowHeight()*(size)+24);
		     add( scrollPane, BorderLayout.CENTER );
		     isTable2Visible = true;
	   }
	   else
		   isTable2Visible = false;
	   
	   scrollPane.setVisible(isTable2Visible);
       repaint();
   }

   public void cloneBranch() {
    if (game == null || !started) {
        JOptionPane.showMessageDialog(this, "Please create and start the system first!");
        return;
    }
    
    ArrayList<Branch> branches = game.getHub().getBranches();
    String[] branchOptions = new String[branches.size()];
    
    for(int i = 0; i < branches.size(); i++) {
        branchOptions[i] = "Branch " + i;
    }
    
    JComboBox<String> branchComboBox = new JComboBox<>(branchOptions);
    String[] options = {"OK", "Cancel"};
    String title = "Select Branch to Clone";
    
    int selection = JOptionPane.showOptionDialog(
        this, 
        branchComboBox, 
        title,
        JOptionPane.DEFAULT_OPTION, 
        JOptionPane.PLAIN_MESSAGE, 
        null, 
        options,
        options[0]
    );
    
    if (selection == 1 || selection == JOptionPane.CLOSED_OPTION) {
        return;
    }
    
    try {
        int selectedBranchIndex = branchComboBox.getSelectedIndex();
        Branch originalBranch = branches.get(selectedBranchIndex);
        
        BranchMemento memento = new BranchMemento(branches.size(), originalBranch);
        game.getBranchCaretaker().saveMemento(memento);
        
        Branch clonedBranch = originalBranch.clone();
        
        // Add the cloned branch to the hub
        game.getHub().add_branch(clonedBranch);
        
        Thread branchThread = new Thread(clonedBranch);
        branchThread.start();
        
        for (Truck truck : clonedBranch.getTrucks()) {
            Thread truckThread = new Thread(truck);
            truckThread.start();
        }
        
        branchesNumber = game.getHub().getBranches().size();
        
        JOptionPane.showMessageDialog(
            this, 
            "Branch cloned successfully!\nOriginal: " + originalBranch.getName() + 
            "\nCloned: " + clonedBranch.getName(),
            "Clone Successful",
            JOptionPane.INFORMATION_MESSAGE
        );
        
        repaint();
        
    } catch (CloneNotSupportedException e) {
        JOptionPane.showMessageDialog(
            this, 
            "Error cloning branch: " + e.getMessage(),
            "Clone Error",
            JOptionPane.ERROR_MESSAGE
        );
        e.printStackTrace();
    }
}
   

   // NEW: Restore functionality (placeholder for Memento pattern)
   public void restore() {
	  if (game == null) {
        JOptionPane.showMessageDialog(this, "Please create the system first!");
        return;
    }
    
    if (!game.getBranchCaretaker().hasSavedState()) {
        JOptionPane.showMessageDialog(this, "No clone operation to restore!");
        return;
    }
    
    boolean success = game.restoreLastClone();
    
    if (success) {
        branchesNumber = game.getHub().getBranches().size();
        JOptionPane.showMessageDialog(this, "Clone operation restored successfully!");
        repaint();
    } else {
        JOptionPane.showMessageDialog(this, "Failed to restore clone operation!");
    }
   }

   public void showReport() {
	   if (game == null) {
		   JOptionPane.showMessageDialog(this, "Please create the system first!");
		   return;
	   }
	   
	   try {
		   StringBuilder content = new StringBuilder();
		   content.append("Tracking File Content:\n");
		   content.append("Format: PackageID,CustomerID,Time,Node,Status\n\n");
		   
		   java.io.BufferedReader reader = new java.io.BufferedReader(
			   new java.io.FileReader("tracking.txt")
		   );
		   String line;
		   while ((line = reader.readLine()) != null) {
			   content.append(line).append("\n");
		   }
		   reader.close();
		   
		   JTextArea textArea = new JTextArea(content.toString());
		   textArea.setEditable(false);
		   JScrollPane scrollPane = new JScrollPane(textArea);
		   scrollPane.setPreferredSize(new java.awt.Dimension(500, 400));
		   
		   JOptionPane.showMessageDialog(
			   this, 
			   scrollPane, 
			   "Tracking Report", 
			   JOptionPane.INFORMATION_MESSAGE
		   );
		   
	   } catch (java.io.IOException e) {
		   JOptionPane.showMessageDialog(
			   this, 
			   "Error reading tracking file: " + e.getMessage(),
			   "File Error",
			   JOptionPane.ERROR_MESSAGE
		   );
	   }
   }
   
   public void destroy(){  	        
      System.exit(0);
   }
   
   
   public void actionPerformed(ActionEvent e) {
	if(e.getSource() == b_num[0]) 
		add();
	else if(e.getSource() == b_num[1]) 
		start();
	else if(e.getSource() == b_num[2])  
		stop();
	else if(e.getSource() == b_num[3])  
		resume(); 
	else if(e.getSource() == b_num[4])  
		info();
	else if(e.getSource() == b_num[5])  
		branchInfo();
	else if(e.getSource() == b_num[6])  
		cloneBranch();
	else if(e.getSource() == b_num[7])  
		restore();
	else if(e.getSource() == b_num[8])  
		showReport();
   }

}