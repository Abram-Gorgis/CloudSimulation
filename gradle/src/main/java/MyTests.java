import com.typesafe.config.Config;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.List;

import org.cloudbus.cloudsim.Vm;
import org.junit.Test;
import static org.junit.Assert.*;


public class MyTests {


    public void testIfCloudletListEmpty(List<Cloudlet> list)
    {
        assertFalse(list.isEmpty());
    }

    public void testIfConfFileFound(Config defaulConfig)
    {
        assertFalse(defaulConfig.isEmpty());
    }

    public void testIfVmsCreated(List<Vm> list, Config defaultConfig)
    {
        assertFalse(list.isEmpty());
        assertTrue(list.size()==defaultConfig.getInt("Host.quantity")*defaultConfig.getInt("VirtualMachine.quantity"));
    }

    public void testIfAllCloudletsCompleted(List<Cloudlet> list)
    {
        for(Cloudlet x : list)
        {
            assertTrue(x.getStatus()==Cloudlet.SUCCESS);
        }
    }

    public void testHostToVm(Config defaultConfig){

        int Hsize = defaultConfig.getInt("Host.size");
        int Hram = defaultConfig.getInt("Host.ram");
        int Hmips = defaultConfig.getInt("Host.mips");
        int Hbw = defaultConfig.getInt("Host.bw");
        int HpesNumber = defaultConfig.getInt("Host.pesNumber");

        int Vmquantity = defaultConfig.getInt("VirtualMachine.quantity");
        int Vmsize = defaultConfig.getInt("VirtualMachine.size")*Vmquantity;
        int Vmram = defaultConfig.getInt("VirtualMachine.ram")*Vmquantity;
        int Vmmips = defaultConfig.getInt("VirtualMachine.mips")*Vmquantity;
        int Vmbw = defaultConfig.getInt("VirtualMachine.bw")*Vmquantity;
        int VmpesNumber = defaultConfig.getInt("VirtualMachine.pesNumber")*Vmquantity;


        assertTrue(Hsize>=Vmsize);
        assertTrue(Hram>=Vmram);
        assertTrue(Hmips>=Vmmips);
        assertTrue(Hbw>=Vmbw);
        assertTrue(HpesNumber>=VmpesNumber);
    }

}
