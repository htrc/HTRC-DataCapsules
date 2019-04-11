package edu.indiana.d2i.sloan.db;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestDBMigration {

    @Test
    public void testDbMigrationUnToGuid() throws IOException{
        /*
        This test is written to verify the username -> guid migration in htrcvirtdb
        Run following commands in the host of the DB to generate required files

        Before migration:
        mysql -u <username> -p<pw> -h <host> htrcvirtdb -e "select  username, useremail from users" |  tr '\t' ',' > un_users.csv
        mysql -u <username> -p<pw> -h <host> htrcvirtdb -e "select  username, GROUP_CONCAT(vmid order by vmid SEPARATOR '|') as vmids from vms group by username" | tr '\t' ',' > un_vms.csv
        mysql -u <username> -p<pw> -h <host> htrcvirtdb -e "select  username, GROUP_CONCAT(id order by id SEPARATOR '|') as ids from vmactivity group by username" | tr '\t' ',' > un_vmactivity.csv
        mysql -u <username> -p<pw> -h <host> htrcvirtdb -e "select  username, GROUP_CONCAT(vmid order by vmid SEPARATOR '|') as vmids, GROUP_CONCAT(role order by vmid SEPARATOR '|') as roles from uservmmap group by username" | tr '\t' ',' > un_uservmmap.csv

        After migration:
        mysql -u <username> -p<pw> -h <host> htrcvirtdb -e "select  guid, useremail from users" |  tr '\t' ',' > guid_users.csv
        mysql -u <username> -p<pw> -h <host> htrcvirtdb -e "select  guid, GROUP_CONCAT(vmid order by vmid SEPARATOR '|') as vmids from vms group by guid" | tr '\t' ',' > guid_vms.csv
        mysql -u <username> -p<pw> -h <host> htrcvirtdb -e "select  guid, GROUP_CONCAT(id order by id SEPARATOR '|') as ids from vmactivity group by guid" | tr '\t' ',' > guid_vmactivity.csv
        mysql -u <username> -p<pw> -h <host> htrcvirtdb -e "select  guid, GROUP_CONCAT(vmid order by vmid SEPARATOR '|') as vmids, GROUP_CONCAT(role order by vmid SEPARATOR '|') as roles from uservmmap group by guid" | tr '\t' ',' > guid_uservmmap.csv

        Copy the generated files to base_dir
         */

        Map<String, String> username_guid_map = new HashMap<>();
        String base_dir = "/Users/charmadu/repo/git/git2/HTRC-DataCapsules/webservice/src/test/java/edu/indiana/d2i/sloan/db_verify/prod/";

        Map<String, String> un_users_map = null;
        Map<String, String> un_uservmmap_map = null;
        Map<String, String> un_vmactivity_map = null;
        Map<String, String> un_vms_map = null;

        Map<String, String> guid_users_map = null;
        Map<String, String> guid_uservmmap_map = null;
        Map<String, String> guid_vmactivity_map = null;
        Map<String, String> guid_vms_map = null;

        //This is the file containing the username->guid map
        // Input CSV File format
        //USERNAME,GUID
        //username-0,000
        //username-1,001
        //username-2,002
        //username-3,003
        String username_guid_csv = base_dir + "username_guid_map_prod.csv";

        String un_users = base_dir + "un_users.csv";
        String un_uservmmap = base_dir + "un_uservmmap.csv";
        String un_vmactivity = base_dir + "un_vmactivity.csv";
        String un_vms = base_dir + "un_vms.csv";

        String guid_users = base_dir + "guid_users.csv";
        String guid_uservmmap = base_dir + "guid_uservmmap.csv";
        String guid_vmactivity = base_dir + "guid_vmactivity.csv";
        String guid_vms = base_dir + "guid_vms.csv";

        username_guid_map = TestDBMigration.load_data(username_guid_csv);

        un_users_map = TestDBMigration.load_data(un_users);
        un_uservmmap_map = TestDBMigration.load_data(un_uservmmap);
        un_vmactivity_map = TestDBMigration.load_data(un_vmactivity);
        un_vms_map = TestDBMigration.load_data(un_vms);

        guid_users_map = TestDBMigration.load_data(guid_users);
        guid_uservmmap_map = TestDBMigration.load_data(guid_uservmmap);
        guid_vmactivity_map = TestDBMigration.load_data(guid_vmactivity);
        guid_vms_map = TestDBMigration.load_data(guid_vms);

        for(String username : username_guid_map.keySet()) {
            String guid = username_guid_map.get(username);
//            //System.out.println("Username : " + username);
//            //System.out.println("\t users : " + un_users_map.get(username));
//            //System.out.println("\t vms : " + un_vms_map.get(username));
//            //System.out.println("\t vmactivity : " + un_vmactivity_map.get(username));
//            //System.out.println("\t uservms : " + un_uservmmap_map.get(username));

//            //System.out.println("GUID : " + guid);
//            //System.out.println("\t users : " + guid_users_map.get(guid));
//            //System.out.println("\t vms : " + guid_vms_map.get(guid));
//            //System.out.println("\t vmactivity : " + guid_vmactivity_map.get(guid));
//            //System.out.println("\t uservms : " + guid_uservmmap_map.get(guid));

            //System.out.println(username + " -> " + guid);
            try {
                if((un_users_map.get(username) == null && guid_users_map.get(guid) == null) || un_users_map.get(username).equals(guid_users_map.get(guid))) {
                    //System.out.println("\t Success : users table");
                } else {
                    //System.out.println("\t Error : users table");
                    Assert.fail("Error : users table not matched for '" + username + "' -> '" + guid + "' map");
                }

                if((un_uservmmap_map.get(username) == null && guid_uservmmap_map.get(guid) == null) || un_uservmmap_map.get(username).equals(guid_uservmmap_map.get(guid))) {
                    //System.out.println("\t Success : uservmmap table");
                } else {
                    //System.out.println("\t Error : uservmmap table");
                    Assert.fail("Error : uservmmap table not matched for '" + username + "' -> '" + guid + "' map");
                }

                if((un_vmactivity_map.get(username) == null && guid_vmactivity_map.get(guid) == null) || un_vmactivity_map.get(username).equals(guid_vmactivity_map.get(guid))) {
                    //System.out.println("\t Success : vmactivity table");
                } else {
                    //System.out.println("\t Error : vmactivity table");
                    Assert.fail("Error : vmactivity table not matched for '" + username + "' -> '" + guid + "' map");
                }

                if((un_vms_map.get(username) == null && guid_vms_map.get(guid) == null) || un_vms_map.get(username).equals(guid_vms_map.get(guid))) {
                    //System.out.println("\t Success : vms table");
                } else {
                    //System.out.println("\t Error : vms table");
                    Assert.fail("Error : vms table not matched for '" + username + "' -> '" + guid + "' map");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //System.out.println();
        }

    }

    private static Map<String, String> load_data(String filename) throws IOException{
        Map<String, String> map = new HashMap<>();
        String line = "";
        String cvsSplitBy = ",";

        BufferedReader br = new BufferedReader(new FileReader(filename));
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split(cvsSplitBy);
            String token_1 = tokens.length > 1 ? tokens[1] : null;
            map.put(tokens[0], token_1);
        }
        return map;
    }

}