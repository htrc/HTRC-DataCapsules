package edu.indiana.d2i.sloan.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruili on 2/20/17.
 */
public class ResultInfoResponseBean {

    private ResultInfoBean result;
    private List<VmUserRole> roles;

    public ResultInfoResponseBean(ResultInfoBean resultInfoBean, List<VmUserRole> roles) {
        this.result = resultInfoBean;
        this.roles = roles;
    }

   /* public ResultInfoBean getResult() {
        return result;
    }*/


    public List<VmUserRole> getRoles() {
        return this.roles;
    }

    public String getResultId() {
        return this.result.getResultId();
    }

    public String getCreatetime(){
        return this.result.getCreatetime();
    }

    public String getNotified(){
        return this.result.getNotified();
    }

    public String getNotifiedtime(){
        return this.result.getNotifiedtime();
    }

    public String getReviewer(){return this.result.getReviewer();}

    public String getStatus(){return this.result.getStatus();}

    public String getComment(){return this.result.getComment();}

    public Boolean isExpired(){return this.result.isExpired();}

    public String getVmid() {
        return this.result.getVmid();
    }

    public String getState() {
        return this.result.getState().name();
    }

    @Override
    public String toString() {
        return result.toString();
    }


}
