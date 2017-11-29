package edu.indiana.d2i.sloan.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruili on 4/12/17.
 */
public class ReviewInfoResponseBean {

    private List<ReviewInfoBean> res = new ArrayList<ReviewInfoBean>();


    public ReviewInfoResponseBean(List<ReviewInfoBean> res) {
        this.res = res;
    }

    public List<ReviewInfoBean> getReviewInfo() {
            return res;
        }

    @Override
    public String toString() {
            return res.toString();
        }


}
