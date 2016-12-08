package edu.indiana.d2i.sloan.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liang on 9/21/16.
 */
public class ListVmKeyInfoResponseBean {
    private List<VmKeyInfoBean> vms = new ArrayList<VmKeyInfoBean>();

    public ListVmKeyInfoResponseBean(List<VmKeyInfoBean> vms) {
        this.vms = vms;
    }

    public List<VmKeyInfoBean> getVMsInfo() {
        return vms;
    }

    @Override
    public String toString() {
        return vms.toString();
    }
}
