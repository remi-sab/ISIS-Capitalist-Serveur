/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.isis.generated;

/**
 *
 * @author remis
 */

import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

@Path("generic")
public class Webservice {
    Services services;
    public Webservice(){
        services =new Services();
    }
    
    @GET
    @Path("world")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})    
    public Response getXml(@Context HttpServletRequest request) throws JAXBException,FileNotFoundException, IOException{
        String username;
        username = request.getHeader("X-user");
        return Response.ok(services.getWorld(username)).build();
    }
    
    @PUT
    @Path("product")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response editProduct(String data) throws JAXBException, FileNotFoundException, IOException{
        ProductType product = new Gson().fromJson(data, ProductType.class);
        return Response.ok(services.updateProduct(data, product)).build();
    }
    
     @PUT
    @Path("manager")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response editManager(String data) throws JAXBException, FileNotFoundException, IOException{
        PallierType manager = new Gson().fromJson(data, PallierType.class);
        return Response.ok(services.updateManager(data, manager)).build();
    }
}
