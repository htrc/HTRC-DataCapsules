package edu.indiana.d2i.sloan.bean;

import java.util.List;

/**
 *
 * List metadata for all entries in result table
 * Create a reviewer dashboard index view
 *
 */

public class ReviewInfoBean {
    private String vmid, resultid, username, useremail;
    private String notified, status, reviewer;
    private String comment;
    private String createtime;
    private List<VmUserRole> roles;

    public ReviewInfoBean(String vmid, String resultid, String notified, String status, String username,
                          String useremail, String reviewer, String comment, String createtime, List<VmUserRole> roles)
    {
        this.vmid = vmid;
        this.resultid = resultid;
        this.username = username;
        this.notified = notified;
        this.useremail = useremail;
        this.reviewer = reviewer;
        this.status = status;
        this.comment = comment;
        this.createtime = createtime;
        this.roles = roles;
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

    public String getCreatetime() { return createtime; }

    public List<VmUserRole> getRoles() {
        return this.roles;
    }
}
