package edu.indiana.d2i.sloan.bean;

/**
 * Created by ruili on 4/12/17.
 */
public class ReviewInfoBean {
    private String vmid, resultid, username, useremail;
    private String notified;

    public ReviewInfoBean(String vmid, String resultid, String notified, String username,
                          String useremail)
    {
        this.vmid = vmid;
        this.resultid = resultid;
        this.username = username;
        this.notified = notified;
        this.useremail = useremail;
    }

    public String getVmid(){
        return vmid;
    }

    public String getResultid(){
        return resultid;
    }

    public String getUsername(){
        return username;
    }

    public String getNotified(){
        return notified;
    }

    public String getUseremail(){
        return useremail;
    }


}
