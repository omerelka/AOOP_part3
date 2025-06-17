package components;

import java.util.List;

public class PackageIterator implements Iterator{
    private int customID;
    private List<Package> packageList;
    private int position;

    public PackageIterator(int customid, List<Package> packages){
        this.customID = customid;
        this.packageList = packages;
        this.position = 0;
    }

    @Override
    public Package nextPackage() {
        Package pack = null;
        for(; position < packageList.size(); position++) {
            if(customID == packageList.get(position).getCustomerId()) {
                pack = packageList.get(position);
                position++;
                break;
            }
        }
        return pack;
    }

    @Override
    public boolean isLastPackage() {
        for(int i = position; i < packageList.size(); i++) {
            if(customID == packageList.get(i).getCustomerId()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Package currentPackage() {
        if(position < packageList.size()) {
            return packageList.get(position);
        }
        return null;
    }
}