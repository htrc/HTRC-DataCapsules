package edu.indiana.d2i.sloan;


import edu.indiana.d2i.sloan.bean.ErrorBean;
import edu.indiana.d2i.sloan.db.DBOperations;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Created by liang on 8/23/16.
 */

@Path("/group_operation")
public class GroupOperation {
    private static Logger logger = Logger.getLogger(GroupOperation.class);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response groupOperation(@FormParam("vmids") String vmids,
                                    @FormParam("usernames") String usernames,
                                    @FormParam("operation") String operation,
                                    @Context HttpHeaders httpHeaders,
                                    @Context HttpServletRequest httpServletRequest) {
        // get operator information
        String operator = httpServletRequest.getHeader(Constants.OPERATOR);
        String operatorEmail = httpServletRequest.getHeader(Constants.OPERATOR_EMAIL);
        if (operatorEmail == null) operatorEmail = "";

        // check operator information
        if (operator == null) {
            logger.error("Operator is not present in http header.");
            return Response
                    .status(500)
                    .entity(new ErrorBean(500,
                            "Operator is not present in http header.")).build();
        }

        // insert operator if it's new
        /*try {
            DBOperations.getInstance().insertUserIfNotExists(operator, operatorEmail);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Response.status(500)
                    .entity(new ErrorBean(500, e.getMessage())).build();
        }*/

        //check operation
        Map<String, String> operationURL = new HashMap<String, String>();
        operationURL.put("launch", "http://localhost:8080:/launchvm");
        operationURL.put("stop", "http://localhost:8080:/stopvm");
        operationURL.put("delete", "http://localhost:8080:/deletevm");

        if (operation == null || operation.length() == 0 || ! operationURL.containsKey(operation)) {
            return Response.status(400)
                    .entity(new ErrorBean(400, "operation is empty or not valid"))
                    .build();
        }

        //check usernames
        if (usernames == null || usernames.length() == 0) {
            return Response.status(400)
                    .entity(new ErrorBean(400, "usernames cannot be empty!"))
                    .build();
        }


        //check vmids
        if (vmids == null || vmids.length() == 0) {
            return Response.status(400)
                    .entity(new ErrorBean(400, "VM IDs cannot be empty!"))
                    .build();
        }

        String[] userNames = usernames.split(",");
        String[] vmIDs = vmids.split(",");
        String URL = operationURL.get(operation);

        List<String> failedVMIDs = new ArrayList<String>();
        for (int i = 0; i < vmIDs.length; i++) {
            String vmid = vmIDs[i];
            String username = userNames[i];

            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(URL);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("vmid", vmid));
            CloseableHttpResponse response = null;
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(nvps));
                httpPost.setHeader(Constants.OPERATOR, operator);
                httpPost.setHeader(Constants.USER_NAME, username);
                response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() != 200) {
                    logger.debug(response.getStatusLine());
                    failedVMIDs.add(vmid);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                try {
                    response.close();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        return failedVMIDs.size() == 0 ? Response.status(200).build() :
                Response.status(400).entity(
                        new ErrorBean(400, StringUtils.join(failedVMIDs, ",")))
                        .build();
    }
}
