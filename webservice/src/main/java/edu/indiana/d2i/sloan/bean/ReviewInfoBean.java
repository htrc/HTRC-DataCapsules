package edu.indiana.d2i.sloan.bean;

/**
 * Created by ruili on 4/12/17.
 */
public class ReviewInfoBean {
    private String vmid, resultid, username, useremail;
    private String notified, status, reviewer;
    private String comment;

    public ReviewInfoBean(String vmid, String resultid, String notified, String status, String username,
                          String useremail, String reviewer, String comment)
    {
        this.vmid = vmid;
        this.resultid = resultid;
        this.username = username;
        this.notified = notified;
        this.useremail = useremail;
        this.reviewer = reviewer;
        this.status = status;
        this.comment = comment;
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

    public String getReviewer(){return reviewer;}

    public String getStatus(){return status;};

    public  String getComment(){return comment;}
}
