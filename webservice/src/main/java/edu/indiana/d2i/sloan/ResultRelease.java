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

import edu.indiana.d2i.sloan.bean.;
import edu.indiana.d2i.sloan.db.DBConnections;
import edu.indiana.d2i.sloan.db.DBSchema;
import edu.indiana.d2i.sloan.vm.VMMode;
import org.apache.log4j.Logger;

import edu.indiana.d2i.sloan.db.DBOperations;
import edu.indiana.d2i.sloan.exception.NoItemIsFoundInDBException;
import edu.indiana.d2i.sloan.hyper.DeleteVMCommand;
import edu.indiana.d2i.sloan.hyper.HypervisorProxy;
import edu.indiana.d2i.sloan.vm.VMState;
import edu.indiana.d2i.sloan.vm.VMStateManager;
import sun.security.util.Password;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;



@Path("/ResultRelease")
public class ResultRelease {

    private static Logger logger = Logger.getLogger(CreateVM.class);
    private DBConnections DBConnections;

    /**
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
**/
public void Response getResourcePost(@Context HttpHeaders httpHeaders,
                                @Context HttpServletRequest httpServletRequest)
    {
        String userName = httpServletRequest.getHeader(Constants.USER_NAME);
        String userEmail = httpServletRequest.getHeader(Constants.USER_EMAIL);



    }
    //_init_(self)


    public ResultRelease()
    {


    }


    public void db_ConnectionInit()
    {
        cmd

    }

    public void querySQL(String sql)
    {

    }

    public void snedEmail(String content, String subject, String destination){}

    public void wirteZipFile(String filename, String data){}





    public void do_show_release()
    {
        String sql = "select * from results where notified=1";


    }
}
