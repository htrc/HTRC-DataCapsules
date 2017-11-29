package edu.indiana.d2i.sloan.bean;

/**
 * Created by ruili on 2/18/17.
 */



public class ResultInfoBean {
    private String vmid, resultId, datafield, createtime, notified, notifiedtime, reviewer, status, comment ;

    public ResultInfoBean(String vmid, String resultId, String datafield,
                          String createtime, String notified, String notifiedtime,
                          String reviewer, String status, String comment)
    {
        this.vmid = vmid;
        this.datafield = datafield;
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

    public String getDatafield() {
        return datafield;
    }

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

}

