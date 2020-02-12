/*Abram Gorgis
cloudlet simulation with map reduce option
 */

import org.cloudbus.cloudsim.*;
import org.slf4j.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import com.typesafe.config.*;
import java.text.DecimalFormat;
import java.util.*;



public class MyCloudSim {
    static MyTests test = new MyTests();

    private static Config defaultConfig = ConfigFactory.parseResources("config.conf");//read from config file
    private static  List<Cloudlet> cloudletList;//stores cloudlet list
    private static List<Cloudlet> MappedList = new ArrayList<Cloudlet>();//stores list if you choose to map and reduce cloudlets
    /** The vmlist. */
    private static List<Vm> vmList;

    private static  DatacenterBroker broker;
    private static boolean mapper = false; //does user want mapper

    public static void main(String[] args) throws Exception {

        Logger log = LoggerFactory.getLogger(MyCloudSim.class);//logger
        mapper = defaultConfig.getBoolean("Simulation.mapper");//checks if user wants to map and reduce or not

        if(defaultConfig.getString("Simulation.simulation").compareTo("0")!=0)//checks if user wants to run from config file or switch to simulation file
        {
            defaultConfig = ConfigFactory.parseResources("simulation"+defaultConfig.getString("Simulation.simulation")+".conf");//switch to simulation file
        }

        test.testIfConfFileFound(defaultConfig);//test if config file was found

        test.testHostToVm(defaultConfig);//checks if proper ratio of physical machine to vm is possible
        Calendar calendar = Calendar.getInstance();

        // Initialize the CloudSim library
        CloudSim.init(defaultConfig.getInt("Users.numberOfUsers"), calendar, defaultConfig.getBoolean("Users.trace_flag"));

        Datacenter datacenter0 = createDatacenter("Datacenter_0");
        broker.submitVmList(createVM());
        if(mapper) {
            createCloudletListMapped(createCloudletList());//create mapped list from original
            test.testIfCloudletListEmpty(MappedList);//check if list was empty
            broker.submitCloudletList(MappedList);
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            printCloudletList(MappedList);
        }
        else//run regular simulation (not mapped)
        {

            broker.submitCloudletList(createCloudletList());
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            printCloudletList(cloudletList);
        }


    log.info("nre");
    }


    private static List<Vm> createVM() {//creates a list of vms
        LinkedList<Vm> list = new LinkedList<Vm>();

        for(int i=0;i<(defaultConfig.getInt("VirtualMachine.quantity")*defaultConfig.getInt("Host.quantity"));i++){//creates quantity vm * quantity host vms

            list.add( new Vm(i, broker.getId(), defaultConfig.getInt("VirtualMachine.mips") , defaultConfig.getInt("VirtualMachine.pesNumber"),
                    defaultConfig.getInt("VirtualMachine.ram"), defaultConfig.getInt("VirtualMachine.bw"),
                    defaultConfig.getInt("VirtualMachine.size"), defaultConfig.getString("VirtualMachine.vmm"), new CloudletSchedulerTimeShared()));
        }

        test.testIfVmsCreated(list,defaultConfig);
        return list;
    }


    /*create the data center with entered name to hold hosts we will create*/
    private static Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<Host>();

        int mips = defaultConfig.getInt("Host.mips");

        //HOST INFO


        //creates j host copys of i physical machine set ups
        for(int j=0;j<defaultConfig.getInt("Host.quantity");j++) {//create this many hosts
            List<Pe> peList = new ArrayList<Pe>();
            for (int i = 0; i < defaultConfig.getInt("Host.pesNumber"); i++) {
                peList.add(new Pe(i, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
            }

            hostList.add(new Host(
                    j,
                    new RamProvisionerSimple(defaultConfig.getInt("Host.ram")),
                    new BwProvisionerSimple(defaultConfig.getInt("Host.bw")),
                    defaultConfig.getInt("Host.size"),
                    peList,
                    new VmSchedulerTimeShared(peList))
            );
        }

        LinkedList<Storage> storageList = new LinkedList<Storage>();

        //set up data center costs
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                defaultConfig.getString("DataCenter.arch"), defaultConfig.getString("DataCenter.os"), defaultConfig.getString("DataCenter.vmm"),
                hostList, defaultConfig.getLong("DataCenter.time_zone"), defaultConfig.getLong("DataCenter.cost"),
                defaultConfig.getLong("DataCenter.costPerMem"),defaultConfig.getLong("DataCenter.costPerStorage"), defaultConfig.getLong("DataCenter.costPerBw"));



        Datacenter datacenter = null;
        datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        broker = createBroker();
        return datacenter;
    }
    private static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    private static List<Cloudlet> createCloudletList()
    {
       cloudletList= new LinkedList<Cloudlet>();
        UtilizationModelStochastic modelStochastic = new UtilizationModelStochastic();//use this model for random cpu usage

        Logger Log = LoggerFactory.getLogger(MyCloudSim.class);
        for(int id=0;id<defaultConfig.getInt("CloudLet.quantity");id++) {
            Cloudlet cloudlet;
            if(defaultConfig.getInt("CloudLet.length")>0) {//if cloudlet length > 0 get length
                cloudlet = new Cloudlet(id, defaultConfig.getInt("CloudLet.length"), defaultConfig.getInt("CloudLet.pes"),
                        defaultConfig.getInt("CloudLet.fileSize"), defaultConfig.getInt("CloudLet.outputSize"),
                        modelStochastic, modelStochastic, modelStochastic);
                cloudlet.setUserId(broker.getId());
            }
            else//if length is 0 or less generate random numbers
            {
                Random rand = new Random();
                cloudlet = new Cloudlet(id,rand.nextInt(60000)+20000, defaultConfig.getInt("CloudLet.pes"),
                        defaultConfig.getInt("CloudLet.fileSize"), defaultConfig.getInt("CloudLet.outputSize"),
                        modelStochastic, modelStochastic, modelStochastic);
                cloudlet.setUserId(broker.getId());
            }

            // add the cloudlet to the list
            cloudletList.add(cloudlet);
        }
        test.testIfCloudletListEmpty(cloudletList);
        return cloudletList;
    }

    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;
        Logger Log = LoggerFactory.getLogger(MyCloudSim.class);
        String indent = "    ";
        Log.info("\n");
        Log.info("========== OUTPUT ==========");
        Log.info("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent +"Vim ID"+indent+ "Cost"+indent+indent+ "Cpu");

        test.testIfAllCloudletsCompleted(list);//makes sure all cloudlets have status success

        double totalCost=0;//used to save total cost of running all cloudlets

        DecimalFormat dft = new DecimalFormat("###.##");
        DecimalFormat dft2 = new DecimalFormat(".##");

        //set up start and end time so we can see how long the cloudlets took from first to last
        double start=list.get(0).getExecStartTime(),finish=list.get(0).getFinishTime();

        //for every cloudlet print information
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
           if(start>cloudlet.getExecStartTime())
               start = cloudlet.getExecStartTime();
           else if(finish<cloudlet.getFinishTime())
               finish = cloudlet.getFinishTime();

            double cpuUsed=0;
            //for this cloudlet get total cpu utilization will multiply by 100 later to make a percent
            for(double j = cloudlet.getExecStartTime();j<cloudlet.getFinishTime();j++)
            {
                cpuUsed+=cloudlet.getUtilizationOfCpu(j);
            }
            //makes cpu used a ratio
            cpuUsed=cpuUsed/(cloudlet.getFinishTime()-cloudlet.getExecStartTime());


            //print cloudlet info if success
            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
            Log.info(indent + cloudlet.getCloudletId() + indent + indent + "SUCCESS" + indent + indent + cloudlet.getResourceId()
                        + indent + indent + indent + indent+ cloudlet.getVmId()
                        + indent + indent
                        + dft.format(cloudlet.getActualCPUTime()*cloudlet.getCostPerSec())+"$"+indent+indent+dft2.format(cpuUsed*100)+"%");
            totalCost+=cloudlet.getActualCPUTime()*cloudlet.getCostPerSec();
            }
        }
        Log.info(indent+"###Total Cost: "+dft2.format(totalCost)+"$"+indent+"Total time:"+dft2.format((finish-start)));
    }

    /*simple mapper that splits cloudlet into 2 and assigns random id from 0-(number of original cloudlets)*2
    * if the new split cloudlets get mapped to the same place they are reduced to one task we then iterate
    * trough the map and then run that list of cloudlets on your vms and it will be more efficient*/
    private static void createCloudletListMapped(List<Cloudlet> preMapped)
    {
        Random rand = new Random();
        Map<Integer,Cloudlet> hm = new HashMap<Integer, Cloudlet>();//used to store cloudlet "shards"
        UtilizationModel utilizationModel = new UtilizationModelFull();

        //for each cloudlet in premapped we will split the length in 2 keep all other info the same and
        //change the id to be a random number from 0-#ofCloudlets*2 so that when reduced it simulates random shards
        for(Cloudlet x : preMapped)
        {
            Cloudlet cloudlet1 = new Cloudlet(rand.nextInt(preMapped.size()*2), x.getCloudletTotalLength()/2, x.getNumberOfPes(),
                x.getCloudletFileSize()/2, x.getCloudletOutputSize()/2,
                utilizationModel, utilizationModel, utilizationModel);
            cloudlet1.setUserId(broker.getId());
            hm.put(cloudlet1.getCloudletId(),cloudlet1);

            Cloudlet cloudlet2 = new Cloudlet(rand.nextInt(preMapped.size()*2), x.getCloudletTotalLength()/2, x.getNumberOfPes(),
                    x.getCloudletFileSize()/2, x.getCloudletOutputSize()/2,
                    utilizationModel, utilizationModel, utilizationModel);
            cloudlet2.setUserId(broker.getId());
            hm.put(cloudlet2.getCloudletId(),cloudlet2);

        }
        //takes the set of the map entry so if two cloudlets get mapped to the same id the get reduced
        Set< Map.Entry< Integer,Cloudlet> > st = hm.entrySet();
        for (Map.Entry< Integer,Cloudlet> me:st)
        {
            MappedList.add(me.getValue());
        }

    }


}