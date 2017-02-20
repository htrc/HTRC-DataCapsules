package edu.indiana.d2i.sloan.bean;

/**
 * Created by ruili on 2/18/17.
 */



public class ResultInfoBean {
    private String vmid, resultId, datafield, createtime, notified, notifiedtime ;

    public ResultInfoBean(String vmid, String resultId, String datafield,
                          String createtime, String notified, String notifiedtime)
    {
        this.vmid = vmid;
        this.datafield = datafield;
        this.resultId = resultId;
        this.createtime = createtime;
        this.notified = notified;
        this.notifiedtime = notifiedtime;
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
}

