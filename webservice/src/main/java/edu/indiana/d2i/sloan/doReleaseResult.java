/*******************************************************************************
 * Copyright 2014 The Trustees of Indiana University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package edu.indiana.d2i.sloan;

/**
 * Created by ruili on 2/20/17.
 */


import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.internet.MimeMessage.RecipientType;

//import com.sun.xml.internal.ws.server.sei.EndpointResponseMessageBuilder;
import edu.indiana.d2i.sloan.bean.*;
import edu.indiana.d2i.sloan.db.DBConnections;
import edu.indiana.d2i.sloan.db.DBSchema;
import edu.indiana.d2i.sloan.utils.EmailUtil;
import edu.indiana.d2i.sloan.vm.VMMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.hyper.DeleteVMCommand;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;
import sun.security.util.Password;


import java.sql.*;
import java.sql.Date;
import java.text.*;
import java.text.ParseException;
import java.util.*;
import java.util.Date.*;

import java.io.*;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Deprecated
 *
 * param:
 *   resultid: identified of record in result table
 *
 *   process release jobs including:
 *     create download link
 *     send emails
 */

@Deprecated
@Path("/ReleaseResult")
public class doReleaseResult {

    private static Logger logger = LoggerFactory.getLogger(CreateVM.class);
    private DBConnections DBConnections;


    @POST
    //@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    //@Produces(MediaType.APPLICATION_JSON)

public Response releaseResult(@FormParam("resultId") String resultid,
                                @Context HttpHeaders httpHeaders,
                                @Context HttpServletRequest httpServletRequest) {
        String userName = httpServletRequest.getHeader(Constants.USER_NAME);
        String userEmail = httpServletRequest.getHeader(Constants.USER_EMAIL);


        /*1. check if the result has been released */
        try {
            ResultInfoBean result = DBOperations.getInstance().getResultInfo(resultid);
            String notified = result.getNotified();
            if (notified.equals("0")) {
                logger.info("The result with id " + resultid + " is still available\n");
                //can continue
            } else {
                logger.info("The result with id " + resultid + " has been released \n");
                return Response.status(406)
                        .entity(new ErrorBean(406, "Result with id" + resultid+"has been released before")).build();
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            return Response
                    .status(400)
                    .entity(new ErrorBean(400, "SQL Syntax Error")).build();
            //e.printStackTrace();
        } catch (NoItemIsFoundInDBException e) {
            logger.error(e.getMessage(), e);
            return Response
                    .status(400)
                    .entity(new ErrorBean(400, "No Item found for this result id")).build();
        } catch (ParseException e) {
            logger.error(e.getMessage(),e);
            return Response
                    .status(400)
                    .entity(new ErrorBean(400, "Parse Error")).build();
        }

        /*2. update starttime in database if result to be released*/
        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        java.util.Date date = new java.util.Date();
        java.sql.Timestamp timestamp = new java.sql.Timestamp(date.getTime());
        try {

            DBOperations.getInstance().updateResultTimeStamp(resultid, timestamp);

        } catch (SQLException e) {
            //e.printStackTrace();
            logger.error(e.getMessage(),e);
            return Response
                    .status(400)
                    .entity(new ErrorBean(400, "SQL Syntax Error")).build();
        }
        //TODO: add catch update fail, do db.rollback


        /*3. send out result datafield as content in email*/
        EmailUtil send_email = new EmailUtil();
        //constructor fetch properites automatically
        String download_addr = String.format(Configuration.PropertyName.RESULT_DOWNLOAD_URL_PREFIX, resultid);

        //construct email content
        String content = String.format("Please download result from the following URL: \n", download_addr);
        send_email.sendEMail(null, userEmail, "HTRC Data Capsule Result Download URL", content);


        /*4. mark this result as released (change notified to 1)*/

        try {
            DBOperations.getInstance().updateResultAsNotified(resultid);
        } catch (SQLException e) {
            //e.printStackTrace();
            logger.error(e.getMessage(),e);
            return Response
                .status(400)
                .entity(new ErrorBean(400, "SQL Syntax Error")).build();
        }

        return Response.status(200).build();
    }





    /*Already in util.EamilUtil*/
    /**
    public void send_email(StringBuilder content, String subject, String recipient) throws AddressException,
            MessagingException, IOException
    {
        List<String> lines = Files.readAllLines(Paths.get(content),Charset.forName("UTF-8"));

        String sender = "htrccapsule@gmail.com";

        StringBuilder builder = new StringBuilder();
        builder = content;


        Properties props = new Properties();


        Session mailSession = Session.getDefaultInstance(props);


        try {



            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(sender));
            message.addRecipients(RecipientType.TO, String.valueOf(new InternetAddress(recipient)));
            message.setSubject(subject);
            message.setText(builder.toString());


            Transport.send(message);

            System.out.println("message sent successfully");

        }finally {

        }

    }
    **/
    //check if it has been released or not

    /*fetch datafield based on resultid*/
    /*return as StringBuilder in case of fetching large content*/


}
