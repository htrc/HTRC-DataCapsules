package edu.indiana.d2i.sloan.bean;

/**
 *
 * List info for a given resultid
 * Create a snapshot for individual result (per id)
 *
 **/



public class ResultInfoBean {
    private String vmid, resultId, createtime, notified, notifiedtime, reviewer, status, comment;

    public ResultInfoBean(String vmid, String resultId,
                          String createtime, String notified, String notifiedtime,
                          String reviewer, String status, String comment)
    {
        this.vmid = vmid;
        //this.datafield = datafield;
        this.resultId = resultId;
        this.createtime = createtime;
        this.notified = notified;
        this.notifiedtime = notifiedtime;
        this.reviewer = reviewer;
        this.status = status;
        this.comment = comment;
    }

    public String getVmid() {
        return vmid;
    }

    //public String getDatafield() {
    //    return datafield;
    //}

    public String getResultId() {
        return resultId;
    }

    public String getCreatetime(){
        return createtime;
    }

    public String getNotified(){
        return notified;
    }

    public String getNotifiedtime(){
        return notifiedtime;
    }

    public String getReviewer(){return reviewer;}

    public String getStatus(){return status;}

    public String getComment(){return comment;}

    //public String getUseremail() { return useremail; }
}

