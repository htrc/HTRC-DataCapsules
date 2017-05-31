package edu.indiana.d2i.sloan.bean;

/**
 * Created by ruili on 5/21/17.
 */
public class CommentResponseBean {
    private String res;

    public CommentResponseBean(String res) {
        this.res = res;
    }

    public String getRes() {
        return res;
    }

    @Override
    public String toString() {
        return res.toString();
    }
}
